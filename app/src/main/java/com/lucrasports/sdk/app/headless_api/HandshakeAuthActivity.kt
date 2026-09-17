package com.lucrasports.sdk.app.headless_api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.isSampleApiKeyConfigured
import com.lucrasports.sdk.app.sampleEnvironment
import com.lucrasports.sdk.app.ui.LucraFlowPresenter
import com.lucrasports.sdk.app.headless_api.HandshakeTokenMinter.KeyEncoding
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.auth.HandshakeAuthError
import com.lucrasports.sdk.core.auth.SignInWithHandshakeAuthResult
import com.lucrasports.sdk.core.ui.LucraUiProvider.LucraFlow
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Drives handshake auth end-to-end, playing the part of the PARTNER APP: it stands in for
 * a partner backend that authenticates its own user and hands Lucra a signed token. In a
 * real integration the provider lambda would call the integrator's own API with their own
 * session; here it either mints an equivalently shaped token on device or fetches one from
 * the handshake POC's partner backend. No signing key ever lives in a real integration.
 *
 * Registering and signing in are separate SDK calls: registering only stores the lambda,
 * and nothing happens until sign-in is triggered — either by the button here, or by
 * entering a Lucra flow that needs a session.
 *
 * Everything the screen reports is derived from [LucraClient], never from local flags. The
 * SDK signs in on its own — entering a Lucra flow runs the handshake without this screen
 * being involved — so a flag only written by these buttons would report "Idle" through all
 * of it.
 */
internal class HandshakeAuthActivity : AppCompatActivity() {

    private val store by lazy { HandshakeConfigStore(this) }

    /** So the profile is logged once per user, not on every republish. */
    private var loggedUserId: String? = null

    // Live SDK state, each written by its own collector below.
    private var isInFlight = false
    private var currentUser: SDKUser? = null
    private var lastError: HandshakeAuthError? = null

    /** When [lastError] was observed, so a card left over from an earlier run reads as one. */
    private var lastErrorAt: String? = null

    private val toastHandler = Handler(Looper.getMainLooper())

