package com.lucrasports.sdk.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.lucrasports.feature.reward_selection_flow.components.RedeemRewardDialogFragment
import com.lucrasports.feature.reward_selection_flow.components.ViewMyRewardsDialogFragment
import com.lucrasports.feature.reward_selection_flow.components.ViewMyRewardsDialogFragment.ViewMyRewardsListener
import com.lucrasports.sdk.app.fake_resources.fakeLucraRewards
import com.lucrasports.sdk.app.fake_resources.fakeLucraTournamentRewards
import com.lucrasports.sdk.app.headless_api.MatchupApiHandler
import com.lucrasports.sdk.app.headless_api.UserApiHandler
import com.lucrasports.sdk.app.logger.FirebaseLogger
import com.lucrasports.sdk.app.ui.OptionBuilder
import com.lucrasports.sdk.app.ui.dialogs.ComponentDialogs
import com.lucrasports.sdk.app.ui.dialogs.ConfigDialogs
import com.lucrasports.sdk.app.ui.dialogs.FlowDialogs
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
import com.lucrasports.sdk.core.ui.LucraFlowListener
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult
import com.lucrasports.sdk.ui.LucraUi
import com.lucrasports.sdk.ui.push_notifications.LucraPushNotificationService
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

private const val API_KEY_OVERRIDE = "API_KEY_OVERRIDE"
private const val TAG_REDEEM_DIALOG = "TAG_REDEEM_DIALOG"
private const val TAG_VIEW_REWARDS = "TAG_VIEW_REWARDS_DIALOG"

class MainActivitySdk : AppCompatActivity(), ColorPickerDialogListener {

    private val  topAppBar: CollapsingToolbarLayout by lazy {
        findViewById(R.id.top_app_bar)
    }

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

    // Helper classes
    private lateinit var recreationalGameDialogs: RecreationalGameDialogs
    private lateinit var tournamentDialogs: TournamentDialogs
    private lateinit var userDialogs: UserDialogs
    private lateinit var configDialogs: ConfigDialogs
    private lateinit var flowDialogs: FlowDialogs
    private lateinit var componentDialogs: ComponentDialogs
    private lateinit var matchupApiHandler: MatchupApiHandler
    private lateinit var userApiHandler: UserApiHandler
    private lateinit var themeManager: ThemeManager
    private lateinit var optionBuilder: OptionBuilder

    private data class FlowOption(
        val title: String,
        val description: String,
        val action: () -> Unit
    )

    private val preferences by lazy {
        getSharedPreferences("LucraSamplePrefs", MODE_PRIVATE)
    }

    private var apiKeyOverride: String?
        get() = preferences.getString(API_KEY_OVERRIDE, null).takeIf { !it.isNullOrBlank() }
        set(value) {
            preferences.edit { putString(API_KEY_OVERRIDE, value) }
        }

    private var lucraRewardProviderEnabled = true
    private var provideLocationIdOnInit = false

    // Managing latest user
    private var lucraSDKUser: SDKUser? = null

    private lateinit var customLogger: FirebaseLogger
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
        matchupApiHandler = MatchupApiHandler(this)
        userApiHandler = UserApiHandler(this)
        themeManager = ThemeManager(this)
        optionBuilder = OptionBuilder(this)

        ViewCompat.setOnApplyWindowInsetsListener(topAppBar) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            topAppBar.setPadding(
                systemBarsInsets.left,
                statusBarInsets.top,
                systemBarsInsets.right,
                0
            )

