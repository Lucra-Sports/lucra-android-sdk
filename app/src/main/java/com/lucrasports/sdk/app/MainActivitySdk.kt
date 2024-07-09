package com.lucrasports.sdk.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.androidParameters
import com.google.firebase.dynamiclinks.iosParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.navigationInfoParameters
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.lucrasports.sdk.app.notifications.FCMService
import com.lucrasports.sdk.app.theming.SampleColorStore
import com.lucrasports.sdk.app.theming.SampleColorStore.intToColorHex
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.LucraClient.Companion.Environment
import com.lucrasports.sdk.core.contest.GamesMatchup
import com.lucrasports.sdk.core.contest.SportsMatchup
import com.lucrasports.sdk.core.events.LucraEvent
import com.lucrasports.sdk.core.events.LucraEventListener
import com.lucrasports.sdk.core.style_guide.ClientTheme
import com.lucrasports.sdk.core.style_guide.Font
import com.lucrasports.sdk.core.style_guide.FontFamily
import com.lucrasports.sdk.core.ui.LucraFlowListener
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult
import com.lucrasports.sdk.ui.LucraUi
import com.lucrasports.sdk.ui.push_notifications.LucraPushNotificationService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

class MainActivitySdk : AppCompatActivity(), ColorPickerDialogListener {

    private val header: TextView by lazy {
        findViewById(R.id.header_title)
    }

    private val headerAuthStatus: ImageView by lazy {
        findViewById(R.id.header_auth_status)
    }

    private val headerSettings: ImageView by lazy {
        findViewById(R.id.header_settings)
    }

    private val fullScreenSwitch: SwitchCompat by lazy {
        findViewById(R.id.fullScreenSwitch)
    }

    private val fullScreenSwitchContainer: ConstraintLayout by lazy {
        findViewById(R.id.fullScreenSwitch_container)
    }

    private val flowsSection: LinearLayout by lazy {
        findViewById(R.id.ll_flow_section)
    }

    private val componentsSection: LinearLayout by lazy {
        findViewById(R.id.ll_components_section)
    }

    private val apiSection: LinearLayout by lazy {
        findViewById(R.id.ll_api_section)
    }

    private val themeOptionRowViewMap = mutableMapOf<Int, View>()

    companion object {
        private const val API_URL_OVERRIDE = "API_URL_OVERRIDE"
        private const val API_KEY_OVERRIDE = "API_KEY_OVERRIDE"
    }

    private val preferences by lazy {
        getSharedPreferences("LucraSamplePrefs", Context.MODE_PRIVATE)
    }
    private var apiUrlOverride: String?
        get() = preferences.getString(API_URL_OVERRIDE, null).takeIf { !it.isNullOrBlank() }
        set(value) {
            preferences.edit().putString(API_URL_OVERRIDE, value).apply()
        }

    private var apiKeyOverride: String?
        get() = preferences.getString(API_KEY_OVERRIDE, null).takeIf { !it.isNullOrBlank() }
        set(value) {
            preferences.edit().putString(API_KEY_OVERRIDE, value).apply()
        }