    /** Reads the store rather than the widgets, so it reports what was actually written. */
    private val saveToast = Runnable {
        Toast.makeText(this, "Saved · ${store.load().summary()}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Suppresses the persist-on-change listeners while [bindConfig] populates the widgets.
     * Spinner adapters fire onItemSelected during setup, which would otherwise write the
     * defaults back over whatever was loaded.
     */
    private var isBinding = false

    private lateinit var environmentChip: TextView
    private lateinit var statusDot: View
    private lateinit var statusLabel: TextView
    private lateinit var statusProgress: ProgressBar
    private lateinit var failureSection: View
    private lateinit var failureHeader: TextView
    private lateinit var failureType: TextView
    private lateinit var failureMessage: TextView
    private lateinit var failureSuggestion: TextView
    private lateinit var providerStatusLabel: TextView
    private lateinit var providerBody: View
    private lateinit var providerChevron: ImageView
    private lateinit var providerSourceLabel: View
    private lateinit var sourceLocal: RadioButton
    private lateinit var sourcePartner: RadioButton
    private lateinit var partnerBaseUrl: EditText
    private lateinit var tokenSummary: TextView
    private lateinit var tokenBody: View
    private lateinit var tokenChevron: ImageView
    private lateinit var signingSection: View
    private lateinit var signingSecret: EditText
    private lateinit var keyEncoding: Spinner
    private lateinit var keyId: EditText
    private lateinit var tenant: Spinner
    private lateinit var partnerUser: Spinner
    private lateinit var partnerUserPhone: TextView
    private lateinit var customUserSection: View
    private lateinit var customSub: EditText
    private lateinit var customSubHint: TextView
    private lateinit var customPhone: EditText
    private lateinit var phoneWarning: View
    private lateinit var customEmail: EditText
    private lateinit var inject: Spinner
    private lateinit var injectExpected: TextView
    private lateinit var bypassTos: CheckBox
    private lateinit var tokenInspectSummary: TextView
    private lateinit var tokenInspectBody: View
    private lateinit var tokenInspectChevron: ImageView
    private lateinit var tokenClaims: TextView
    private lateinit var copyToken: Button
    private lateinit var logOutput: TextView

    /** Value order for the pickers; the adapters show labels derived from these. */
    private val partnerUserIds = TestUser.all.map { it.id } + CUSTOM_USER_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handshake_auth)

        setSupportActionBar(findViewById<Toolbar>(R.id.handshake_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Read before the auto-registration below flips it: it is the only signal for
        // "first ever visit", which is when the thing you have to configure should not be
        // hidden behind a chevron.
        val neverRegistered = !store.isProviderRegistered

        bindViews()
        setUpPickers()
        bindConfig(store.load())
        wireListeners()
        observeSdkState()

        // Once per screen instance, not per resume: re-entering after "Clear" must not
        // silently re-register, or the cleared state can never be observed.
        if (savedInstanceState == null && neverRegistered) registerProvider()

        restoreSections(savedInstanceState, expandTokenByDefault = neverRegistered)

        renderEnvironment()
        renderStatus()
        renderLastToken()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Otherwise a rotation mid-scenario folds away everything you had open.
        outState.putBoolean(STATE_PROVIDER_EXPANDED, providerBody.isVisible())
        outState.putBoolean(STATE_TOKEN_EXPANDED, tokenBody.isVisible())
        outState.putBoolean(STATE_LAST_TOKEN_EXPANDED, tokenInspectBody.isVisible())
    }

    override fun onStart() {
        super.onStart()
        // Also picks up lines written while this screen was away — a sign-in the SDK ran
        // on its own is the run most worth reading.
        HandshakeLog.observe {
            renderLog()
            // A new token lands with the log lines that report it.
            renderLastToken()
        }
    }

    override fun onStop() {
        HandshakeLog.observe(null)
        super.onStop()
    }

    override fun onDestroy() {
        toastHandler.removeCallbacks(saveToast)
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // region View wiring

    private fun bindViews() {
        environmentChip = findViewById(R.id.environment_chip)
        statusDot = findViewById(R.id.status_dot)
        statusLabel = findViewById(R.id.status_label)
        statusProgress = findViewById(R.id.status_progress)
        failureSection = findViewById(R.id.failure_section)
        failureHeader = findViewById(R.id.failure_header)
        failureType = findViewById(R.id.failure_type)
        failureMessage = findViewById(R.id.failure_message)
        failureSuggestion = findViewById(R.id.failure_suggestion)
        providerStatusLabel = findViewById(R.id.provider_status_label)
        providerBody = findViewById(R.id.provider_body)
        providerChevron = findViewById(R.id.provider_chevron)
        providerSourceLabel = findViewById(R.id.provider_source_label)
        sourceLocal = findViewById(R.id.source_local)
        sourcePartner = findViewById(R.id.source_partner)
        partnerBaseUrl = findViewById(R.id.partner_base_url)
        tokenSummary = findViewById(R.id.token_summary)
        tokenBody = findViewById(R.id.token_body)
        tokenChevron = findViewById(R.id.token_chevron)
        signingSection = findViewById(R.id.signing_section)
        signingSecret = findViewById(R.id.signing_secret)
        keyEncoding = findViewById(R.id.key_encoding)
        keyId = findViewById(R.id.key_id)
        tenant = findViewById(R.id.tenant)
        partnerUser = findViewById(R.id.partner_user)
        partnerUserPhone = findViewById(R.id.partner_user_phone)
        customUserSection = findViewById(R.id.custom_user_section)
        customSub = findViewById(R.id.custom_sub)
        customSubHint = findViewById(R.id.custom_sub_hint)
        customPhone = findViewById(R.id.custom_phone)
        phoneWarning = findViewById(R.id.phone_warning)
        customEmail = findViewById(R.id.custom_email)
        inject = findViewById(R.id.inject)
        injectExpected = findViewById(R.id.inject_expected)
        bypassTos = findViewById(R.id.bypass_tos)
        tokenInspectSummary = findViewById(R.id.token_inspect_summary)
        tokenInspectBody = findViewById(R.id.token_inspect_body)
        tokenInspectChevron = findViewById(R.id.token_inspect_chevron)
        tokenClaims = findViewById(R.id.token_claims)
        copyToken = findViewById(R.id.copy_token)
        logOutput = findViewById(R.id.log_output)

        // Production and sandbox ship no disposable key, so the on-device signing controls
        // stay hidden there rather than offering something that cannot work.
        if (!HandshakeConfig.localMintingAvailable) {
            signingSection.visibility = View.GONE
            providerSourceLabel.visibility = View.GONE
            findViewById<View>(R.id.provider_source).visibility = View.GONE
        }
    }

    private fun setUpPickers() {
        fun adapter(values: List<String>) = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            values,
        )

        keyEncoding.adapter = adapter(KeyEncoding.entries.map { it.label })
        tenant.adapter = adapter(HandshakeConfig.TENANTS)
        partnerUser.adapter = adapter(
            TestUser.all.map { it.name } + "Custom",
        )
        inject.adapter = adapter(Injection.entries.map { it.label })
    }

    /**
     * Sections keep whatever was open across a recreate. On a first ever visit there is
     * nothing to keep and nothing yet configured, so Token opens: who you are signing in
     * as is the one thing that has to be set before any of this means anything.
     */
    private fun restoreSections(savedInstanceState: Bundle?, expandTokenByDefault: Boolean) {
        setExpanded(
            providerBody,
            providerChevron,
            savedInstanceState?.getBoolean(STATE_PROVIDER_EXPANDED) ?: false,
        )
        setExpanded(
            tokenBody,
            tokenChevron,
            savedInstanceState?.getBoolean(STATE_TOKEN_EXPANDED) ?: expandTokenByDefault,
        )
        setExpanded(
            tokenInspectBody,
            tokenInspectChevron,
            savedInstanceState?.getBoolean(STATE_LAST_TOKEN_EXPANDED) ?: false,
        )
    }

    private fun bindConfig(config: HandshakeConfig) {
        isBinding = true

        sourceLocal.isChecked = config.mintsLocally
        sourcePartner.isChecked = !config.mintsLocally
        partnerBaseUrl.setText(config.partnerBaseURL)
        signingSecret.setText(config.signingSecret)
        keyEncoding.setSelection(KeyEncoding.entries.indexOf(config.keyEncoding))
        keyId.setText(config.keyId)
        tenant.setSelection(HandshakeConfig.TENANTS.indexOf(config.tenant).coerceAtLeast(0))
        partnerUser.setSelection(partnerUserIds.indexOf(config.partnerUser).coerceAtLeast(0))
        customSub.setText(config.customSub)
        customPhone.setText(config.customPhone)
        customEmail.setText(config.customEmail)
        inject.setSelection(Injection.entries.indexOf(config.inject).coerceAtLeast(0))
        bypassTos.isChecked = config.bypassTosAgreement

        isBinding = false
        renderEditor()
    }

    private fun wireListeners() {
        findViewById<View>(R.id.provider_header).setOnClickListener {
            toggle(providerBody, providerChevron)
        }
        findViewById<View>(R.id.token_header).setOnClickListener {
            toggle(tokenBody, tokenChevron)
        }
        findViewById<View>(R.id.token_inspect_header).setOnClickListener {
            toggle(tokenInspectBody, tokenInspectChevron)
        }

        findViewById<Button>(R.id.register_provider).setOnClickListener { registerProvider() }
        findViewById<Button>(R.id.clear_provider).setOnClickListener { clearProvider() }
        findViewById<Button>(R.id.sign_in).setOnClickListener { signIn() }
        findViewById<Button>(R.id.sign_out).setOnClickListener { signOut() }
        findViewById<Button>(R.id.present_tos).setOnClickListener { present(LucraFlow.HandshakeTOS) }
        findViewById<Button>(R.id.open_profile).setOnClickListener { present(LucraFlow.Profile) }
        findViewById<Button>(R.id.how_to_test).setOnClickListener { showScenarioList() }
        findViewById<Button>(R.id.reset_defaults).setOnClickListener { resetToDefaults() }
        copyToken.setOnClickListener { copyLastToken() }

        findViewById<Button>(R.id.clear_log).setOnClickListener { HandshakeLog.clear() }

        listOf(
            partnerBaseUrl, signingSecret, keyId, customSub, customPhone, customEmail,
        ).forEach { it.addTextChangedListener(PersistOnChange()) }

        listOf(keyEncoding, tenant, partnerUser, inject).forEach {
            it.onItemSelectedListener = PersistOnSelection()
        }

        bypassTos.setOnCheckedChangeListener { _, checked ->
            if (isBinding) return@setOnCheckedChangeListener
            persist()
            // The one control here the provider does not re-read: it is handed to the SDK
            // when the provider is stored, so applying it means registering again.
            if (store.isProviderRegistered) registerProvider(silent = true)
            append("TOS bypass ${if (checked) "requested" else "not requested"}")
        }

        val onSourceChanged = { _: View ->
            if (!isBinding) persist()
        }
        sourceLocal.setOnClickListener(onSourceChanged)
        sourcePartner.setOnClickListener(onSourceChanged)
    }

    private fun toggle(section: View, chevron: ImageView) {
        val expanding = !section.isVisible()
        section.visibility = if (expanding) View.VISIBLE else View.GONE
        chevron.animate()
            .rotation(if (expanding) EXPANDED_ROTATION else COLLAPSED_ROTATION)
            .setDuration(150)
            .start()
    }

    /** [toggle] without the animation, for restoring state before the screen is drawn. */
    private fun setExpanded(section: View, chevron: ImageView, expanded: Boolean) {
        section.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.rotation = if (expanded) EXPANDED_ROTATION else COLLAPSED_ROTATION
    }

    private fun View.isVisible() = visibility == View.VISIBLE

    private inner class PersistOnChange : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            if (!isBinding) persist()
        }
    }

