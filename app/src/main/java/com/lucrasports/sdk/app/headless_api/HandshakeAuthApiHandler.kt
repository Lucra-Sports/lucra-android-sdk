package com.lucrasports.sdk.app.headless_api

import androidx.appcompat.app.AppCompatActivity

/**
 * Entry point for the handshake auth harness.
 *
 * The harness itself is [HandshakeAuthActivity]; this exists for the two things the host
 * activity needs from it — opening it, and re-registering the provider at app start.
 */
internal class HandshakeAuthApiHandler(private val activity: AppCompatActivity) {

    /**
     * Re-registers on app start when a provider was registered before, so an organic flow
     * entry (Wallet, Home…) exercises the handshake with no menu visit — the way a real
     * integration registers from Application.onCreate.
     */
    fun autoRegisterIfEnabled() {
        if (!HandshakeConfigStore(activity).isProviderRegistered) return
        try {
            HandshakeProviderFactory.register(activity)
        } catch (e: IllegalStateException) {
            // LucraClient.initialize was skipped (no API key set); nothing to register on.
        }
    }

    fun showHandshakeAuthScreen() {
        activity.startActivity(HandshakeAuthActivity.intent(activity))
    }
}
