package com.lucrasports.sdk.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.lucrasports.feature.reward_selection_flow.components.RedeemRewardDialogFragment
import com.lucrasports.feature.reward_selection_flow.components.ViewMyRewardsDialogFragment
import com.lucrasports.feature.reward_selection_flow.components.ViewMyRewardsDialogFragment.ViewMyRewardsListener
import com.lucrasports.sdk.app.catalog.AuthRequirement
import com.lucrasports.sdk.app.catalog.CatalogPreferences
import com.lucrasports.sdk.app.catalog.CatalogUiState
import com.lucrasports.sdk.app.catalog.MAX_RECENTS
import com.lucrasports.sdk.app.catalog.SampleActions
import com.lucrasports.sdk.app.catalog.SampleCategory
import com.lucrasports.sdk.app.catalog.SampleEntry
import com.lucrasports.sdk.app.catalog.SampleSurface
import com.lucrasports.sdk.app.catalog.buildListItems
import com.lucrasports.sdk.app.catalog.sampleCatalog
import com.lucrasports.sdk.app.catalog.surfaceCounts
import com.lucrasports.sdk.app.fake_resources.fakeLucraRewards
import com.lucrasports.sdk.app.headless_api.HandshakeAuthApiHandler
import com.lucrasports.sdk.app.headless_api.MatchupApiHandler
import com.lucrasports.sdk.app.headless_api.PhoneAuthApiHandler
import com.lucrasports.sdk.app.headless_api.TournamentApiHandler
import com.lucrasports.sdk.app.headless_api.UserApiHandler
import com.lucrasports.sdk.app.logger.FirebaseLogger
import com.lucrasports.sdk.app.ui.LucraFlowPresenter
import com.lucrasports.sdk.app.ui.SampleCatalogAdapter
import com.lucrasports.sdk.app.ui.SampleSettingsSheet
import com.lucrasports.sdk.app.ui.SettingsRow
import com.lucrasports.sdk.app.ui.dialogs.ComponentDialogs
import com.lucrasports.sdk.app.ui.dialogs.ConfigDialogs
import com.lucrasports.sdk.app.ui.dialogs.DebugDialogs
import com.lucrasports.sdk.app.ui.dialogs.FlowDialogs
import com.lucrasports.sdk.app.ui.dialogs.MiniGameDialogs
import com.lucrasports.sdk.app.ui.dialogs.RecentIdStore
import com.lucrasports.sdk.app.ui.dialogs.RecreationalGameDialogs
import com.lucrasports.sdk.app.ui.dialogs.TournamentDialogs
import com.lucrasports.sdk.app.ui.dialogs.UserDialogs
import com.lucrasports.sdk.app.ui.theming.SampleColorStore
import com.lucrasports.sdk.app.ui.theming.ThemeManager
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.LucraClient.Companion.Environment
import com.lucrasports.sdk.core.convert_credit.LucraConvertToCreditProvider
import com.lucrasports.sdk.core.convert_credit.LucraConvertToCreditWithdrawMethod
import com.lucrasports.sdk.core.convert_credit.LucraWithdrawCardTheme
import com.lucrasports.sdk.core.events.LucraEvent
import com.lucrasports.sdk.core.events.LucraEventListener
import com.lucrasports.sdk.core.reward.LucraReward
import com.lucrasports.sdk.core.reward.LucraRewardProvider
import com.lucrasports.sdk.core.reward.toReward
import com.lucrasports.sdk.core.style_guide.ClientTheme
import com.lucrasports.sdk.core.style_guide.Font
import com.lucrasports.sdk.core.style_guide.FontFamily
import com.lucrasports.sdk.core.style_guide.ThemeMode
import com.lucrasports.sdk.core.ui.LucraFlowListener
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult
import com.lucrasports.sdk.ui.LucraUi
import com.lucrasports.sdk.ui.push_notifications.DeeplinkConstants.NOTIFICATION_DEEPLINK
import com.lucrasports.sdk.ui.push_notifications.LucraPushNotificationService
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val AUTO_JOIN_ENABLED = "AUTO_JOIN_ENABLED"
private const val LAUNCH_FULL_SCREEN = "LAUNCH_FULL_SCREEN"
private const val STATE_QUERY = "STATE_QUERY"
private const val SUPPRESS_REWARD_SHEET = "SUPPRESS_REWARD_SHEET"
private const val THEME_MODE_OVERRIDE = "THEME_MODE_OVERRIDE"
private const val TAG_REDEEM_DIALOG = "TAG_REDEEM_DIALOG"
private const val TAG_REWARD_SHEET = "TAG_REWARD_SHEET"
private const val TAG_VIEW_REWARDS = "TAG_VIEW_REWARDS_DIALOG"
private const val TOURNAMENT_DETAIL_PATH_SEGMENT = "TournamentDetailView"
private const val REWARDS_PATH_SEGMENT = "Rewards"