    private inner class PersistOnSelection : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!isBinding) persist()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    // endregion

    // region Scenario guide

    /**
     * What to actually do with this screen.
     *
     * The controls say what they set, not what any of it is FOR, and the paths that matter
     * are the ones that look broken when they are working: the ToS gate that only fires
     * for users who do not exist yet, the failure that is supposed to fall through to
     * phone auth, the sign-out that signs you straight back in on the next tap.
     */
    private fun showScenarioList() {
        MaterialAlertDialogBuilder(this)
            .setTitle("How to test different scenarios")
            .setItems(HandshakeScenarios.all.map { it.title }.toTypedArray()) { _, index ->
                showScenario(HandshakeScenarios.all[index])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showScenario(scenario: Scenario) {
        MaterialAlertDialogBuilder(this)
            .setTitle(scenario.title)
            .setMessage(scenario.body())
            // Back rather than only Close: these are read side by side, and reopening the
            // list from the top for every one of them gets old fast.
            .setNegativeButton("Back") { _, _ -> showScenarioList() }
            .setPositiveButton("Got it", null)
            .show()
    }

    // endregion

    // region Config

    /** The widgets are the source of truth while this screen is up; disk follows them. */
    private fun readConfig() = HandshakeConfig(
        partnerBaseURL = partnerBaseUrl.text.toString().trim()
            .ifEmpty { HandshakeConfig.DEFAULT_PARTNER_BASE_URL },
        partnerUser = partnerUserIds
            .getOrElse(partnerUser.selectedItemPosition) { HandshakeConfig.DEFAULT_PARTNER_USER },
        tenant = HandshakeConfig.TENANTS
            .getOrElse(tenant.selectedItemPosition) { HandshakeConfig.DEFAULT_TENANT },
        inject = Injection.entries.getOrElse(inject.selectedItemPosition) { Injection.NONE },
        customSub = customSub.text.toString().trim(),
        customPhone = customPhone.text.toString().trim(),
        customEmail = customEmail.text.toString().trim(),
        mintsLocally = sourceLocal.isChecked,
        signingSecret = signingSecret.text.toString(),
        keyEncoding = KeyEncoding.entries
            .getOrElse(keyEncoding.selectedItemPosition) { KeyEncoding.UTF8 },
        keyId = keyId.text.toString().trim(),
        bypassTosAgreement = bypassTos.isChecked,
    )

    /**
     * Commits what is on screen, on every change.
     *
     * The provider lambda loads the STORED config on every call, so anything left only in
     * a widget is not what gets minted. An explicit Save made that gap something you could
     * forget to close, and split writing between here and [registerProvider] — which reads
     * the widgets too, and so committed "unsaved" edits behind the screen's own back.
     */
    private fun persist() {
        saveConfig(readConfig())
        renderEditor()
    }

    /**
     * The single write path, so every save is confirmed the same way.
     *
     * Debounced because the config is written on every keystroke: undebounced, typing a
     * phone number queues twelve toasts that go on stacking up long after you have moved
     * on to the next field.
     */
    private fun saveConfig(config: HandshakeConfig) {
        store.save(config)
        toastHandler.removeCallbacks(saveToast)
        toastHandler.postDelayed(saveToast, SAVE_TOAST_DEBOUNCE_MS)
    }

    /** Live feedback for the fields being edited. Everything here follows the widgets. */
    private fun renderEditor() {
        val config = readConfig()

        customUserSection.visibility = if (config.isCustom) View.VISIBLE else View.GONE
        partnerUserPhone.text = if (config.isCustom) "" else config.phoneNumber
        customSubHint.text = if (config.customSub.isEmpty()) {
            "sub is empty — minting as ${HandshakeConfig.derivedSub(config.customPhone)}"
        } else {
            ""
        }
        phoneWarning.visibility =
            if (config.customPhone.isNotEmpty() && !isE164(config.customPhone)) {
                View.VISIBLE
            } else {
                View.GONE
            }

        // Slugs alone say nothing about what an injection should do, and a working
        // injection looks exactly like a broken integration: the phone screen appears.
        injectExpected.text = config.inject.expected

        tokenSummary.text = config.summary()
    }

    /**
     * Back to a config that is known to work, without clearing app data — a harness whose
     * only recovery from a wedged field is uninstalling it gets abandoned mid-scenario.
     */
    private fun resetToDefaults() {
        val defaults = HandshakeConfig()
        saveConfig(defaults)
        bindConfig(defaults)
        HandshakeLastToken.clear()

        if (store.isProviderRegistered) registerProvider(silent = true)

        append("harness reset to defaults — ${defaults.summary()}")
        renderLastToken()
        renderStatus()
    }

    // endregion

    // region Actions

    /**
     * THE ENTIRE CLIENT-SIDE INTEGRATION. A real integrator's lambda body would call their
     * own backend with their own session instead of minting or calling the POC.
     */
    private fun registerProvider(silent: Boolean = false) {
        var config = readConfig()

        // With no signing key resolvable, local minting can only throw — route the
        // provider to the partner backend instead, and say so rather than leaving the
        // screen claiming it will sign on device.
        if (config.mintsLocally && config.resolvedSecret.isEmpty()) {
            config = config.copy(mintsLocally = false)
            isBinding = true
            sourceLocal.isChecked = false
            sourcePartner.isChecked = true
            isBinding = false
            append("no signing secret set — provider will fetch from the partner backend")
        }

        saveConfig(config)
        renderEditor()

        try {
            HandshakeProviderFactory.register(this)
        } catch (e: IllegalStateException) {
            // LucraClient.initialize was skipped (no API key set); nothing to register on.
            append("FAILED to register: ${e.message}")
            return
        }

        if (!silent) {
            append(
                "provider registered -> " +
                        if (config.mintsLocallyNow) "local minting" else config.partnerBaseURL
            )
        }
        renderStatus()
    }

    private fun clearProvider() {
        try {
            HandshakeProviderFactory.clear(this)
        } catch (e: IllegalStateException) {
            append("SDK not initialized — nothing to clear")
            return
        }
        append("provider cleared — Lucra flows will use phone auth again")
        renderStatus()
    }

    private fun signIn() {
        val config = store.load()
        append(
            "sign in (tenant=${config.tenant} user=${config.partnerUser} " +
                    "inject=${config.inject.slug})"
        )

        withClient { client ->
            client.signInWithHandshakeAuth { result ->
                runOnUiThread {
                    when (result) {
                        is SignInWithHandshakeAuthResult.Success ->
                            append("exchange ok — signed in as ${result.sdkUser.username ?: result.sdkUser.userId}")

                        is SignInWithHandshakeAuthResult.Failure -> onSignInFailed(result.error)
                    }
                }
            }
        }
    }

    /**
     * THE PAIRING: every failure here falls back to phone auth except one. TosNotAccepted
     * means the exchange worked in every way but the agreement the user has not been asked
     * for yet, and the flow that captures it resubmits the sign-in itself.
     */
    private fun onSignInFailed(error: HandshakeAuthError) {
        if (error is HandshakeAuthError.TosNotAccepted) {
            append("TOS not accepted — presenting the Terms of Service flow")
            present(LucraFlow.HandshakeTOS)
            return
        }

        append("FAILED: ${error.message}")
        append("check: ${error.recoverySuggestion}")
    }

    private fun signOut() {
        withClient { it.logout(this) }
        loggedUserId = null // so the next sign-in logs its profile too
        append("signed out — background traffic stays out; a tap or flow signs back in")
    }

    // Through the presenter, not this activity's FragmentManager directly: the SDK's exit
    // request goes to the one listener registered from MainActivitySdk, which cannot see
    // fragments shown here.
    private fun present(flow: LucraFlow) = withClient {
        LucraFlowPresenter.present(this, flow)
    }

    // endregion

    // region SDK state

    private fun observeSdkState() = withClient { client ->
        client.isHandshakeAuthInFlight
            .onEach {
                isInFlight = it
                renderStatus()
            }
            .catch { append("stopped observing in-flight state: ${it.message}") }
            .launchIn(lifecycleScope)

        client.handshakeAuthError
            .onEach {
                lastError = it
                lastErrorAt = it?.let { _ -> stamp() }
                renderStatus()
            }
            .catch { append("stopped observing handshake errors: ${it.message}") }
            .launchIn(lifecycleScope)

        // The user arrives after the exchange resolves, so log it when it lands rather
        // than reading it at sign-in time and getting nothing.
        client.observeSDKUserFlow()
            .onEach { result ->
                currentUser = (result as? SDKUserResult.Success)?.sdkUser
                currentUser?.userId?.takeIf { it != loggedUserId }?.let { id ->
                    loggedUserId = id
                    append("profile loaded: ${currentUser?.username ?: id}")
                }
                renderStatus()
            }
            .catch { append("stopped observing the user: ${it.message}") }
            .launchIn(lifecycleScope)
    }

    /**
     * Every LucraClient accessor throws until [LucraClient.initialize] has run, which the
     * sample skips when no API key is set. One report beats a crash per button.
     */
    private inline fun withClient(block: (LucraClient) -> Unit) {
        try {
            block(LucraClient())
        } catch (e: IllegalStateException) {
            append("SDK not initialized — set an API key in the sample's settings first")
        }
    }

    private fun renderStatus() {
        val registered = store.isProviderRegistered
        val user = currentUser?.takeIf { !it.userId.isNullOrEmpty() }

        val (color, label) = when {
            isInFlight -> ORANGE to "Sign-in in flight"
            // The session exists before the profile arrives, so this fills in late.
            user != null -> GREEN to "Signed in: ${user.username ?: user.userId}"
            lastError != null -> RED to "Failed: ${lastError?.message}"
            registered -> GRAY to "Idle — provider ready"
            else -> GRAY to "Idle"
        }

        statusDot.backgroundTintList = ColorStateList.valueOf(color)
        statusLabel.text = label
        statusProgress.visibility = if (isInFlight) View.VISIBLE else View.GONE

        providerStatusLabel.text = if (registered) "Registered" else "Not registered"
        providerStatusLabel.setTextColor(if (registered) GREEN else GRAY)

        // Handshake failures fall back to phone auth silently, so without this the only
        // symptom is "the phone screen appeared" with no reason given.
        val error = lastError
        failureSection.visibility = if (error == null) View.GONE else View.VISIBLE
        if (error != null) {
            // The SDK clears this on SUCCESS, not when the next attempt starts, so while
            // a retry is in flight this is still the previous run's failure. Unlabelled,
            // it reads as the live one.
            failureHeader.text = buildString {
                append("Last failure")
                lastErrorAt?.let { append(" · $it") }
                if (isInFlight) append(" (previous attempt)")
            }
            // The type is what maps to an expectation — ProviderTimedOut and
            // ExchangeFailed are different bugs with very similar-sounding messages.
            failureType.text = error::class.simpleName.orEmpty()
            failureMessage.text = error.message
            failureSuggestion.text = error.recoverySuggestion
        }
    }

    /**
     * Which backend verifies the token decides the outcome of everything on this screen,
     * and nothing else here says which one is in play. Without the API key line, a build
     * that skipped [com.lucrasports.sdk.core.LucraClient.initialize] reads as an SDK that
     * throws on every button.
     */
    private fun renderEnvironment() {
        environmentChip.text = listOf(
            BuildConfig.BUILD_TYPE,
            sampleEnvironment().name,
            if (isSampleApiKeyConfigured(this)) "API key set" else "NO API KEY",
            if (HandshakeConfig.localMintingAvailable) {
                "on-device signing available"
            } else {
                "partner BE only"
            },
        ).joinToString(" · ")
    }

    /**
     * The log truncates tokens to keep it readable, but a rejected exchange is exactly
     * when the whole thing — and its claims — is what you need.
     */
    private fun renderLastToken() {
        val record = HandshakeLastToken.latest()

        if (record == null) {
            tokenInspectSummary.text = "none yet"
            tokenClaims.text =
                "No token yet — sign in, or enter a Lucra flow, and it lands here."
            copyToken.isEnabled = false
            return
        }

        tokenInspectSummary.text = record.source.label
        tokenClaims.text = HandshakeLastToken.decoded()
            // Which is the whole point of the malformed injection, so say so rather than
            // showing an empty box.
            ?: "Not a decodable JWT — what the malformed injection produces.\n\n${record.token}"
        copyToken.isEnabled = true
    }

    private fun copyLastToken() {
        val token = HandshakeLastToken.latest()?.token ?: return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Lucra handshake token", token))
        append("token copied to the clipboard")
    }

    // endregion

    private fun append(line: String) = HandshakeLog.append(line)

    private fun renderLog() {
        logOutput.text = HandshakeLog.newestFirst().joinToString("\n")
    }

    private fun stamp(): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    companion object {
        /** Chevron points at the row it opens when collapsed, and down when open. */
        private const val COLLAPSED_ROTATION = -90f
        private const val EXPANDED_ROTATION = 0f

        /** Long enough to cover typing, short enough to still feel like a confirmation. */
        private const val SAVE_TOAST_DEBOUNCE_MS = 500L

        private const val STATE_PROVIDER_EXPANDED = "provider_expanded"
        private const val STATE_TOKEN_EXPANDED = "token_expanded"
        private const val STATE_LAST_TOKEN_EXPANDED = "last_token_expanded"

        private val GRAY = Color.parseColor("#9E9E9E")
        private val ORANGE = Color.parseColor("#FFA726")
        private val GREEN = Color.parseColor("#66BB6A")
        private val RED = Color.parseColor("#FF6B6B")

        fun intent(context: Context) = Intent(context, HandshakeAuthActivity::class.java)
    }
}