    // Managing latest user
    private var lucraSDKUser: SDKUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_sdk)

        initializeLucraClient()

        setupPushNotifications()

        val flow = LucraPushNotificationService.handleNotificationIntent(intent)
        flow?.let { launchFlow(it) }

        setupNotificationChannel()

        consumeSampleDeepLink()

        observeLoggedInUser()

        setupAuthHeaderButton()

        setupAuthSettingsButton()

        header.text = "${BuildConfig.BUILD_TYPE.uppercase()} SDK ${BuildConfig.VERSION_NAME}"

        fullScreenSwitchContainer.setOnClickListener {
            fullScreenSwitch.toggle()
        }

        appendFlowOptions()

        appendApiOptions()

        appendComponentOptions()
    }

    private fun buildApiUrlSet() = BuildConfig.TESTING_API_URL != "ADD YOUR API URL HERE"
    private fun buildApiKeySet() = BuildConfig.TESTING_API_KEY != "ADD YOUR API KEY HERE"

    private fun initializeLucraClient() {
        val apiUrlSet = buildApiUrlSet() || apiUrlOverride != null
        val apiKeySet = buildApiKeySet() || apiKeyOverride != null
        if (!apiUrlSet || !apiKeySet) {
            Log.e("Lucra SDK Sample", "Be sure to use a valid API Url prior to initialization!")
            MaterialAlertDialogBuilder(this)
                .setTitle("Woah, hold up!")
                .setMessage("This sample only works if you've been given a valid API URL and API Key. Press configure to manually enter these values or reach out to your Lucra contact to get started.")
                .setNeutralButton("Configure") { dialog, _ ->
                    overrideApiUrlAndKey()
                }
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            // TODO lucra client does NOT gracefully handle empty or invalid apikey/url
            //  https://lucrasports.atlassian.net/browse/LF-3596
        }

        LucraClient.initialize(
            application = application,
            lucraUiProvider = buildLucraUiInstance(),
            // This must be updated to the correct auth0 client id per environment
            // Logins won't work if there's a mismatch
            apiKey = apiKeyOverride ?: BuildConfig.TESTING_API_KEY,
            // This must be updated to the correct api url per environment
            apiUrl = apiUrlOverride ?: BuildConfig.TESTING_API_URL,
            environment = getEnvironmentFromBuildType(),
            outputLogs = true,
            clientTheme = ClientTheme(
                lightColorStyle = SampleColorStore.getLightColorStyle(),
                darkColorStyle = SampleColorStore.getDarkColorStyle(),
                fontFamily = FontFamily(
                    mediumFont = Font("merriweather_medium.ttf"),
                    normalFont = Font("merriweather_regular.ttf"),
                    semiBoldFont = Font("merriweather_bold.ttf"),
                    boldFont = Font("merriweather_black.ttf"),
                )
            ),
        )

        LucraClient().setEventListener(object : LucraEventListener {
            override fun onEvent(event: LucraEvent) {
                when (event) {
                    is LucraEvent.GamesContest.Created -> {
                        Log.d("Sample", "Games contest created: ${event.contestId}")
                    }

                    is LucraEvent.SportsContest.Created -> {
                        Log.d("Sample", "Sports contest created: ${event.contestId}")
                    }

                    else -> {
                        Log.d("Sample", "Other Event: $event")
                    }
                }

                Toast.makeText(
                    this@MainActivitySdk,
                    "Event has been triggered --> ${event}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        LucraClient().setDeeplinkTransformer {
            generateNavigateLink(it)
        }
    }

    private fun setupPushNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    LucraPushNotificationService.refreshFirebaseToken(token)
                }
        } catch (e: Exception) {
            Log.e(
                "Lucra SDK Sample",
                "Firebase is not setup for this sample project, and this Firebase token example will not work...",
                e
            )
        }
    }

    private fun consumeSampleDeepLink() {
        intent?.let {
            lifecycleScope.launch {
                try {
                    val dynamicLink = Firebase.dynamicLinks.getDynamicLink(intent).await()
                    dynamicLink?.link?.toString()?.let { linkString ->
                        LucraClient().getLucraFlowForDeeplinkUri(linkString)
                            .let { lucraFlow ->
                                if (lucraFlow != null) {
                                    launchFlow(lucraFlow)
                                }
                            }
                        intent?.replaceExtras(Bundle())
                        intent?.data = null
                    }
                } catch (e: Exception) {
                    Log.e(
                        "Lucra SDK Sample",
                        "Firebase is not setup for this sample project, and this deeplink example will not work...",
                        e
                    )
                }
            }
        }
    }

    private fun appendComponentOptions() {
        appendOption(
            "Profile Pill",
            "Show the profile pill with the user balance. Authentication not required, but clicking will launch the auth flow.",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.childCount > 0) {
                viewGroup.removeAllViews()
            } else {
                val view = LucraClient().getLucraComponent(
                    this,
                    LucraUiProvider.LucraComponent.ProfilePill {
                        launchFlow(it)
                    })
                viewGroup.addView(view)
            }
        }

        appendOption(
            "Recommended Matchups Banner",
            "Show the recommended matchups banner component. Authentication required.",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.childCount > 0) {
                viewGroup.removeAllViews()
            } else {
                val view = LucraClient().getLucraComponent(
                    this,
                    LucraUiProvider.LucraComponent.RecommendedMatchups {
                        launchFlow(it)
                    }
                )
                viewGroup.addView(view)
            }
        }

        appendOption(
            "Floating Action Button",
            "Show the floating action button to create a sports contest.",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.childCount > 0) {
                viewGroup.removeAllViews()
            } else {
                val view = LucraClient().getLucraComponent(
                    this,
                    LucraUiProvider.LucraComponent.FloatingActionButton {
                        launchFlow(it)
                    })

                viewGroup.addView(view)
            }
        }

        appendOption(
            "Mini Public Feed",
            "Show the mini public feed, showing contest cards in an non-scroll list. This will prompt for two player Ids but it can accept an array. No authentication required. Any proceeding actions with prompt the user to authenticate first",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->

            if (viewGroup.childCount > 0) {
                viewGroup.removeAllViews()
                return@appendOption
            }

            val builder = MaterialAlertDialogBuilder(this)
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            val playerOneInput = EditText(this).apply {
                hint = "Set player 1 ID (optional)"
                //                setText("aaafe923-e36a-4514-a463-3e620033c416")
            }
            val playerTwoInput = EditText(this).apply {
                hint = "Set player 2 ID (optional)"
                //                setText( "0ae16407-9f7f-484c-9eb3-4f2a641c78bc")
            }

            layout.addView(playerOneInput)
            layout.addView(playerTwoInput)

            builder.setTitle("Add player ids")
                .setView(layout)
                .setPositiveButton("OK") { dialog, id ->
                    val playerOneId = playerOneInput.text.toString()
                    val playerTwoId = playerTwoInput.text.toString()
                    val view = LucraClient().getLucraComponent(
                        this,
                        LucraUiProvider.LucraComponent.MiniPublicFeed(
                            listOf(playerOneId, playerTwoId)
                        ) {
                            launchFlow(it)
                        })
                    viewGroup.addView(view)

                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }

            builder.show()
        }

        appendOption(
            "Contest Card",
            "Show contest card for a specific contest",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.childCount > 0) {
                viewGroup.removeAllViews()
                return@appendOption
            }

            val builder = MaterialAlertDialogBuilder(this)
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            val contestInput = EditText(this).apply {
                hint = "Set contest ID"
                setText("000cfba7-17cd-4702-9d04-ed23c84a89fe")
            }

            layout.addView(contestInput)

            builder.setTitle("Add Contest Id")
                .setView(layout)
                .setPositiveButton("OK") { dialog, id ->
                    val contestId = contestInput.text.toString()
                    val view = LucraClient().getLucraComponent(
                        this,
                        LucraUiProvider.LucraComponent.ContestCard(
                            contestId = contestId,
                        ) {
                            launchFlow(it)
                        }
                    )

                    viewGroup.addView(view)
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()
                }

            builder.show()
        }
    }

    private fun buildLucraUiInstance() = LucraUi(
        lucraFlowListener = object : LucraFlowListener {
            override fun launchNewLucraFlowEntryPoint(entryLucraFlow: LucraUiProvider.LucraFlow): Boolean {
                Log.d("Sample", "launchNewLucraFlowEntryPoint: $entryLucraFlow")
                return if (fullScreenSwitch.isChecked) {
                    showLucraDialogFragment(entryLucraFlow)
                    true
                } else {
                    false
                }
            }

            override fun onFlowDismissRequested(entryLucraFlow: LucraUiProvider.LucraFlow) {
                Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow")
                Log.d("Sample", "fragments: ${supportFragmentManager.fragments}")
                Log.d(
                    "Sample",
                    "backstack count: ${supportFragmentManager.backStackEntryCount}"
                )
                supportFragmentManager.findFragmentByTag(entryLucraFlow.toString())?.let {
                    Log.d("Sample", "Found $entryLucraFlow as $it")

                    if (it is DialogFragment)
                        it.dismiss()
                    else
                        supportFragmentManager.beginTransaction().remove(it).commit()
                } ?: run {
                    Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow not found")
                }
            }
        }
    )

    private fun getEnvironmentFromBuildType(): Environment {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> Environment.DEVELOPMENT
            "staging" -> Environment.STAGING
            "sandbox" -> Environment.SANDBOX
            "release" -> Environment.PRODUCTION
            else -> Environment.STAGING
        }
    }

    // TODO add configure user API
    // TODO add create games you play API
    // TODO add accept games you play API
    // TODO add cancel games you play API
    private fun appendApiOptions() {
        appendOption(
            "Logout",
            "Logout the current user.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            logoutUser()
        }

        appendOption(
            "Update Username",
            "A prompt will show to update the current user's username. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            updateUsernameDialog()
        }

        appendOption(
            "Configure User",
            "A prompt will show to update or preconfigure the SDK User. Authentication not required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            configureUserDialog()
        }

        appendOption(
            "Retrieve Games Matchup",
            "A prompt will show to set the games matchup_id. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            retrieveGamesMatch()
        }

        appendOption(
            "Retrieve Sports Matchup",
            "A prompt will show to set the sports matchup_id. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            retrieveSportsMatch()
        }

        appendOption(
            "Check KYC Status",
            "Verify the KYC status of the current user. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            if (lucraSDKUser?.userId == null) {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Not logged in yet!",
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@appendOption
            }

            LucraClient().checkUsersKYCStatus(
                lucraSDKUser!!.userId!!,
                object : LucraClient.LucraKYCStatusListener {
                    override fun onKYCStatusCheckFailed(exception: Exception) {
                        Toast.makeText(
                            this@MainActivitySdk,
                            "Verified Failed ${exception}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onKYCStatusAvailable(isVerified: Boolean) {
                        Toast.makeText(
                            this@MainActivitySdk,
                            "Is user verified? $isVerified",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                })
        }
    }

    //
    private fun setupAuthSettingsButton() {
        headerSettings.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Settings Options")
                .setPositiveButton("Close", null)
                .setItems(
                    arrayOf<CharSequence>(
                        "Override API URL and key",
                        "Set League Filter",
                        "View Recent IDs of games, leagues and players",
                        "Update Style Colors"
                    )
                ) { dialog, which ->
                    when (which) {
                        0 -> {
                            overrideApiUrlAndKey()
                        }

                        1 -> {
                            val editText =
                                layoutInflater.inflate(
                                    R.layout.main_option_setting_edit_text,
                                    null
                                )
                            val leagueFilter = LucraClient().getPublicFeedLeagueIdFilter()
                            editText.findViewById<TextInputLayout>(R.id.option_setting_edit_text_layout)
                                .setHint("Add a league filter id")
                            if (leagueFilter.currentIdFilters.value.isNotEmpty()) {
                                editText.findViewById<TextView>(R.id.option_setting_edit_text_supporting_text).text =
                                    "Active ids: " + leagueFilter.currentIdFilters.value.joinToString(
                                        ",\n"
                                    )
                            }

                            MaterialAlertDialogBuilder(this)
                                .setTitle("Add League Filter ID")
                                .setView(editText)
                                .setNeutralButton("Clear Filter(s)") { dialog, id ->
                                    leagueFilter.clearIds()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Close", null)
                                .setPositiveButton("Add ID") { dialog, id ->
                                    val id =
                                        editText.findViewById<EditText>(R.id.option_setting_edit_text)
                                            .text.toString()
                                    leagueFilter.addId(id)
                                    dialog.dismiss()
                                }
                                .show()
                        }

                        2 -> {
                            // TODO expose an internal list which contains list of recent
                            //  Games Ids, League Ids, and Player Ids
                            //  It should be a capped list and for testing purposes only
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Recent Game Data")
                                .setMessage("This feature is not yet implemented")
                                .setPositiveButton("Close", null)
                                .show()
                        }

                        3 -> {

                            val colorLayout = layoutInflater.inflate(
                                /* resource = */ R.layout.main_theming_options_layout,
                                /* root = */ null
                            )
                            val themingOptionsSection =
                                colorLayout.findViewById<LinearLayout>(R.id.ll_theming_options_section)
                            val btnThemeDefault: Button =
                                colorLayout.findViewById(R.id.btn_theme_default)
                            val btnThemeDupr: Button =
                                colorLayout.findViewById(R.id.btn_theme_dupr)
                            val btnThemeChaos: Button =
                                colorLayout.findViewById(R.id.btn_theme_chaos)
                            val btnThemePsf: Button =
                                colorLayout.findViewById(R.id.btn_theme_psf)
                            val btnThemeT1: Button =
                                colorLayout.findViewById(R.id.btn_theme_t1)

                            btnThemeDefault.setOnClickListener {
                                SampleColorStore.applyTheme(
                                    SampleColorStore.defaultLightModeTheme,
                                    SampleColorStore.defaultDarkModeTheme
                                )
                                resetThemingOptions(themingOptionsSection)
                            }
                            btnThemeDupr.setOnClickListener {
                                SampleColorStore.applyTheme(
                                    SampleColorStore.duprTheme
                                )
                                resetThemingOptions(themingOptionsSection)
                            }
                            btnThemeChaos.setOnClickListener {
                                SampleColorStore.applyTheme(
                                    SampleColorStore.chaosTheme
                                )
                                resetThemingOptions(themingOptionsSection)
                            }
                            btnThemeT1.setOnClickListener {
                                SampleColorStore.applyTheme(
                                    SampleColorStore.t1Theme
                                )
                                resetThemingOptions(themingOptionsSection)
                            }
                            btnThemePsf.setOnClickListener {
                                SampleColorStore.applyTheme(
                                    SampleColorStore.psfTheme
                                )
                                resetThemingOptions(themingOptionsSection)
                            }

                            appendThemingOptions(themingOptionsSection)
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Lucra Theming")
                                .setView(colorLayout)
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Apply") { themingDialog, _ ->
                                    themingDialog.dismiss()
                                    restartActivity()
                                }
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun overrideApiUrlAndKey() {
        val layout =
            layoutInflater.inflate(
                R.layout.main_option_configure_api,
                null
            )
        val apiUrlEt = layout.findViewById<TextInputEditText>(R.id.et_api_url)
        val apiUrlTil = layout.findViewById<TextInputLayout>(R.id.til_api_url)
        apiUrlOverride?.let {
            apiUrlTil.hint = "API URL (overrode)"
            apiUrlEt.setText(it)
        } ?: run {
            apiUrlEt.setText(
                if (buildApiUrlSet()) {
                    apiUrlTil.hint = "API URL (default)"
                    BuildConfig.TESTING_API_URL
                } else {
                    "Add an API Url!"
                }
            )
        }
        val apiKeyEt = layout.findViewById<TextInputEditText>(R.id.et_api_key)
        val apiKeyTil = layout.findViewById<TextInputLayout>(R.id.til_api_key)
        apiKeyOverride?.let {
            apiKeyTil.hint = "API Key (overrode)"
            apiKeyEt.setText(it)
        } ?: run {
            apiKeyEt.setText(
                if (buildApiKeySet()) {
                    apiKeyTil.hint = "API Key (default)"
                    BuildConfig.TESTING_API_KEY
                } else {
                    "Add an API Key!"
                }
            )
        }

        val configureApiDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Update Api Url ")
            .setView(layout)
            .setNeutralButton("Reset") { dialog, id ->
                apiUrlOverride = null
                apiKeyOverride = null
                dialog.dismiss()
                restartActivity()
            }
            .setNegativeButton("Close", null)
            .setPositiveButton("Save", null)
            .create()

        configureApiDialog.setOnShowListener {

            val positiveButton = configureApiDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {

                val enteredApiUrl = apiUrlEt.text.toString()
                if (enteredApiUrl.isEmpty()) {
                    apiUrlEt.error = "Cannot be blank"
                    return@setOnClickListener
                }

                val enteredApiKey = apiKeyEt.text.toString()
                if (enteredApiKey.isEmpty()) {
                    apiKeyEt.error = "Cannot be blank"
                    return@setOnClickListener
                }

                apiKeyOverride = if (enteredApiKey != BuildConfig.TESTING_API_KEY) {
                    enteredApiKey
                } else {
                    null
                }


                apiUrlOverride = if (enteredApiUrl != BuildConfig.TESTING_API_URL) {
                    enteredApiUrl
                } else {
                    null
                }
                configureApiDialog.dismiss()
                restartActivity()
            }
        }
        configureApiDialog.show()
    }

    private fun setupAuthHeaderButton() {
        headerAuthStatus.setOnClickListener {
            val isUserLoggedIn = lucraSDKUser != null
            MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.lucra_bolt)
                .setTitle("Authentication Status")
                .setMessage(
                    if (!isUserLoggedIn)
                        "User is not logged in"
                    else
                        lucraSDKUser!!.toString()
                )
                .apply {
                    if (isUserLoggedIn) {
                        setPositiveButton("Logout") { dialog, id ->
                            logoutUser()
                            dialog.dismiss()
                        }
                        setNeutralButton(
                            "Copy"
                        ) { _, _ -> // Get a handle to the clipboard service.
                            val clipboard: ClipboardManager =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            // Create a clip with the message text.
                            val clip = ClipData.newPlainText(
                                "Lucra SDK User Info",
                                lucraSDKUser!!.toString()
                            )
                            // Set the clip to the clipboard.
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                applicationContext,
                                "User info copied to clipboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        setPositiveButton("Login") { dialog, id ->
                            launchFlow(LucraUiProvider.LucraFlow.Login)
                            dialog.dismiss()
                        }
                    }
                    setNegativeButton("Configure") { dialog, id ->
                        configureUserDialog()
                        dialog.dismiss()
                    }
                }
                .show()
        }
    }

    private fun logoutUser() {
        LucraClient().logout(this)
        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_LONG).show()
    }

    private fun appendFlowOptions() {
        appendOption(
            "Login",
            "Navigate directly to the login screen. If user is already logged in, this will exit immediately",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.Login)
        }

        appendOption(
            "Profile",
            "Navigate to the current user's profile. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.Profile)
        }

        appendOption(
            "Add Funds",
            "Navigate to the add funds flow. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.AddFunds)
        }

        appendOption(
            "Withdraw Funds",
            "Navigate to the withdraw funds flow. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.WithdrawFunds)
        }

        appendOption(
            "Public Sports Feed",
            "Navigate to the public sports feed. No authentication required. Any proceeding actions with prompt the user to authenticate first",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.PublicFeed)
        }

        appendOption(
            "Create Sports Matchup",
            "Navigate to the create sports match up flow. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.CreateSportsMatchup)
        }

        appendOption(
            "Create Games Matchup",
            "Navigate to the create games match up flow. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.CreateGamesMatchup)
        }

        appendOption(
            "My Matchups",
            "Navigate to the user's created matchups screen. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.MyMatchup)
        }

        appendOption(
            "Deeplink to Matchup Details",
            "Navigate to specific matchup via deeplink uri. Authentication required",
            flowsSection
        ) {
            val builder = MaterialAlertDialogBuilder(this)

            // Create the EditText for the dialog.
            val input = EditText(this).apply {
                hint = "deeplink uri, Ex: lucra://referral/com.tennis..."
            }

            builder.setTitle("Provide Deeplink URI")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    LucraClient().getLucraFlowForDeeplinkUri(input.text.toString())
                        .let { lucraFlow ->

                            if (lucraFlow != null) {
                                launchFlow(lucraFlow)
                            } else {
                                Toast.makeText(
                                    /* context = */ this@MainActivitySdk,
                                    /* text = */ "Invalid URI - could not parse!",
                                    /* duration = */ Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            builder.show()
        }

        appendOption(
            "Verify User Identity",
            "Navigate user identity verification screen. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.VerifyIdentity)
        }
    }

    private fun resetThemingOptions(root: ViewGroup) {
        root.removeAllViews()
        appendThemingOptions(root)
    }

    private fun appendThemingOptions(root: ViewGroup) {
        layoutInflater.inflate(R.layout.theme_color_selector, root, false)
            .apply {
                findViewById<TextView>(R.id.colorDescriptorTv).text = "LightTheme options"
                findViewById<TextView>(R.id.colorHexTv).visibility = View.GONE
                findViewById<View>(R.id.colorPreview).visibility = View.GONE
            }
            .also { root.addView(it) }


        SampleColorStore.getColorIdHexIntForAllLightModeProperties { id, title, colorHex, colorInt ->
            appendThemingOption(
                title = title,
                colorHex = colorHex,
                id = id,
                defaultColor = colorInt,
                root = root
            ).also { themeOptionRowViewMap[id] = it }
        }

        layoutInflater.inflate(R.layout.theme_color_selector, root, false)
            .apply {
                findViewById<TextView>(R.id.colorDescriptorTv).text = "DarkTheme options"
                findViewById<TextView>(R.id.colorHexTv).visibility = View.GONE
                findViewById<View>(R.id.colorPreview).visibility = View.GONE
            }
            .also { root.addView(it) }

        SampleColorStore.getColorIdHexIntForAllDarkModeProperties { id, title, colorHex, colorInt ->
            appendThemingOption(
                title = title,
                colorHex = colorHex,
                id = id,
                defaultColor = colorInt,
                root = root
            ).also { themeOptionRowViewMap[id] = it }
        }
    }

    override fun onColorSelected(
        dialogId: Int,
        color: Int
    ) {
        SampleColorStore.ColorIdMap.updateColorBasedOnId(dialogId, color)

        themeOptionRowViewMap[dialogId]?.run {
            findViewById<TextView>(R.id.colorHexTv).text = color.intToColorHex()
            findViewById<View>(R.id.colorPreview).setBackgroundColor(color)
        }
    }

    override fun onDialogDismissed(dialogId: Int) { /* no-op */
    }

    private fun appendOption(
        title: String,
        description: String,
        root: ViewGroup,
        drawable: Drawable? = null,
        onClick: (ViewGroup) -> Unit
    ) {
        val optionView = layoutInflater.inflate(R.layout.main_option_click, root, false)
        optionView.findViewById<TextView>(R.id.option_title).text = title
        drawable?.let {
            optionView.findViewById<ImageView>(R.id.option_icon).setImageDrawable(it)
        }
        optionView.findViewById<TextView>(R.id.option_description).text = description

        optionView.setOnClickListener {
            onClick(optionView.findViewById(R.id.option_view_container))
        }
        root.addView(optionView)
    }

    private fun appendThemingOption(
        title: String,
        colorHex: String,
        id: Int,
        defaultColor: Int,
        root: ViewGroup,
    ): View {
        val optionView =
            layoutInflater.inflate(R.layout.theme_color_selector, root, false).apply {
                findViewById<TextView>(R.id.colorDescriptorTv).text = title
                findViewById<TextView>(R.id.colorHexTv).text = colorHex
                findViewById<View>(R.id.colorPreview).setBackgroundColor(defaultColor)
                setOnClickListener {
                    showColorPickerDialog(
                        id = id,
                        defaultColor = defaultColor
                    )
                }
            }

        root.addView(optionView)
        return optionView
    }

    private fun showColorPickerDialog(
        id: Int,
        defaultColor: Int
    ) {
        ColorPickerDialog.newBuilder()
            .setDialogId(id) // set id here
            .setColor(defaultColor)
            .show(this)
    }

    private fun observeLoggedInUser() {
        // Or use observeSDKUser { result -> ... }
        LucraClient().observeSDKUserFlow().onEach { sdkUserResult ->
            when (sdkUserResult) {
                is SDKUserResult.Success -> {
                    Log.d("Lucra SDK Sample", "Fetched latest user")
                    lucraSDKUser = sdkUserResult.sdkUser
                    headerAuthStatus.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_logged_in
                        )
                    )
                }

                is SDKUserResult.Error -> {
                    Log.e("Lucra SDK Sample", "Unable to get username ${sdkUserResult.error}")
                    lucraSDKUser = null
                    headerAuthStatus.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_logged_out
                        )
                    )
                }

                SDKUserResult.InvalidUsername -> {
                    // Shouldn't happen here
                }

                SDKUserResult.NotLoggedIn, SDKUserResult.WaitingForLogin -> {
                    Log.e(
                        "Lucra SDK Sample",
                        "User not logged in yet!"
                    )
                    lucraSDKUser = null
                    headerAuthStatus.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_logged_out
                        )
                    )
                }

                SDKUserResult.Loading -> {

                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun retrieveSportsMatch() {
        val builder = MaterialAlertDialogBuilder(this)
        val input = EditText(this).apply {
            setText("123db0c6-3096-443a-ad63-8cc302bddf92")
        }
        builder.setTitle("Set Matchup Id")
            .setView(input)
            .setPositiveButton("OK") { dialog, id ->
                val matchUpId = input.text.toString()
                LucraClient().getSportsMatchup(matchUpId) {
                    val builderDisplay = MaterialAlertDialogBuilder(this)
                    if (it is SportsMatchup.RetrieveSportsMatchupResult.SportsMatchupDetailsOutput) {
                        var displayString = ""
                        displayString += "${it.sportsMatchup}"

                        val textView = TextView(this).apply {
                            setText(displayString)
                            setPadding(50, 50, 50, 50)
                        }

                        builderDisplay.setTitle("Matchup Results")
                            .setView(textView)
                            .setPositiveButton("OK") { dialog, id ->
                                dialog.dismiss()
                            }.show()

                    } else if (it is SportsMatchup.RetrieveSportsMatchupResult.Failure) {
                        builderDisplay.setTitle("Failed to find matchup")
                            .setMessage(it.failure.toString())
                            .setPositiveButton("OK") { dialog, id ->
                                dialog.dismiss()
                            }.show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        builder.show()
    }


    private fun retrieveGamesMatch() {
        val builder = MaterialAlertDialogBuilder(this)
        val input = EditText(this).apply {
            setText("7e547d90-5d54-4446-8443-26ad57e838c6")
        }
        builder.setTitle("Set Matchup Id")
            .setView(input)
            .setPositiveButton("OK") { dialog, id ->
                val matchUpId = input.text.toString()
                LucraClient().getGamesMatchup(matchUpId) {
                    val builderDisplay = MaterialAlertDialogBuilder(this)
                    if (it is GamesMatchup.RetrieveGamesMatchupResult.GYPMatchupDetailsOutput) {
                        var displayString = ""
                        displayString += "Amount > ${it.wagerAmount}\n" +
                                "Status > ${it.status}\n" +
                                "Created At > ${it.createdAt}\n" +
                                "Updated At > ${it.updatedAt}\n" +
                                "Owner Id > ${it.ownerId}\n" +
                                "Game Type > ${it.gameType}\n\n"

                        displayString += "===Teams===\n"

                        it.teams.forEach { gameMatchupTeam ->
                            displayString += "Team Id: ${gameMatchupTeam.id}\n"
                            displayString += "Number of Users: ${gameMatchupTeam.users.size}\n"
                            gameMatchupTeam.users.forEach { matchupTeamUser ->
                                displayString += "--User Id: ${matchupTeamUser.id}\n"
                                displayString += "--Username: ${matchupTeamUser.username}\n"
                            }

                            displayString += "\n"
                        }
                        displayString += "\n"

                        val textView = TextView(this).apply {
                            setText(displayString)
                            setPadding(50, 50, 50, 50)
                        }

                        builderDisplay.setTitle("Matchup Results")
                            .setView(textView)
                            .setPositiveButton("OK") { dialog, id ->
                                dialog.dismiss()
                            }.show()

                    } else if (it is GamesMatchup.RetrieveGamesMatchupResult.Failure) {
                        builderDisplay.setTitle("Failed to find matchup")
                            .setMessage(it.failure.toString())
                            .setPositiveButton("OK") { dialog, id ->
                                dialog.dismiss()
                            }.show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        builder.show()
    }

    private var userSelectedBirthdate: Calendar? = null

    private fun configureUserDialog() {

        val builder = MaterialAlertDialogBuilder(this)

        // Create the EditText for the dialog.
        val userForm =
            layoutInflater.inflate(R.layout.main_configure_user_options, null).apply {
                findViewById<TextInputEditText>(R.id.username).setText(lucraSDKUser?.username.orEmpty())
                findViewById<TextInputEditText>(R.id.email).setText(lucraSDKUser?.email.orEmpty())
                findViewById<TextInputEditText>(R.id.avatar).setText(lucraSDKUser?.avatarUrl.orEmpty())
                findViewById<TextInputEditText>(R.id.phone).setText(lucraSDKUser?.phoneNumber.orEmpty())
                findViewById<TextInputEditText>(R.id.firstName).setText(lucraSDKUser?.firstName.orEmpty())
                findViewById<TextInputEditText>(R.id.lastName).setText(lucraSDKUser?.lastName.orEmpty())
                findViewById<TextInputEditText>(R.id.address).setText(lucraSDKUser?.address.orEmpty())
                findViewById<TextInputEditText>(R.id.addresCont).setText(lucraSDKUser?.addressCont.orEmpty())
                findViewById<TextInputEditText>(R.id.city).setText(lucraSDKUser?.city.orEmpty())
                findViewById<TextInputEditText>(R.id.state).setText(lucraSDKUser?.state.orEmpty())
                findViewById<TextInputEditText>(R.id.zip).setText(lucraSDKUser?.zip.orEmpty())
                findViewById<MaterialButton>(R.id.btn_birthday).apply {
                    setText(
                        lucraSDKUser?.birthday?.toMonthDayYear() ?: "Birthday (empty)"
                    )
                    setOnClickListener {
                        val openAt = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            add(Calendar.YEAR, -21)
                        }.timeInMillis

                        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select a Date")
                            .setCalendarConstraints(
                                CalendarConstraints.Builder()
                                    .setOpenAt(
                                        userSelectedBirthdate?.timeInMillis
                                            ?: lucraSDKUser?.birthday?.timeInMillis
                                            ?: openAt
                                    )
                                    .setValidator(DateValidatorPointBackward.now())
                                    .build()
                            )
                        val datePicker = datePickerBuilder.build()
                        datePicker.show(supportFragmentManager, "DATE_PICKER")
                        datePicker.addOnPositiveButtonClickListener { selectedTime ->
                            userSelectedBirthdate =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = selectedTime
                                }
                            setText(
                                userSelectedBirthdate?.toMonthDayYear() ?: "(empty)"
                            )
                        }
                    }
                }
            }


        builder.setTitle("Configure the user")
            .setView(userForm)
            .setPositiveButton("Configure") { dialog, id ->
                val newSdkUser = SDKUser(
                    username = userForm.findViewById<TextInputEditText>(R.id.username).getText()
                        .toString().takeIf { it.isNotBlank() },
                    email = userForm.findViewById<TextInputEditText>(R.id.email).getText()
                        .toString().takeIf { it.isNotBlank() },
                    avatarUrl = userForm.findViewById<TextInputEditText>(R.id.avatar).getText()
                        .toString().takeIf { it.isNotBlank() },
                    phoneNumber = userForm.findViewById<TextInputEditText>(R.id.phone).getText()
                        .toString().takeIf { it.isNotBlank() },
                    firstName = userForm.findViewById<TextInputEditText>(R.id.firstName)
                        .getText()
                        .toString().takeIf { it.isNotBlank() },
                    lastName = userForm.findViewById<TextInputEditText>(R.id.lastName).getText()
                        .toString().takeIf { it.isNotBlank() },
                    address = userForm.findViewById<TextInputEditText>(R.id.address).getText()
                        .toString().takeIf { it.isNotBlank() },
                    addressCont = userForm.findViewById<TextInputEditText>(R.id.addresCont)
                        .getText().toString().takeIf { it.isNotBlank() },
                    city = userForm.findViewById<TextInputEditText>(R.id.city).getText()
                        .toString().takeIf { it.isNotBlank() },
                    state = userForm.findViewById<TextInputEditText>(R.id.state).getText()
                        .toString().takeIf { it.isNotBlank() },
                    zip = userForm.findViewById<TextInputEditText>(R.id.zip).getText()
                        .toString().takeIf { it.isNotBlank() },
                    birthday = userSelectedBirthdate
                )

                // set details here so information is not lost
                lucraSDKUser = newSdkUser
                LucraClient().configure(
                    newSdkUser
                ) { result ->
                    when (result) {
                        is SDKUserResult.Error -> {
                            Log.e(
                                "Lucra SDK Sample",
                                "Unable to configure user ${result.error}"
                            )
                            Toast.makeText(
                                this@MainActivitySdk,
                                "Error!: " + result.error.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        SDKUserResult.InvalidUsername -> {

                            Toast.makeText(
                                this@MainActivitySdk,
                                "Invalid Username!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        SDKUserResult.Loading -> {
                            // no loading here...
                            dialog.dismiss()
                        }

                        SDKUserResult.NotLoggedIn -> {
                            // shouldn't happen here!
                        }

                        is SDKUserResult.Success -> {
                            dialog.dismiss()
                            Toast.makeText(
                                this@MainActivitySdk,
                                "User configured!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }

                        SDKUserResult.WaitingForLogin -> {
                            dialog.dismiss()

                            Toast.makeText(
                                this@MainActivitySdk,
                                "Waiting for login prior to configuration",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        builder.show()
    }

    private fun updateUsernameDialog() {

        if (lucraSDKUser == null) {
            Toast.makeText(
                this@MainActivitySdk,
                "Not logged in yet!",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        val builder = MaterialAlertDialogBuilder(this)

        // Create the EditText for the dialog.
        val input = EditText(this).apply {
            if (lucraSDKUser?.username.isNullOrEmpty()) {
                hint = "No username set yet"
            } else {
                hint = "Current username: ${lucraSDKUser?.username}"
                setText(lucraSDKUser?.username!!)
            }

        }

        builder.setTitle("Update Username")
            .setView(input)
            .setPositiveButton("OK") { dialog, id ->
                val newUsername = input.text.toString()
                if (newUsername.isEmpty()) {
                    Toast.makeText(
                        this@MainActivitySdk,
                        "Username cannot be empty",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@setPositiveButton
                } else {

                    // User configure to update the username!
                    val newSdkUser = lucraSDKUser!!.copy(username = newUsername)
                    LucraClient().configure(newSdkUser) {
                        when (it) {
                            is SDKUserResult.Error -> {
                                Log.e(
                                    "Lucra SDK Sample",
                                    "Unable to update username ${it.error}"
                                )
                                Toast.makeText(
                                    this@MainActivitySdk,
                                    "Unable to update username",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                            SDKUserResult.InvalidUsername -> {
                                Toast.makeText(
                                    this@MainActivitySdk,
                                    "Invalid username, try a different one",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                            SDKUserResult.NotLoggedIn -> {
                                Log.e(
                                    "Lucra SDK Sample",
                                    "User not logged in yet! Close this dialog and try again"
                                )
                                Toast.makeText(
                                    this@MainActivitySdk,
                                    "User not logged in!",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                            is SDKUserResult.Success -> {
                                Toast.makeText(
                                    this@MainActivitySdk,
                                    "Username updated to ${it.sdkUser.username}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                lucraSDKUser = it.sdkUser
                                dialog.dismiss()
                            }

                            SDKUserResult.Loading -> {
                                // Show loading affordance
                            }

                            SDKUserResult.WaitingForLogin -> {
                                // not used in this scenario
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        builder.show()
    }

    /**
     * We need to call this to restart the activity and reinitialize the sdk with new values.
     */
    private fun restartActivity() {
        val intent = Intent(this, this::class.java)

        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        LucraClient.release()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showLucraDialogFragment(lucraFlow: LucraUiProvider.LucraFlow) {
        LucraClient().getLucraDialogFragment(lucraFlow).also {
            it.show(supportFragmentManager, lucraFlow.toString())
        }
    }

    private fun launchFlow(lucraFlow: LucraUiProvider.LucraFlow) {
//        showLucraFragment(lucraFlow)
        showLucraDialogFragment(lucraFlow)
    }

    override fun onDestroy() {
        super.onDestroy()
        // NOTE: Don't release on rotation with a dialog fragment open, this will break the instance
        // e.g. DialogFragment will recover before a flow has been set again.
//        LucraClient.release()
    }

    suspend fun generateNavigateLink(url: String): String {
        val link = try {
            val dynamicLinksDomainURIPrefix = BuildConfig.FIREBASE_DEEPLINK_URL
            val builder = FirebaseDynamicLinks.getInstance().createDynamicLink()
            builder.link = Uri.parse(url)
            builder.domainUriPrefix = dynamicLinksDomainURIPrefix
            builder.androidParameters(packageName) {

            }

            val iosBundleSuffix = if (BuildConfig.BUILD_TYPE == "debug") {
                "-dev"
            } else if (BuildConfig.BUILD_TYPE == "staging" || BuildConfig.BUILD_TYPE == "sandbox") {
                "-stg"
            } else {
                ""
            }

            val iosBundle = "com.lucrasports.mobile-sample${iosBundleSuffix}"
            builder.iosParameters(iosBundle) {

            }

            builder.navigationInfoParameters {
                forcedRedirectEnabled = false
            }

            val dynamicLinkUri = builder.buildDynamicLink().uri

            try {
                val result = builder.buildShortDynamicLink().await()
                val shortLink = result.shortLink
                shortLink?.toString() ?: dynamicLinkUri.toString()
            } catch (e: Exception) {
                dynamicLinkUri.toString()
            }
        } catch (e: Exception) {
            ""
        }

        return link
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val flow = LucraPushNotificationService.handleNotificationIntent(intent)
        flow?.let { launchFlow(it) }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FCMService.DEFAULT_NOTIFICATION_CHANNEL_ID,
                FCMService.DEFAULT_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun Calendar.toMonthDayYear(): String =
        "${get(Calendar.MONTH) + 1}/${get(Calendar.DAY_OF_MONTH)}/${get(Calendar.YEAR)}"

}