private fun nextThemeMode(current: ThemeMode?): ThemeMode? = when (current) {
    null -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.AUTO
    ThemeMode.AUTO -> null
}

internal class MainActivitySdk : AppCompatActivity(), ColorPickerDialogListener, SampleActions {

    private val rootContainer: LinearLayout by lazy { findViewById(R.id.root_container) }

    private val headerBar: View by lazy { findViewById(R.id.header_bar) }

    private val headerAuthStatus: Chip by lazy { findViewById(R.id.header_auth_status) }

    private val headerSettings: ImageView by lazy { findViewById(R.id.header_settings) }

    private val catalogList: RecyclerView by lazy { findViewById(R.id.catalog_list) }

    private val searchField: TextInputEditText by lazy { findViewById(R.id.catalog_search) }

    private val surfaceBlurb: TextView by lazy { findViewById(R.id.surface_blurb) }

    private val chipEnvironment: Chip by lazy { findViewById(R.id.chip_environment) }
    private val chipVersion: Chip by lazy { findViewById(R.id.chip_version) }
    private val chipApiKey: Chip by lazy { findViewById(R.id.chip_api_key) }
    private val chipSurfaceAll: Chip by lazy { findViewById(R.id.chip_surface_all) }
    private val chipSurfaceFlows: Chip by lazy { findViewById(R.id.chip_surface_flows) }
    private val chipSurfaceApis: Chip by lazy { findViewById(R.id.chip_surface_apis) }
    private val chipSurfaceUi: Chip by lazy { findViewById(R.id.chip_surface_ui) }

    // Helper classes
    private lateinit var recreationalGameDialogs: RecreationalGameDialogs
    private lateinit var tournamentDialogs: TournamentDialogs
    private lateinit var userDialogs: UserDialogs
    private lateinit var configDialogs: ConfigDialogs
    private lateinit var flowDialogs: FlowDialogs
    private lateinit var componentDialogs: ComponentDialogs
    private lateinit var miniGameDialogs: MiniGameDialogs
    private lateinit var matchupApiHandler: MatchupApiHandler
    private lateinit var tournamentApiHandler: TournamentApiHandler
    private lateinit var phoneAuthApiHandler: PhoneAuthApiHandler
    private lateinit var handshakeAuthApiHandler: HandshakeAuthApiHandler
    private lateinit var userApiHandler: UserApiHandler
    private lateinit var themeManager: ThemeManager
    private lateinit var debugDialogs: DebugDialogs
    private val settingsSheet by lazy { SampleSettingsSheet(this) }
    private val recentIdStore by lazy { RecentIdStore(this) }

    private val preferences by lazy {
        getSharedPreferences(SAMPLE_PREFS, MODE_PRIVATE)
    }

    private var apiKeyOverride: String?
        get() = preferences.getString(API_KEY_OVERRIDE, null).takeIf { !it.isNullOrBlank() }
        set(value) {
            preferences.edit { putString(API_KEY_OVERRIDE, value) }
        }

    private var autoJoinEnabled: Boolean
        get() = preferences.getBoolean(AUTO_JOIN_ENABLED, false)
        set(value) {
            preferences.edit { putBoolean(AUTO_JOIN_ENABLED, value) }
        }

    private var suppressRewardSheet: Boolean
        get() = preferences.getBoolean(SUPPRESS_REWARD_SHEET, false)
        set(value) {
            preferences.edit { putBoolean(SUPPRESS_REWARD_SHEET, value) }
        }

