package com.lucrasports.sdk.app.headless_api

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The handshake harness's running log, scoped to the process rather than to the screen.
 *
 * The provider lambda is handed to the SDK and outlives whatever created it, so it cannot
 * write into an Activity — capturing one there leaks it for as long as the provider stays
 * registered. Writing here instead also means the lines from a sign-in the SDK ran on its
 * own (entering a Lucra flow with the screen closed) are still there when the screen is
 * next opened, which is exactly the run you want to read.
 */
internal object HandshakeLog {

    /** Enough to cover several sign-ins; the harness is not an audit trail. */
    private const val MAX_LINES = 200

    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Set and cleared on the main thread by whichever screen is showing the log. */
    private var listener: (() -> Unit)? = null

    /** Called from any thread — the provider runs on the SDK's IO dispatcher. */
    fun append(line: String) {
        synchronized(lock) {
            lines.addLast("${timestamp.format(Date())}  $line")
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
        mainHandler.post { listener?.invoke() }
    }

    /** Newest first, matching how the screen renders it. */
    fun newestFirst(): List<String> = synchronized(lock) { lines.toList().asReversed() }

    fun clear() {
        synchronized(lock) { lines.clear() }
        mainHandler.post { listener?.invoke() }
    }

    /** Pass null to stop observing; only one screen shows the log at a time. */
    fun observe(onChanged: (() -> Unit)?) {
        listener = onChanged
        onChanged?.invoke()
    }
}
