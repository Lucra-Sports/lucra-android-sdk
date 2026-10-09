package com.lucrasports.sdk.app.headless_api

import android.content.Context
import com.lucrasports.sdk.core.LucraClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Registers the handshake auth token provider, and produces the tokens it returns.
 *
 * Shared by the Handshake Auth screen and the app-start registration in
 * [HandshakeAuthApiHandler], so a flow entered organically exercises exactly the provider
 * the screen configures.
 *
 * [register] IS the entire client-side integration. A real integrator's lambda body would
 * call their own backend with their own session; here it either mints a token on device
 * (see [HandshakeTokenMinter] for why that is a harness shortcut and not a pattern) or
 * fetches one from the handshake POC's partner backend, which gates its token endpoint on
 * its own session exactly as a real partner would.
 */
internal object HandshakeProviderFactory {

    /** Failures the partner-backend path can produce, named so the log says which leg failed. */
    class PartnerBackendException(message: String) : IOException(message)

    /**
     * Stores the provider on the client. Nothing signs in here — registering and signing
     * in are separate calls, and this one is safe to repeat.
     *
     * Nothing screen-scoped may be captured by the lambda: the SDK holds it for as long as
     * the provider stays registered, so progress goes to [HandshakeLog] instead.
     */
    fun register(context: Context) {
        val store = HandshakeConfigStore(context)

        LucraClient().registerHandshakeAuthTokenProvider(
            bypassTosAgreement = store.load().bypassTosAgreement,
        ) {
            // Loaded per call, never captured: see HandshakeConfig.
            token(store.load())
        }
        store.isProviderRegistered = true
    }

    fun clear(context: Context) {
        LucraClient().registerHandshakeAuthTokenProvider(null)
        HandshakeConfigStore(context).isProviderRegistered = false
    }

    suspend fun token(config: HandshakeConfig): String {
        HandshakeLog.append(
            "token provider called — user=${config.partnerUser} inject=${config.inject.slug}"
        )

        // `slow` still returns inside the SDK's 5s provider deadline, so it demonstrates a
        // sluggish partner rather than a failure; `timeout` outlasts it so the
        // ProviderTimedOut fallback can actually be watched. Both live here rather than in
        // the minter because they break the provider CALL, not the token.
        when (config.inject) {
            Injection.SLOW -> delay(3_000)
            Injection.TIMEOUT -> delay(6_000)
            else -> Unit
        }

        return if (config.mintsLocallyNow) {
            mintOnDevice(config)
        } else {
            fetchFromPartnerBackend(config)
        }
    }

    private fun mintOnDevice(config: HandshakeConfig): String {
        val phone = config.phoneNumber
        if (phone.isNotEmpty() && !isE164(phone) && config.inject != Injection.BAD_PHONE) {
            HandshakeLog.append("⚠︎ phone_number $phone is not E.164 — expect the exchange to reject it")
        }

        val token = HandshakeTokenMinter.mint(config.toMinterInput())
        HandshakeLastToken.record(token, HandshakeLastToken.Source.LOCAL)
        HandshakeLog.append("token minted locally (${token.take(18)}…) — see Last token")

        return token
    }

    /**
     * Two legs, mirroring a real integration: log in to the partner's own backend, then
     * ask it — with that session — for a Lucra handshake token. No signing key is ever
     * involved on the device.
     */
    private suspend fun fetchFromPartnerBackend(
        config: HandshakeConfig,
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("username", config.partnerUser)
            if (config.isCustom) {
                put("id", config.effectiveCustomSub)
                put("phone", config.customPhone)
                if (config.customEmail.isNotEmpty()) put("email", config.customEmail)
            }
        }

        val cookie = login(config.partnerBaseURL, body)
        val token = requestToken(config, cookie)
        HandshakeLastToken.record(token, HandshakeLastToken.Source.PARTNER)
        HandshakeLog.append("partner token received (${token.take(18)}…) — see Last token")

        token
    }

    private fun login(baseUrl: String, body: JSONObject): String {
        val connection = openConnection("$baseUrl/login").apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        return connection.use {
            it.outputStream.write(body.toString().toByteArray(Charsets.UTF_8))
            if (it.responseCode != HttpURLConnection.HTTP_OK) {
                throw PartnerBackendException(
                    "Partner backend login failed (HTTP ${it.responseCode}). Is it running, " +
                            "and reachable at this URL from the device?"
                )
            }
            // Only the name=value pair travels back; the attributes are the server's.
            it.getHeaderField("Set-Cookie")?.substringBefore(';')
                ?: throw PartnerBackendException("Partner backend login returned no session cookie.")
        }
    }

    private fun requestToken(config: HandshakeConfig, cookie: String): String {
        val url = "${config.partnerBaseURL}/api/lucra/token" +
                "?client=${config.tenant}&inject=${config.inject.slug}"
        val connection = openConnection(url).apply {
            requestMethod = "GET"
            setRequestProperty("Cookie", cookie)
        }

        return connection.use {
            if (it.responseCode != HttpURLConnection.HTTP_OK) {
                throw PartnerBackendException(
                    "Partner backend token endpoint failed (HTTP ${it.responseCode})."
                )
            }
            val payload = it.inputStream.bufferedReader().use { reader -> reader.readText() }
            JSONObject(payload).optString("token").ifEmpty {
                throw PartnerBackendException("Partner backend returned no token.")
            }
        }
    }

    /**
     * The base URL is free-typed in the harness, so a malformed one has to read as a
     * configuration problem rather than an opaque provider failure.
     */
    private fun openConnection(url: String): HttpURLConnection {
        val parsed = try {
            URL(url)
        } catch (e: Exception) {
            throw PartnerBackendException("Partner backend URL is not a valid URL: $url")
        }

        return (parsed.openConnection() as HttpURLConnection).apply {
            // Comfortably inside the SDK's 5s provider deadline, so a dead backend
            // surfaces as ProviderFailed rather than racing ProviderTimedOut.
            connectTimeout = 3_000
            readTimeout = 3_000
        }
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }
}