    private var themeModeOverride: ThemeMode?
        get() = preferences.getString(THEME_MODE_OVERRIDE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
        set(value) {
            preferences.edit { putString(THEME_MODE_OVERRIDE, value?.name) }
        }

    private val catalog by lazy {
        sampleCatalog(includeDebugOnly = BuildConfig.BUILD_TYPE != "release")
    }
    private val catalogById by lazy { catalog.associateBy(SampleEntry::id) }
    private val catalogPreferences by lazy { CatalogPreferences(preferences) }
    private lateinit var catalogAdapter: SampleCatalogAdapter
    private var uiState = CatalogUiState()

    /**
     * Live SDK component views for expanded rows, held here rather than in a ViewHolder so a
     * recycled holder can never drop one or bind it under the wrong row.
     */
    private val hostedComponents = mutableMapOf<String, View>()

    private var launchFullScreen: Boolean
        get() = preferences.getBoolean(LAUNCH_FULL_SCREEN, true)
        set(value) {
            preferences.edit { putBoolean(LAUNCH_FULL_SCREEN, value) }
        }

    private var lucraRewardProviderEnabled = true
    private var provideLocationIdOnInit = false

    // Managing latest user
    private var lucraSDKUser: SDKUser? = null

    private lateinit var customLogger: FirebaseLogger

    private lateinit var lucraUi: LucraUi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_sdk)

        // Initialize helper classes
        recreationalGameDialogs = RecreationalGameDialogs(this)
        tournamentDialogs = TournamentDialogs(this)
        userDialogs = UserDialogs(this)
        configDialogs = ConfigDialogs(this)
        flowDialogs = FlowDialogs(this)
        componentDialogs = ComponentDialogs(this)
        miniGameDialogs = MiniGameDialogs(this)
        matchupApiHandler = MatchupApiHandler(this)
        tournamentApiHandler = TournamentApiHandler(this)
        phoneAuthApiHandler = PhoneAuthApiHandler(this)
        handshakeAuthApiHandler = HandshakeAuthApiHandler(this)
        userApiHandler = UserApiHandler(this)
        themeManager = ThemeManager(this)
        debugDialogs = DebugDialogs(this)