            insets
        }

        initializeLucraClient()

        setupPushNotifications()

        val flow = LucraPushNotificationService.handleNotificationIntent(intent)
        flow?.let { launchFlow(it) }

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

    private fun buildApiKeySet() = BuildConfig.TESTING_API_KEY != "ADD YOUR API KEY HERE"

    private fun initializeLucraClient() {
        val apiKeySet = buildApiKeySet() || apiKeyOverride != null
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
            lucraUiProvider = buildLucraUiInstance(),
            apiKey = apiKeyOverride ?: BuildConfig.TESTING_API_KEY,
            environment = getEnvironmentFromBuildType(),
            outputLogs = true,
            customLogger = customLogger,
            clientTheme = ClientTheme(
                lightColorStyle = SampleColorStore.getLightColorStyle(),
                darkColorStyle = SampleColorStore.getDarkColorStyle(),
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
                        Log.d("Sample", "Active game contest started: ${event.matchupId} match object: ${event.lucraMatchup}")

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
                val buo = BranchUniversalObject().setContentMetadata( ContentMetadata().addCustomMetadata("matchupId", matchupId) )
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

    private fun appendToggleComponent(
        title: String,
        description: String,
        componentProvider: () -> View
    ) {
        appendOption(title, description, componentsSection, AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)) { viewGroup ->
            if (viewGroup.isNotEmpty()) {
                viewGroup.removeAllViews()
            } else {
                viewGroup.addView(componentProvider())
            }
        }
    }

    private fun appendComponentOptions() {
        appendToggleComponent(
            "Profile Pill",
            "Show the profile pill with the user balance. Authentication not required, but clicking will launch the auth flow."
        ) {
            LucraClient().getLucraComponent(this, LucraUiProvider.LucraComponent.ProfilePill { launchFlow(it) })
        }

        appendToggleComponent(
            "Recommended Matchups Banner",
            "Show the recommended matchups banner component. Authentication required."
        ) {
            LucraClient().getLucraComponent(this, LucraUiProvider.LucraComponent.RecommendedMatchups { launchFlow(it) })
        }

        appendToggleComponent(
            "Floating Action Button",
            "Show the floating action button to create a sports contest."
        ) {
            LucraClient().getLucraComponent(this, LucraUiProvider.LucraComponent.FloatingActionButton { launchFlow(it) })
        }

        appendOption(
            "Mini Public Feed",
            "Show the mini public feed, showing contest cards in an non-scroll list. This will prompt for two player Ids but it can accept an array. No authentication required. Any proceeding actions with prompt the user to authenticate first",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.isNotEmpty()) {
                viewGroup.removeAllViews()
                return@appendOption
            }

            componentDialogs.showMiniPublicFeedDialog { playerOneId, playerTwoId ->
                val view = LucraClient().getLucraComponent(
                    this,
                    LucraUiProvider.LucraComponent.MiniPublicFeed(listOf(playerOneId, playerTwoId)) {
                        launchFlow(it)
                    }
                )
                viewGroup.addView(view)
            }
        }

        appendOption(
            "Contest Card",
            "Show contest card for a specific contest",
            componentsSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_down)
        ) { viewGroup ->
            if (viewGroup.isNotEmpty()) {
                viewGroup.removeAllViews()
                return@appendOption
            }

            componentDialogs.showContestCardDialog { contestId ->
                val view = LucraClient().getLucraComponent(
                    this,
                    LucraUiProvider.LucraComponent.ContestCard(contestId = contestId) {
                        launchFlow(it)
                    }
                )
                viewGroup.addView(view)
            }
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
                Log.d("Sample", "backstack count: ${supportFragmentManager.backStackEntryCount}")
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
            "dev2" -> Environment.DEVELOPMENT2
            else -> Environment.STAGING
        }
    }

    private fun appendApiOptions() {
        appendOption(
            "Logout",
            "Logout the current user.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            userApiHandler.logout()
        }

        appendOption(
            "Update Username",
            "A prompt will show to update the current user's username. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            userDialogs.showUpdateUsernameDialog(lucraSDKUser) { updatedUser ->
                lucraSDKUser = updatedUser
            }
        }

        appendOption(
            "Configure User",
            "A prompt will show to update or preconfigure the SDK User. Authentication not required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            userDialogs.showConfigureUserDialog(lucraSDKUser) { updatedUser ->
                lucraSDKUser = updatedUser
            }
        }

        appendOption(
            "Retrieve Matchup",
            "A prompt will show to set the matchup_id.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            matchupApiHandler.showRetrieveMatchupDialog()
        }

        appendOption(
            "Check KYC Status",
            "Verify the KYC status of the current user. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                userApiHandler.checkKYCStatus(lucraSDKUser!!.userId!!)
            }
        }

        appendOption(
            "Retrieve Tournament",
            "A prompt will show to set the tournament_id.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                tournamentDialogs.showRetrieveTournamentDialog()
            }
        }

        appendOption(
            "Join Tournament",
            "A prompt will show to set the tournament_id. For free tournaments, will launch demographic form if email/zip missing. For paid tournaments, will launch verification if not verified.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                tournamentDialogs.showJoinTournamentDialog(::launchFlow)
            }
        }

        appendOption(
            "Submit tournament score manually",
            "Submit the score of a tournament for a specified tournament. Tournament ID is required",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                tournamentDialogs.showSubmitScoreDialog(::launchFlow)
            }
        }

        appendOption(
            "Submit Score by Metadata",
            "Submit a tournament score by matching matchup details, game ID or location ID.. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            if (lucraSDKUser?.userId == null) {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Not logged in yet!",
                    Toast.LENGTH_SHORT
                ).show()
                return@appendOption
            }
            tournamentDialogs.showSubmitUserScoreByMetadataDialog()
        }

        appendOption(
            "Search Matchups by Metadata",
            "Search for matchups using metadata criteria. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            if (lucraSDKUser?.userId == null) {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Not logged in yet!",
                    Toast.LENGTH_SHORT
                ).show()
                return@appendOption
            }
            tournamentDialogs.showSearchMatchupsByMetadataDialog()
        }


        appendOption(
            "Recommended Tournaments",
            "Retrieve 20 recommended tournaments.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                tournamentDialogs.showRecommendedTournamentsDialog()
            }
        }

        appendOption(
            "Recommended Tournaments Light",
            "Retrieve lightweight recommended tournament data. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                tournamentDialogs.showRecommendedTournamentsLightDialog()
            }
        }


        // Add the recreational games API options
        appendRecreationalGamesApiOptions()
    }


    private fun appendRecreationalGamesApiOptions() {
        appendOption(
            "Create Recreational Game",
            "Create a recreational game with specified parameters. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                recreationalGameDialogs.showCreateGameDialog()
            }
        }

        appendOption(
            "Accept Versus Recreational Game",
            "Accept a Group vs Group recreational game. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                recreationalGameDialogs.showAcceptVersusGameDialog()
            }
        }

        appendOption(
            "Accept Free-For-All Recreational Game",
            "Accept a Free-For-All recreational game. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                recreationalGameDialogs.showAcceptFreeForAllGameDialog()
            }
        }

        appendOption(
            "Cancel Recreational Game",
            "Cancel a recreational game. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            requireAuth {
                recreationalGameDialogs.showCancelGameDialog()
            }
        }
    }

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
                        "Update Style Colors",
                        "Configure Reward Service",
                        "Update Convert to Credit Info",
                        "View Configuration",
                        "Close all Lucra Flows in 10 seconds",
                    )
                ) { dialog, which ->
                    when (which) {
                        0 -> {
                            overrideApiUrlAndKey()
                        }

                        1 -> {
                            val leagueFilter = LucraClient().getPublicFeedLeagueIdFilter()
                            configDialogs.showLeagueFilterDialog(
                                currentFilters = leagueFilter.currentIdFilters.value,
                                onAddFilter = { id -> leagueFilter.addId(id) },
                                onClearFilters = { leagueFilter.clearIds() }
                            )
                        }

                        2 -> {
                            // TODO expose an internal list which contains list of recent
                            //  Games Ids, League Ids, and Player Ids
                            //  It should be a capped list and for testing purposes only
                            configDialogs.showNotImplementedDialog("Recent Game Data")
                        }

                        3 -> {
                            configDialogs.showThemingDialog(themeManager, ::restartActivity)
                        }

                        4 -> {
                            configDialogs.showRewardProviderDialog(
                                currentRewards = fakeLucraRewards,
                                enabled = lucraRewardProviderEnabled
                            ) { enabled, updatedRewards ->
                                lucraRewardProviderEnabled = enabled
                                fakeLucraRewards = updatedRewards
                                setupRewardProvider(if (enabled) updatedRewards else null)
                            }
                        }

                        5 -> {
                            configDialogs.showConvertToCreditDialog { provider ->
                                LucraClient().setConvertToCreditProvider(provider)
                            }
                        }

                        6 -> {
                            configDialogs.showViewConfigurationDialog(LucraClient().revealConfiguration())
                        }

                        7 -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(10000)
                                LucraClient().closeFullScreenLucraFlows(supportFragmentManager)
                            }
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }
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

    private fun getFlowOptions(): List<FlowOption> = listOf(
        FlowOption(
            "Profile",
            "Navigate to the current user's profile. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.Profile) },
        
        FlowOption(
            "Home Page",
            "Navigate to the primary starting point for Tournaments and Games."
        ) { flowDialogs.showHomePageDialog(::launchFlow) },
        
        FlowOption(
            "Wallet",
            "Navigate to the current user's wallet. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.Wallet) },
        
        FlowOption(
            "Verify User Identity",
            "Navigate user identity verification screen. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.VerifyIdentity) },
        
        FlowOption(
            "Demographic Form",
            "Navigate to the demographic form to collect user information. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.DemographicForm) },
        
        FlowOption(
            "Create Games Matchup",
            "Navigate to the create games match up flow. Authentication required"
        ) { flowDialogs.showCreateGamesMatchupDialog(::launchFlow) },
        
        FlowOption(
            "Login",
            "Navigate directly to the login screen. If user is already logged in, this will exit immediately"
        ) { launchFlow(LucraUiProvider.LucraFlow.Login) },
        
        FlowOption(
            "Add Funds",
            "Navigate to the add funds flow. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.AddFunds) },
        
        FlowOption(
            "Withdraw Funds",
            "Navigate to the withdraw funds flow. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.WithdrawFunds) },
        
        FlowOption(
            "Public Sports Feed",
            "Navigate to the public sports feed. No authentication required. Any proceeding actions with prompt the user to authenticate first"
        ) { launchFlow(LucraUiProvider.LucraFlow.PublicFeed) },
        
        FlowOption(
            "Create Sports Matchup",
            "Navigate to the create sports match up flow. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.CreateSportsMatchup) },
        
        FlowOption(
            "Show Matchup",
            "Navigate to the matchup details flow. Authentication required"
        ) { flowDialogs.showMatchupDetailsDialog(::launchFlow) },
        
        FlowOption(
            "Show Tournament Matchup",
            "Navigate to the tournament details flow. Authentication required"
        ) { flowDialogs.showTournamentDetailsDialog(::launchFlow) },
        
        FlowOption(
            "My Matchups",
            "Navigate to the user's created matchups screen. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.MyMatchup) },
        
        FlowOption(
            "Deeplink to Matchup Details",
            "Navigate to specific matchup via a legacy deeplink uri. Authentication required"
        ) { flowDialogs.showDeeplinkDialog(::launchFlow) },
        
        FlowOption(
            "Tournaments",
            "Navigate to the Tournaments screen."
        ) { launchFlow(LucraUiProvider.LucraFlow.Tournaments) },

        FlowOption(
            "Achievements",
            "Navigate to the Achievements screen to view and claim achievement rewards. Authentication required"
        ) { launchFlow(LucraUiProvider.LucraFlow.Achievements) },

        FlowOption(
            "Claim Prize Sheet",
            "Launches the claim prize bottom sheet seeded with sample tournament rewards. " +
                "Demonstrates that the sheet is invokable as a regular LucraFlow via onLaunchFlow(...)."
        ) { launchFlow(LucraUiProvider.LucraFlow.ClaimRewards(fakeLucraTournamentRewards)) }
    )

    private fun appendFlowOptions() {
        getFlowOptions().forEach { option ->
            appendOption(option.title, option.description, flowsSection) { option.action() }
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        themeManager.onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) { /* no-op */ }

    private inline fun requireAuth(action: () -> Unit) {
        if (lucraSDKUser?.userId == null) {
            Toast.makeText(this, "Not logged in yet!", Toast.LENGTH_SHORT).show()
            return
        }
        action()
    }

    private fun appendOption(
        title: String,
        description: String,
        root: ViewGroup,
        drawable: Drawable? = null,
        onClick: (ViewGroup) -> Unit
    ) {
        optionBuilder.appendOption(title, description, root, drawable, onClick)
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

                else -> {

                }
            }
        }.launchIn(lifecycleScope)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val flow = LucraPushNotificationService.handleNotificationIntent(intent)
        flow?.let { launchFlow(it) }
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
}
