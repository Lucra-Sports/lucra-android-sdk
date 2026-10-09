package com.lucrasports.sdk.app

import android.content.Context
import com.lucrasports.sdk.core.LucraClient.Companion.Environment

internal const val SAMPLE_PREFS = "LucraSamplePrefs"
internal const val API_KEY_OVERRIDE = "API_KEY_OVERRIDE"

/** The placeholder [BuildConfig.TESTING_API_KEY] ships with, meaning "not configured". */
private const val API_KEY_PLACEHOLDER = "ADD YOUR API KEY HERE"

/**
 * Which Lucra backend this build talks to.
 *
 * Shared rather than private to [MainActivitySdk] because screens that only matter
 * against a particular backend — handshake auth, whose whole outcome depends on which
 * tenant's Auth0 Action verifies the token — need to be able to say which one is in play.
 */
internal fun sampleEnvironment(): Environment = when (BuildConfig.BUILD_TYPE) {
    "debug" -> Environment.DEVELOPMENT
    "staging" -> Environment.STAGING
    "sandbox" -> Environment.SANDBOX
    "release" -> Environment.PRODUCTION
    "dev2" -> Environment.DEVELOPMENT2
    else -> Environment.STAGING
}

/**
 * Whether [LucraClient.initialize] had a key to run with. It is skipped without one, and
 * every client accessor throws afterwards — which reads as "the SDK is broken" unless the
 * screen says the key is missing.
 */
internal fun isSampleApiKeyConfigured(context: Context): Boolean {
    if (BuildConfig.TESTING_API_KEY != API_KEY_PLACEHOLDER) return true

    return !context.applicationContext
        .getSharedPreferences(SAMPLE_PREFS, Context.MODE_PRIVATE)
        .getString(API_KEY_OVERRIDE, null)
        .isNullOrBlank()
}
