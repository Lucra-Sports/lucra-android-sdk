package com.lucrasports.sdk.app.headless_api

import android.util.Base64
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * The last token the provider handed the SDK, kept so the screen can show it.
 *
 * The log deliberately truncates tokens — a full JWT per line makes it unreadable — but a
 * rejected exchange is exactly when you want the whole thing to paste into a decoder, and
 * the decoded claims to compare against what the screen said it would mint. Nobody should
 * have to re-run a scenario with a debugger attached to see the token that failed.
 *
 * Process-scoped for the same reason [HandshakeLog] is: the provider outlives the screen,
 * and a sign-in the SDK ran on its own is the run most worth inspecting.
 *
 * Separate from [HandshakeTokenMinter]'s own record of the last MINTED token, which backs
 * the `replay` injection and must not start replaying tokens the partner backend issued.
 */
internal object HandshakeLastToken {

    /** Where the token came from, so the claims are read with the right expectations. */
    enum class Source(val label: String) {
        LOCAL("minted on device"),
        PARTNER("partner backend"),
    }

    data class Record(val token: String, val source: Source)

    private val current = AtomicReference<Record?>(null)

    fun record(token: String, source: Source) {
        current.set(Record(token, source))
    }

    fun latest(): Record? = current.get()

    fun clear() = current.set(null)

    /**
     * Header and payload, pretty-printed. The signature is not shown: it is opaque bytes,
     * and [Record.token] is what you copy when you want to verify it somewhere else.
     *
     * Returns null when there is nothing to decode — which for the `malformed` injection
     * is the point, and the caller says so rather than showing an empty box.
     */
    fun decoded(): String? {
        val token = current.get()?.token ?: return null
        val parts = token.split(".")
        if (parts.size < 2) return null

        val header = decodeSegment(parts[0]) ?: return null
        val payload = decodeSegment(parts[1]) ?: return null

        return "header\n$header\n\npayload\n$payload"
    }

    private fun decodeSegment(segment: String): String? = try {
        val json = String(
            Base64.decode(segment, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
            Charsets.UTF_8,
        )
        JSONObject(json).toString(2)
    } catch (e: Exception) {
        null
    }
}
