package com.lucrasports.sdk.app.headless_api

import android.util.Base64
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Mints partner handshake tokens ON DEVICE so handshake auth can be exercised without
 * running a partner backend. TEST HARNESS ONLY.
 *
 * This is deliberately NOT how an integration works. In production the signing key never
 * leaves the partner's server and the app receives an already-signed token from the
 * partner's own authenticated API — which is what the provider lambda exists for. A
 * shared secret sitting on a device can mint a token for ANY phone number, so treat the
 * key used here as a disposable dev1 credential and nothing more.
 *
 * The Lucra side verifies with:
 *
 *     jwtVerify(token, key, { algorithms: ['HS256'], maxTokenAge: '60s', clockTolerance: 30 })
 *
 * so only the signature and `iat` freshness gate the exchange. The payload is exactly:
 *
 *     { sub, phone_number, email (optional), iat, jti }
 *
 * `phone_number` is what the post-login chain resolves the Lucra user by, and `jti` is
 * the handle for replay protection. Nothing else is sent — no `iss`, `aud`, or `exp` —
 * because an unexpected claim can only ever reject a token that would otherwise pass.
 */
internal object HandshakeTokenMinter {

    /**
     * How the typed secret becomes HMAC key bytes.
     *
     * The POC's Node side did `new TextEncoder().encode(secret)` on a hex STRING, so the
     * key was the UTF-8 bytes of the hex text rather than the 32 bytes that text spells.
     * Which one the real backend loads is not visible from its verify call, so this is a
     * picker instead of a guess — picking wrong surfaces only as an opaque signature
     * rejection.
     */
    enum class KeyEncoding(val label: String) {
        UTF8("UTF-8 text"),
        HEX("hex → bytes"),
        BASE64("base64 → bytes");

        fun keyBytes(secret: String): ByteArray {
            // Pasted keys routinely carry a trailing newline, and under UTF8 that
            // silently changes every byte of the MAC.
            val trimmed = secret.trim()
            require(trimmed.isNotEmpty()) {
                "No signing secret set — configure the shared key first."
            }

            return when (this) {
                UTF8 -> trimmed.toByteArray(Charsets.UTF_8)
                HEX -> hexDecoded(trimmed)
                    ?: throw IllegalArgumentException("Signing secret is not valid hex.")

                BASE64 -> try {
                    Base64.decode(trimmed, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Signing secret is not valid base64.")
                }
            }
        }

        private fun hexDecoded(value: String): ByteArray? {
            if (value.length % 2 != 0) return null
            return try {
                ByteArray(value.length / 2) { index ->
                    value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    data class Input(
        val secret: String = "",
        val keyEncoding: KeyEncoding = KeyEncoding.UTF8,
        val keyId: String = "",
        val subject: String = "",
        val phoneNumber: String = "",
        val email: String = "",
        val inject: Injection = Injection.NONE,
    )

    class InjectedFailure : Exception("Injected provider failure.")

    /**
     * Remembers the last minted token so the `replay` injection can re-send it verbatim,
     * original `jti` and `iat` included.
     */
    private val lastToken = AtomicReference<String?>(null)

    fun mint(input: Input): String {
        val now = System.currentTimeMillis() / 1000
        var issuedAt = now
        var phoneNumber: String? = input.phoneNumber.ifEmpty { null }
        var corruptSignature = false

        when (input.inject) {
            // Outside `maxTokenAge: '60s'` even after the 30s clock tolerance.
            Injection.EXPIRED -> issuedAt = now - 300
            Injection.NO_PHONE -> phoneNumber = null
            Injection.BAD_PHONE -> phoneNumber = "555-CALL-NOW"
            Injection.BAD_SIG -> corruptSignature = true
            Injection.MALFORMED -> return "not.a.jwt"
            Injection.ERROR -> throw InjectedFailure()
            Injection.REPLAY -> {
                // Nothing to replay on the first attempt — mint one, and the next call
                // sends this same token, `jti` included, back.
                lastToken.get()?.let { return it }
            }

            // Break the provider CALL rather than the token; applied in
            // HandshakeProviderFactory before this is ever reached.
            Injection.NONE, Injection.SLOW, Injection.TIMEOUT -> Unit
        }

        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
            if (input.keyId.isNotEmpty()) put("kid", input.keyId)
        }

        // Exactly the claims the verifier expects. No `exp`: freshness is `maxTokenAge`
        // over `iat`, and an extra claim is one more thing that can reject a good token.
        val claims = JSONObject().apply {
            put("sub", input.subject)
            put("iat", issuedAt)
            put("jti", UUID.randomUUID().toString())
            phoneNumber?.let { put("phone_number", it) }
            if (input.email.isNotEmpty()) put("email", input.email)
        }

        val signingInput = "${base64Url(header.toString().toByteArray(Charsets.UTF_8))}." +
                base64Url(claims.toString().toByteArray(Charsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(input.keyEncoding.keyBytes(input.secret), "HmacSHA256"))
        }.doFinal(signingInput.toByteArray(Charsets.UTF_8))
        if (corruptSignature) mac[0] = (mac[0].toInt() xor 0xFF).toByte()

        val token = "$signingInput.${base64Url(mac)}"
        lastToken.set(token)

        return token
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