        val headerBase = Rect(
            headerBar.paddingLeft,
            headerBar.paddingTop,
            headerBar.paddingRight,
            headerBar.paddingBottom,
        )
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Horizontal insets once on the root, so every row shares one gutter under a cutout.
            rootContainer.updatePadding(left = bars.left, right = bars.right)
            headerBar.setPadding(
                headerBase.left,
                headerBase.top + bars.top,
                headerBase.right,
                headerBase.bottom,
            )
            catalogList.updatePadding(bottom = bars.bottom)
            insets
        }

        initializeLucraClient()

        // After initialize, like a real integration registering from app start — so an
        // organic flow entry exercises the handshake with no menu visit.
        handshakeAuthApiHandler.autoRegisterIfEnabled()

        setupPushNotifications()

        handleNotificationDeeplink(intent)

        observeLoggedInUser()

        setupAuthHeaderButton()

        setupAuthSettingsButton()

        setupHeaderChips()

        setupCatalog(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_QUERY, uiState.query)
    }

    private fun buildApiKeySet() = isSampleApiKeyConfigured(this)

    private fun initializeLucraClient() {
        val apiKeySet = buildApiKeySet()
        if (!apiKeySet) {
            Log.e("Lucra SDK Sample", "Did you forget to set your API key?")
            MaterialAlertDialogBuilder(this)
                .setTitle("Woah, hold up!")
                .setMessage("This sample only works if you've been given a API Key. Press configure to manually enter these values or reach out to your Lucra contact to get started.")
                .setNeutralButton("Configure") { dialog, _ ->
                    overrideApiUrlAndKey()
                }
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        customLogger = FirebaseLogger(applicationContext)
        LucraClient.initialize(
            application = application,
            lucraUiProvider = buildLucraUiInstance().also { lucraUi = it },
            apiKey = apiKeyOverride ?: BuildConfig.TESTING_API_KEY,
            environment = getEnvironmentFromBuildType(),
            outputLogs = true,
            customLogger = customLogger,
            autoJoin = autoJoinEnabled,
            allowRewardSheetToDisplay = !suppressRewardSheet,
            clientTheme = ClientTheme(
                lightColorStyle = SampleColorStore.getLightColorStyle(),
                darkColorStyle = SampleColorStore.getDarkColorStyle(),
                themeMode = themeModeOverride,
                fontFamily = FontFamily(
                    mediumFont = Font("bauziet_norm_medium.otf"),
                    normalFont = Font("bauziet_norm_regular.otf"),
                    semiBoldFont = Font("bauziet_norm_bold.otf"),
                    boldFont = Font("bauziet_norm_extra_bold.otf"),
                )
            ),
        )

        LucraClient().setEventListener(object : LucraEventListener {
            override fun onEvent(event: LucraEvent) {
                when (event) {
                    // Tournaments events
                    is LucraEvent.Tournament.Joined -> {
                        Log.d("Sample", "Tournament joined: ${event.tournamentId}")
                    }

                    // Games events
                    is LucraEvent.GamesContest.Created -> {
                        Log.d("Sample", "Games contest created: ${event.contestId}")
                    }

                    is LucraEvent.GamesContest.Accepted -> {
                        Log.d("Sample", "Games contest accepted: ${event.contestId}")
                    }

                    is LucraEvent.GamesContest.Canceled ->
                        Log.d("Sample", "Games contest canceled: ${event.matchupId}")

                    is LucraEvent.GamesContest.Started ->
                        Log.d("Sample", "Games contest started: ${event.matchupId}")


                    // Sports events
                    is LucraEvent.SportsContest.Created -> {
                        Log.d("Sample", "Sports contest created: ${event.contestId}")
                    }

                    is LucraEvent.SportsContest.Accepted ->
                        Log.d("Sample", "Sports contest accepted: ${event.contestId}")

                    is LucraEvent.SportsContest.Canceled ->
                        Log.d("Sample", "Sports contest canceled: ${event.matchupId}")

                    is LucraEvent.GamesContest.StartedActive ->
                        Log.d(
                            "Sample",
                            "Active game contest started: ${event.matchupId} match object: ${event.lucraMatchup}"
                        )

                    is LucraEvent.Tournament.AutoJoinedTournaments ->
                        Log.d("Sample", "Auto joined tournaments: ${event.tournamentIds}")

                    is LucraEvent.MiniGame.Finished -> {
                        Log.d(
                            "Sample",
                            "Mini game finished: gameId=${event.gameId} mode=${event.gameMode} amount=${event.amount} matchupId=${event.matchupId}"
                        )
                    }
                }
            }
        })

        setupRewardProvider(if (lucraRewardProviderEnabled) fakeLucraRewards else null)

        if (provideLocationIdOnInit) {
            LucraClient()
                .setLocationId("f2708938-2517-46fb-a639-229f4d2ca6c7") // locationId for navy_pier
        }

        LucraClient().setConvertToCreditProvider(object : LucraConvertToCreditProvider {
            override suspend fun getCreditAmount(cashAmount: Double): LucraConvertToCreditWithdrawMethod {
                val convertedAmount = cashAmount + 10.0
                delay(2000L)
                return LucraConvertToCreditWithdrawMethod(
                    id = UUID.randomUUID().toString(),
                    conversionTerms = "No Fee  |  Instant transfer",
                    title = "Gift card",
                    amount = cashAmount,
                    convertedAmount = cashAmount + 10.0,
                    convertedAmountDisplay = "$convertedAmount credits",
                    shortDescription = "This is a short description",
                    longDescription = "This is a long description. This is a long description. This is a long description",
                    metaData = mapOf("testingKey" to "testingValue"),
                    theme = LucraWithdrawCardTheme(
                        cardColor = "#5A1668",
                        cardTextColor = "#FFFFFF",
                        pillColor = "#5A1668",
                        pillTextColor = "#FFFFFF",
                    )
                )
            }
        })


        LucraClient().setDeeplinkTransformer { url ->
            val link = try {
                val branchLinkProperties = LinkProperties()
                val branchUniversalObject = BranchUniversalObject()
                    .setCanonicalUrl(url)
                val shortLink = branchUniversalObject.getShortUrl(this, branchLinkProperties)
                shortLink
            } catch (e: Exception) {
                ""
            }

            link
        }

        LucraClient().setMatchupInviteDeeplinkProvider { matchupId ->
            try {
                val buo = BranchUniversalObject().setContentMetadata(
                    ContentMetadata().addCustomMetadata(
                        "matchupId",
                        matchupId
                    )
                )
                val linkProps = LinkProperties()
                buo.getShortUrl(this, linkProps)
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun setupRewardProvider(newRewards: List<LucraReward>? = fakeLucraRewards) {
        if (newRewards != null) {
            LucraClient().setRewardProvider(object : LucraRewardProvider {
                override suspend fun availableRewards(): List<LucraReward> {
                    // Emulating delay...
                    delay((100..500).random().toLong())
                    return newRewards
                }

                override fun claimReward(reward: LucraReward) {
                    // The idea is to "show" or "reveal" details of the client based Reward
                    // In this case, we're simply dismissing the entire stack of Lucra Flows
                    // NOTE: This will not work for all setups, it's important to keep track of all
                    // instances of LucraFlows, whether they are DialogFragments or a specific Fragment.
                    // You don't want to remove/dismiss fragments/dialogs that aren't related to Contest creation
                    supportFragmentManager.fragments.filterIsInstance<DialogFragment>().forEach {
                        it.dismiss()
                    }
                    if (supportFragmentManager.findFragmentByTag(TAG_REDEEM_DIALOG) == null)
                        RedeemRewardDialogFragment.newInstance(reward.toReward())
                            .show(supportFragmentManager, TAG_REDEEM_DIALOG)
                }

                override fun viewRewards() {
                    // Again, the idea here is to show the list of available rewards for the current user
                    // This is just a dummy example

                    supportFragmentManager.fragments.filterIsInstance<DialogFragment>().forEach {
                        it.dismiss()
                    }
                    if (supportFragmentManager.findFragmentByTag(TAG_VIEW_REWARDS) == null)
                        ViewMyRewardsDialogFragment.newInstance(object : ViewMyRewardsListener {
                            override fun navigateToCreateSYW() {
                                launchFlow(LucraUiProvider.LucraFlow.CreateSportsMatchup)
                            }

                            override fun navigateToCreateGYP() {
                                launchFlow(LucraUiProvider.LucraFlow.CreateGamesMatchup())
                            }

                        }).show(supportFragmentManager, TAG_VIEW_REWARDS)
                }
            })
        } else {
            LucraClient().setRewardProvider(null)
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

    override fun onStart() {
        super.onStart()

        Branch.sessionBuilder(this)
            .withCallback { buo, lp, error ->
                if (error != null) {
                    Log.e("Branch", "init failed: ${error.message}")
                    return@withCallback
                }

                val matchupId = buo?.contentMetadata?.customMetadata?.get("matchupId")
                if (!matchupId.isNullOrBlank()) {
                    launchFlow(LucraUiProvider.LucraFlow.MatchupDetails(matchupId))
                } else {
                    Log.w("Branch", "No matchupId found in deeplink")
                }
            }
            .withData(intent?.data)
            .init()
    }

    private fun buildLucraUiInstance() = LucraUi(
        lucraFlowListener = object : LucraFlowListener {
            override fun launchNewLucraFlowEntryPoint(entryLucraFlow: LucraUiProvider.LucraFlow): Boolean {
                Log.d("Sample", "launchNewLucraFlowEntryPoint: $entryLucraFlow")
                return if (launchFullScreen) {
                    showLucraDialogFragment(entryLucraFlow)
                    true
                } else {
                    false
                }
            }

            override fun onFlowDismissRequested(entryLucraFlow: LucraUiProvider.LucraFlow) {
                Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow")
                // Routed rather than looked up here: this listener is registered once, but
                // flows are presented from more than one screen, and each activity has its
                // own FragmentManager.
                if (!LucraFlowPresenter.dismiss(entryLucraFlow, supportFragmentManager)) {
                    Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow not found")
                }
            }
        }
    )

    private fun getEnvironmentFromBuildType(): Environment = sampleEnvironment()

    private fun setupAuthSettingsButton() {
        headerSettings.setOnClickListener { settingsSheet.show(::settingsRows) }
    }

    private fun settingsRows(): List<SettingsRow> = buildList {
        add(SettingsRow.Group("SDK"))
        add(SettingsRow.Info("Environment", BuildConfig.BUILD_TYPE))
        add(
            SettingsRow.Action(
                title = "API key",
                value = if (apiKeyOverride != null) "override" else "from build",
                restarts = true,
                onClick = ::overrideApiUrlAndKey,
            )
        )
        add(
            SettingsRow.Action(
                title = "Theme mode",
                value = themeModeOverride?.name ?: "derived",
                restarts = true,
            ) {
                themeModeOverride = nextThemeMode(themeModeOverride)
                restartActivity()
            }
        )
        add(
            SettingsRow.Toggle(
                title = "Full screen flows",
                checked = launchFullScreen,
            ) { launchFullScreen = it }
        )
        add(
            SettingsRow.Toggle(
                title = "Auto-join tournaments",
                checked = autoJoinEnabled,
                restarts = true,
            ) { autoJoinEnabled = it }
        )
        add(
            SettingsRow.Toggle(
                title = "Suppress reward sheet",
                checked = suppressRewardSheet,
                restarts = true,
            ) {
                suppressRewardSheet = it
                restartActivity()
            }
        )

        add(SettingsRow.Group("Providers"))
        add(
            SettingsRow.Action(
                title = "Reward provider",
                value = if (lucraRewardProviderEnabled) "on · ${fakeLucraRewards.size}" else "off",
            ) {
                configDialogs.showRewardProviderDialog(
                    currentRewards = fakeLucraRewards,
                    enabled = lucraRewardProviderEnabled
                ) { enabled, updatedRewards ->
                    lucraRewardProviderEnabled = enabled
                    fakeLucraRewards = updatedRewards
                    setupRewardProvider(if (enabled) updatedRewards else null)
                }
            }
        )
        add(
            SettingsRow.Action(title = "Convert to credit") {
                configDialogs.showConvertToCreditDialog { provider ->
                    LucraClient().setConvertToCreditProvider(provider)
                }
            }
        )
        add(
            SettingsRow.Action(
                title = "League filter",
                value = LucraClient().getPublicFeedLeagueIdFilter()
                    .currentIdFilters.value.size
                    .let { if (it == 0) "none" else "$it" },
            ) {
                val leagueFilter = LucraClient().getPublicFeedLeagueIdFilter()
                configDialogs.showLeagueFilterDialog(
                    currentFilters = leagueFilter.currentIdFilters.value,
                    onAddFilter = { id -> leagueFilter.addId(id) },
                    onClearFilters = { leagueFilter.clearIds() }
                )
            }
        )
        add(
            SettingsRow.Action(title = "Theme colors") {
                configDialogs.showThemingDialog(themeManager, ::restartActivity)
            }
        )
        add(
            SettingsRow.Action(
                title = "Remembered IDs",
                value = recentIdStore.total().let { if (it == 0) "none" else "$it stored" },
                onClick = ::showRememberedIds,
            )
        )

        add(SettingsRow.Group("Actions"))
        add(
            SettingsRow.Action(title = "View configuration") {
                configDialogs.showViewConfigurationDialog(LucraClient().revealConfiguration())
            }
        )
        add(
            SettingsRow.Action(title = "Close all flows in 10s") {
                lifecycleScope.launch {
                    delay(10000)
                    LucraClient().closeFullScreenLucraFlows(supportFragmentManager)
                }
            }
        )
        add(
            SettingsRow.Action(title = "Restart SDK", destructive = true, onClick = ::restartActivity)
        )

        add(SettingsRow.Group("Build"))
        add(SettingsRow.Info("Version", "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"))
        if (BuildConfig.GIT_BRANCH.isNotEmpty()) {
            add(SettingsRow.Info("Branch", BuildConfig.GIT_BRANCH))
        }
        if (BuildConfig.GIT_COMMIT.isNotEmpty()) {
            add(SettingsRow.Info("Commit", BuildConfig.GIT_COMMIT))
        }
    }

    private fun showRememberedIds() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remembered IDs")
            .setMessage(recentIdStore.summary())
            .setPositiveButton("Close", null)
            .setNegativeButton("Clear") { dialog, _ ->
                recentIdStore.clear()
                dialog.dismiss()
            }
            .show()
    }

    private fun overrideApiUrlAndKey() {
        configDialogs.showOverrideApiKeyDialog(
            currentApiKeyOverride = apiKeyOverride,
            buildApiKeySet = buildApiKeySet(),
            onSave = { newApiKey ->
                apiKeyOverride = newApiKey
                restartActivity()
            },
            onReset = {
                apiKeyOverride = null
                restartActivity()
            }
        )
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
                            userApiHandler.logout()
                            dialog.dismiss()
                        }
                        setNeutralButton(
                            "Copy"
                        ) { _, _ ->
                            val clipboard: ClipboardManager =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "Lucra SDK User Info",
                                lucraSDKUser!!.toString()
                            )
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
                        userDialogs.showConfigureUserDialog(lucraSDKUser) { updatedUser ->
                            lucraSDKUser = updatedUser
                        }
                        dialog.dismiss()
                    }
                }
                .show()
        }
    }

    private fun setupHeaderChips() {
        chipEnvironment.text = BuildConfig.BUILD_TYPE
        chipEnvironment.setTextColor(
            ContextCompat.getColor(
                this,
                when (BuildConfig.BUILD_TYPE) {
                    "release" -> R.color.env_release
                    "staging", "sandbox" -> R.color.env_staging
                    else -> R.color.env_debug
                }
            )
        )
        chipVersion.text = "v${BuildConfig.VERSION_NAME}"
        chipApiKey.isVisible = !buildApiKeySet()
        renderAuthChip()
    }

    private fun renderAuthChip() {
        val user = lucraSDKUser
        headerAuthStatus.text = user?.username?.takeIf { it.isNotBlank() }
            ?: user?.userId?.take(8)
            ?: getString(R.string.catalog_signed_out)
        headerAuthStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (user?.userId != null) R.color.signed_in else R.color.text_muted
            )
        )
    }

    private fun setupCatalog(savedInstanceState: Bundle?) {
        catalogAdapter = SampleCatalogAdapter(
            onEntryClick = ::onEntrySelected,
            onCategoryClick = ::onCategoryToggled,
            onClearSearch = { searchField.setText("") },
            hostedComponent = hostedComponents::get,
        )
        catalogList.layoutManager = LinearLayoutManager(this)
        catalogList.adapter = catalogAdapter
        // Cross-fading every row on each keystroke reads as flicker.
        (catalogList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        searchField.doAfterTextChanged { text ->
            update { it.copy(query = text?.toString().orEmpty()) }
        }

        // Per chip, not on the ChipGroup: CompoundButton is stable across the Material versions
        // this module compiles against and runs against.
        surfaceChips().forEach { (chip, surface) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) update { it.copy(surface = surface) }
            }
        }

        uiState = catalogPreferences.load().copy(
            query = savedInstanceState?.getString(STATE_QUERY).orEmpty(),
            signedIn = lucraSDKUser?.userId != null,
        )
        surfaceChips().first { (_, surface) -> surface == uiState.surface }.first.isChecked = true
        if (uiState.query.isNotEmpty()) searchField.setText(uiState.query)
        render()
    }

    private fun surfaceChips(): List<Pair<Chip, SampleSurface?>> = listOf(
        chipSurfaceAll to null,
        chipSurfaceFlows to SampleSurface.Flow,
        chipSurfaceApis to SampleSurface.Api,
        chipSurfaceUi to SampleSurface.Ui,
    )

    private fun update(transform: (CatalogUiState) -> CatalogUiState) {
        uiState = transform(uiState)
        catalogPreferences.save(uiState)
        render()
    }

    private fun render() {
        catalogAdapter.submitList(buildListItems(catalog, uiState))

        val counts = surfaceCounts(catalog, uiState.query)
        chipSurfaceAll.text = getString(R.string.catalog_filter_all, counts[null] ?: 0)
        chipSurfaceFlows.text =
            getString(R.string.catalog_filter_flows, counts[SampleSurface.Flow] ?: 0)
        chipSurfaceApis.text =
            getString(R.string.catalog_filter_apis, counts[SampleSurface.Api] ?: 0)
        chipSurfaceUi.text = getString(R.string.catalog_filter_ui, counts[SampleSurface.Ui] ?: 0)

        surfaceBlurb.setText(
            when (uiState.surface) {
                SampleSurface.Flow -> R.string.lucra_flow_description
                SampleSurface.Api -> R.string.lucra_headless_description
                SampleSurface.Ui -> R.string.lucra_component_description
                null -> R.string.catalog_blurb_all
            }
        )
    }

    private fun onEntrySelected(entryId: String) {
        val entry = catalogById[entryId] ?: return
        if (entry.auth == AuthRequirement.HostBlocks && lucraSDKUser?.userId == null) {
            Toast.makeText(this, "Not logged in yet!", Toast.LENGTH_SHORT).show()
            return
        }
        update {
            it.copy(recentIds = (listOf(entryId) + it.recentIds).distinct().take(MAX_RECENTS))
        }
        if (entry.surface == SampleSurface.Ui) toggleComponent(entry) else entry.onSelect(this)
    }

    private fun onCategoryToggled(category: SampleCategory) {
        update {
            val expanded =
                if (category in it.expanded) it.expanded - category else it.expanded + category
            it.copy(expanded = expanded)
        }
    }

    override val flows get() = flowDialogs
    override val tournaments get() = tournamentDialogs
    override val users get() = userDialogs
    override val recreational get() = recreationalGameDialogs
    override val components get() = componentDialogs
    override val miniGames get() = miniGameDialogs
    override val debug get() = debugDialogs
    override val matchupApi get() = matchupApiHandler
    override val tournamentApi get() = tournamentApiHandler
    override val phoneAuthApi get() = phoneAuthApiHandler
    override val handshakeAuthApi get() = handshakeAuthApiHandler
    override val userApi get() = userApiHandler

    override val currentUser: SDKUser? get() = lucraSDKUser

    override fun onUserUpdated(user: SDKUser?) {
        lucraSDKUser = user
    }

    override fun lucraComponent(component: LucraUiProvider.LucraComponent): View =
        LucraClient().getLucraComponent(this, component)

    /** A cancelled ID prompt never calls back, so nothing expands in that case. */
    private fun toggleComponent(entry: SampleEntry) {
        if (entry.id in uiState.expandedComponents) {
            releaseComponent(entry.id)
            update { it.copy(expandedComponents = it.expandedComponents - entry.id) }
            return
        }
        val provider = entry.embedded ?: return
        provider.provide(this) { view ->
            hostedComponents[entry.id] = view
            update { it.copy(expandedComponents = it.expandedComponents + entry.id) }
        }
    }

    private fun releaseComponent(entryId: String) {
        val view = hostedComponents.remove(entryId) ?: return
        (view.parent as? ViewGroup)?.removeView(view)
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        themeManager.onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) { /* no-op */
    }

    private fun observeLoggedInUser() {
        // Or use observeSDKUser { result -> ... }
        LucraClient().observeSDKUserFlow().onEach { sdkUserResult ->
            when (sdkUserResult) {
                is SDKUserResult.Success -> {
                    Log.d("Lucra SDK Sample", "Fetched latest user")
                    lucraSDKUser = sdkUserResult.sdkUser
                }

                is SDKUserResult.Error -> {
                    Log.e("Lucra SDK Sample", "Unable to get username ${sdkUserResult.error}")
                    lucraSDKUser = null
                }

                SDKUserResult.NotLoggedIn, SDKUserResult.WaitingForLogin -> {
                    Log.e("Lucra SDK Sample", "User not logged in yet!")
                    lucraSDKUser = null
                }

                // Transient: keep the last known user rather than flashing signed out.
                SDKUserResult.Loading, SDKUserResult.InvalidUsername -> Unit
            }
            onAuthStateChanged()
        }.launchIn(lifecycleScope)
    }

    private fun onAuthStateChanged() {
        val signedIn = lucraSDKUser?.userId != null
        renderAuthChip()
        // Drives the lock icons, so signing in visibly unlocks the gated rows.
        if (::catalogAdapter.isInitialized) update { it.copy(signedIn = signedIn) }
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
        LucraFlowPresenter.present(this, lucraFlow)
    }

    override fun launchFlow(flow: LucraUiProvider.LucraFlow) {
        showLucraDialogFragment(flow)
    }

    override fun showRewardSheet() {
        // Unseeded, so the sheet runs the same combined-rewards fetch auto-show does.
        lucraUi.getRewardSheetDialogFragment().show(supportFragmentManager, TAG_REWARD_SHEET)
    }

    override fun onDestroy() {
        super.onDestroy()
        // NOTE: Don't release on rotation with a dialog fragment open, this will break the instance
        // e.g. DialogFragment will recover before a flow has been set again.
//        LucraClient.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeeplink(intent)
        if (intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra(
                "branch_force_new_session",
                false
            )
        ) {
            Branch.sessionBuilder(this).withCallback { referringParams, error ->
                if (error != null) {
                    Log.e("Sample", "BranchIO new intent error: ${error.message}")
                } else if (referringParams != null) {
                    Log.i("Sample", "BranchIO referring params: $referringParams")
                }
            }.reInit()
        }
    }

    private fun handleNotificationDeeplink(intent: Intent) {
        val deeplink = intent.getStringExtra(NOTIFICATION_DEEPLINK)
            ?: intent.extras?.getString("deeplink")
            ?: return

        val uri = try {
            deeplink.toUri()
        } catch (_: Exception) {
            return
        }

        val tournamentMatchupId = uri.getQueryParameter("matchupId")
            ?.takeIf { uri.lastPathSegment.equals(TOURNAMENT_DETAIL_PATH_SEGMENT, ignoreCase = true) }

        val isRewardsPath = uri.lastPathSegment.equals(REWARDS_PATH_SEGMENT, ignoreCase = true)
        val achievementId = uri.getQueryParameter("achievementId")?.takeIf { it.isNotBlank() }

        when {
            !tournamentMatchupId.isNullOrBlank() ->
                launchFlow(LucraUiProvider.LucraFlow.TournamentDetails(tournamentMatchupId))

            isRewardsPath && achievementId != null ->
                launchFlow(LucraUiProvider.LucraFlow.MinigamesRewards)

            isRewardsPath ->
                launchFlow(LucraUiProvider.LucraFlow.Profile)

            else -> {
                LucraPushNotificationService.handleNotificationIntent(intent)?.let { launchFlow(it) }
            }
        }
    }
}
