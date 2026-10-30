package com.lucrasports.sdk.app.headless_api

import android.content.Context
import androidx.core.content.edit
import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.app.headless_api.HandshakeTokenMinter.KeyEncoding

/**
 * Mirrors the users in `handshake-poc/src/partner-be/server.js`, and the same list the
 * iOS sample carries — both platforms resolve the same dev1 accounts.
 *
 * The test users' phones are in the 555-01xx range reserved for fiction, and each is
 * DISTINCT — the post-login chain resolves the Lucra user by phone, so two users sharing
 * one phone land on one Lucra account no matter what `sub` says. "Hack Joe's Account"
 * exists to demonstrate exactly that, with a real number.
 */
internal data class TestUser(
    /** The POC backend's login username, not the token's `sub` — see [subject]. */
    val id: String,
    val name: String,
    val phone: String,
    /**
     * What the POC puts in `sub`, mirrored here so a locally minted token carries the
     * same external identity as one fetched from the POC.
     */
    val subject: String,
    val email: String,
) {
    companion object {
        val all = listOf(
            TestUser("test1", "Test User 1", "+17045550109", "acme-test-0009", "test9@acme-sports.example"),
            TestUser("test2", "Test User 2", "+17045550110", "acme-test-0010", "test10@acme-sports.example"),
            TestUser("test3", "Test User 3", "+17045550111", "acme-test-0011", "test11@acme-sports.example"),
            TestUser("test4", "Test User 4", "+17045550112", "acme-test-0012", "test12@acme-sports.example"),
            TestUser("test5", "Test User 5", "+17045550113", "acme-test-0013", "test13@acme-sports.example"),
            // A different partner user asserting someone else's phone. The token is
            // entirely valid; the Lucra user is resolved by phone, so this lands on that
            // account.
            TestUser("hackjoe", "Hack Joe's Account", "+12628537663", "acme-hack-0001", "hackjoe@acme-sports.example"),
        )

        fun named(id: String): TestUser? = all.firstOrNull { it.id == id }
    }
}

/** The "Custom" entry in the partner user picker — free-typed sub/phone/email. */
internal const val CUSTOM_USER_ID = "custom"

/**
 * The Action rejects anything that is not E.164, and that rejection arrives as a generic
 * exchange failure — so a typo reads as "handshake auth is broken" unless it is called
 * out before the token is even minted.
 */
internal fun isE164(phone: String): Boolean = Regex("^\\+[1-9][0-9]{7,14}$").matches(phone)

/**
 * What the provider lambda signs (or fetches) with, read fresh from disk on every call.
 *
 * Deliberately a snapshot loaded per call rather than state captured at registration:
 * the lambda is handed to the SDK and outlives whatever screen created it, so a captured
 * value keeps whatever was selected when the provider was registered. On iOS that
 * produced a screen showing "test2" while minting tokens for test1, with nothing to
 * suggest the two had diverged.
 */
internal data class HandshakeConfig(
    val partnerBaseURL: String = DEFAULT_PARTNER_BASE_URL,
    val partnerUser: String = DEFAULT_PARTNER_USER,
    val tenant: String = DEFAULT_TENANT,
    val inject: Injection = Injection.NONE,
    val customSub: String = "",
    val customPhone: String = "",
    val customEmail: String = "",
    /** Default, because the partner backend is the extra moving part. */
    val mintsLocally: Boolean = true,
    val signingSecret: String = "",
    val keyEncoding: KeyEncoding = KeyEncoding.UTF8,
    val keyId: String = "",
    val bypassTosAgreement: Boolean = false,
) {

    val isCustom: Boolean get() = partnerUser == CUSTOM_USER_ID

    /**
     * The token's `sub` IS the account identity: once an identity exists, the exchange
     * resolves its Lucra user directly and the phone in the token is ignored — only a
     * NEW sub consults the phone (known phone links, unused phone creates). A fixed sub
     * therefore pins every custom run to one account no matter what phone is typed.
     * Deriving it from the phone keeps "new phone number = new user" true.
     */
    val effectiveCustomSub: String
        get() = customSub.ifEmpty { derivedSub(customPhone) }

    val subject: String
        get() = if (isCustom) effectiveCustomSub else TestUser.named(partnerUser)?.subject ?: partnerUser

    val phoneNumber: String
        get() = if (isCustom) customPhone else TestUser.named(partnerUser)?.phone.orEmpty()

    val email: String
        get() = if (isCustom) customEmail else TestUser.named(partnerUser)?.email.orEmpty()

    /** Empty means local minting can only throw — the caller routes to the partner backend. */
    val resolvedSecret: String get() = signingSecret.trim()

    /** Whether this config actually mints on device, honouring the environment gate. */
    val mintsLocallyNow: Boolean
        get() = mintsLocally && localMintingAvailable && resolvedSecret.isNotEmpty()

    fun toMinterInput() = HandshakeTokenMinter.Input(
        secret = resolvedSecret,
        keyEncoding = keyEncoding,
        keyId = keyId,
        subject = subject,
        phoneNumber = phoneNumber,
        email = email,
        inject = inject,
    )

    /** What the Token row shows without having to open it. */
    fun summary(): String = buildString {
        append(tenant)
        append(" · ")
        append(if (isCustom) "Custom" else TestUser.named(partnerUser)?.name ?: partnerUser)
        // Which side signed it is the first thing you want to know when the exchange fails.
        if (localMintingAvailable) append(if (mintsLocally) " · local" else " · partner BE")
        if (inject != Injection.NONE) append(" · ${inject.slug}")
    }

    companion object {
        /** Emulator localhost is the emulator itself; 10.0.2.2 is the host machine. */
        const val DEFAULT_PARTNER_BASE_URL = "http://10.0.2.2:4000"

        /**
         * The tenants the POC is configured for. Here rather than in the screen so the
         * picker, the default, and the fallback when a stored value no longer exists
         * cannot drift apart.
         */
        val TENANTS = listOf("SAMPLE", "LUCRA")

        const val DEFAULT_TENANT = "SAMPLE"

        const val DEFAULT_PARTNER_USER = "test1"

        /**
         * On-device signing is offered only where a disposable key is acceptable.
         * Production and sandbox hide the controls rather than presenting something that
         * should never be used there.
         */
        val localMintingAvailable: Boolean
            get() = BuildConfig.BUILD_TYPE in setOf("debug", "dev2", "staging")

        fun derivedSub(phone: String): String {
            val digits = phone.filter { it.isDigit() }
            return if (digits.isEmpty()) "custom-user" else "custom-$digits"
        }
    }
}

