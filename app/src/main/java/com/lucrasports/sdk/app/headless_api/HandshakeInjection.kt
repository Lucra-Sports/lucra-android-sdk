package com.lucrasports.sdk.app.headless_api

/**
 * Failure injections, so the fallback to phone auth can be watched actually happening.
 *
 * The slug — not the enum name — is what is persisted and what the POC backend's
 * `?inject=` parameter takes, so the spelling lives here once rather than in a `when` in
 * [HandshakeTokenMinter], another in [HandshakeProviderFactory], and a picker in the UI.
 *
 * [expected] is shown under the picker. A tester who did not write the minter has no way
 * to tell a working injection from a broken integration otherwise: both look like "the
 * phone screen appeared".
 *
 * `wrong-aud` and `unknown-iss` are deliberately absent: the deployed Action verifies no
 * `iss` and no `aud`, so those tokens exchange successfully and the injection would prove
 * nothing. The tenant is proven by the signature, which [BAD_SIG] already covers.
 */
internal enum class Injection(
    val slug: String,
    val label: String,
    val expected: String,
) {
    NONE(
        "none",
        "None — valid token",
        "The exchange should succeed and you should land signed in.",
    ),
    ERROR(
        "error",
        "Provider throws",
        "The lambda throws before returning a token: ProviderFailed, then the phone screen.",
    ),

    // Breaks the provider CALL rather than the token, so it is applied in
    // HandshakeProviderFactory. Still inside the SDK's 5s provider deadline.
    SLOW(
        "slow",
        "Slow provider (3s)",
        "Returns inside the SDK's 5s provider deadline — sluggish, but it still signs in.",
    ),
    TIMEOUT(
        "timeout",
        "Provider timeout (6s)",
        "Outlasts the SDK's 5s provider deadline: ProviderTimedOut, then the phone screen.",
    ),

    MALFORMED(
        "malformed",
        "Malformed token",
        "Returns something that is not a JWT at all: ExchangeFailed.",
    ),
    NO_PHONE(
        "no-phone",
        "No phone_number claim",
        "The exchange has no phone to resolve a Lucra user by; expect it to be rejected.",
    ),
    BAD_PHONE(
        "bad-phone",
        "Non-E.164 phone",
        "Sends 555-CALL-NOW. The Action rejects anything that is not E.164.",
    ),
    EXPIRED(
        "expired",
        "Stale iat (5 min old)",
        "Outside maxTokenAge 60s even after the 30s clock tolerance: ExchangeFailed.",
    ),
    BAD_SIG(
        "bad-sig",
        "Corrupt signature",
        "Claims are untouched, one signature byte is flipped: ExchangeFailed.",
    ),
    REPLAY(
        "replay",
        "Replay the last token",
        "Re-sends the previous token verbatim, jti included, so replay protection can reject it.",
    );

    companion object {
        /** Unknown slugs — a stale preference, a typo'd intent extra — read as [NONE]. */
        fun fromSlug(slug: String): Injection =
            entries.firstOrNull { it.slug == slug } ?: NONE
    }
}