/**
 * Persistence for [HandshakeConfig], so a relaunch keeps whatever was being tested with.
 *
 * Reads go straight to SharedPreferences rather than through a cached instance — the
 * provider lambda calls [load] on every token, which is what makes changing the
 * injection or the phone number take effect on the next sign-in without re-registering.
 */
internal class HandshakeConfigStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isProviderRegistered: Boolean
        get() = prefs.getBoolean(KEY_REGISTERED, false)
        set(value) = prefs.edit { putBoolean(KEY_REGISTERED, value) }

    fun load(): HandshakeConfig {
        val defaults = HandshakeConfig()
        val customPhone = prefs.getString(KEY_PHONE, "").orEmpty()

        return HandshakeConfig(
            partnerBaseURL = prefs.getString(KEY_PARTNER_BASE_URL, null)
                ?: defaults.partnerBaseURL,
            // A harness configured before the picker existed only ever had free-typed
            // fields, so keep it on Custom rather than silently switching it to a seeded
            // user and minting for a different phone than it did yesterday.
            partnerUser = prefs.getString(KEY_PARTNER_USER, null)
                ?: if (customPhone.isNotEmpty()) CUSTOM_USER_ID else defaults.partnerUser,
            tenant = prefs.getString(KEY_TENANT, null) ?: defaults.tenant,
            inject = prefs.getString(KEY_INJECT, null)
                ?.let { Injection.fromSlug(it) } ?: defaults.inject,
            customSub = prefs.getString(KEY_SUBJECT, "").orEmpty(),
            customPhone = customPhone,
            customEmail = prefs.getString(KEY_EMAIL, "").orEmpty(),
            mintsLocally = prefs.getBoolean(KEY_MINTS_LOCALLY, defaults.mintsLocally),
            signingSecret = prefs.getString(KEY_SECRET, "").orEmpty(),
            keyEncoding = prefs.getString(KEY_ENCODING, null)
                ?.let { name -> KeyEncoding.entries.firstOrNull { it.name == name } }
                ?: defaults.keyEncoding,
            keyId = prefs.getString(KEY_KEY_ID, "").orEmpty(),
            bypassTosAgreement = prefs.getBoolean(KEY_BYPASS_TOS, false),
        )
    }

    fun save(config: HandshakeConfig) = prefs.edit {
        putString(KEY_PARTNER_BASE_URL, config.partnerBaseURL)
        putString(KEY_PARTNER_USER, config.partnerUser)
        putString(KEY_TENANT, config.tenant)
        putString(KEY_INJECT, config.inject.slug)
        putString(KEY_SUBJECT, config.customSub)
        putString(KEY_PHONE, config.customPhone)
        putString(KEY_EMAIL, config.customEmail)
        putBoolean(KEY_MINTS_LOCALLY, config.mintsLocally)
        putString(KEY_SECRET, config.signingSecret)
        putString(KEY_ENCODING, config.keyEncoding.name)
        putString(KEY_KEY_ID, config.keyId)
        putBoolean(KEY_BYPASS_TOS, config.bypassTosAgreement)
    }

    private companion object {
        const val PREFS_NAME = "handshake_auth_harness"
        const val KEY_PARTNER_BASE_URL = "partner_base_url"
        const val KEY_PARTNER_USER = "partner_user"
        const val KEY_TENANT = "tenant"
        const val KEY_SECRET = "secret"
        const val KEY_ENCODING = "key_encoding"
        const val KEY_KEY_ID = "key_id"
        const val KEY_SUBJECT = "subject"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_INJECT = "inject"
        const val KEY_REGISTERED = "registered"
        const val KEY_BYPASS_TOS = "bypass_tos"
        const val KEY_MINTS_LOCALLY = "mints_locally"
    }
}
