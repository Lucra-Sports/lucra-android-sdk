package com.lucrasports.sdk.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.LucraClient.Companion.Environment
import com.lucrasports.sdk.core.contest.GamesMatchup
import com.lucrasports.sdk.core.style_guide.ClientTheme
import com.lucrasports.sdk.core.style_guide.ColorStyle
import com.lucrasports.sdk.core.style_guide.Font
import com.lucrasports.sdk.core.style_guide.FontFamily
import com.lucrasports.sdk.core.style_guide.FontWeight
import com.lucrasports.sdk.core.ui.LucraFlowListener
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult
import com.lucrasports.sdk.ui.LucraUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class MainActivitySdk : AppCompatActivity() {

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

    // Managing latest user
    private var lucraSDKUser: SDKUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_sdk)

        LucraClient.initialize(
            application = application,
            lucraUiProvider = buildLucraUiInstance(),
            // This must be updated to the correct auth0 client id per environment
            // Logins won't work if there's a mismatch
            apiKey = BuildConfig.TESTING_API_KEY,
            // This must be updated to the correct api url per environment
            apiUrl = BuildConfig.TESTING_API_URL,
            environment = getEnvironmentFromBuildType(),
            outputLogs = true,
            clientTheme = ClientTheme(
                colorStyle = ColorStyle(
                    primary = "#1976D2",
                    secondary = "#F57C00",
                    tertiary = "#388E3C",
                    surface = "#FFFFFF",
                    background = "#F5F5F5",
                    onPrimary = "#FFFFFF",
                    onSecondary = "#FFFFFF",
                    onTertiary = "#FFFFFF",
                    onSurface = "#000000",
                    onBackground = "#000000",
                ),
                fontFamily = FontFamily(
                    listOf(
                        Font(
                            fontName = "merriweather_bold",
                            weight = FontWeight.Bold
                        ),
                        Font(
                            fontName = "merriweather_regular",
                            weight = FontWeight.Normal
                        ),
                        Font(
                            fontName = "merriweather_medium",
                            weight = FontWeight.Medium
                        )
                    )
                )
            )
        )

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
            "Mini Public Feed",
            "Show the mini public feed, showing contest cards in an non-scroll list. No authentication required. Any proceeding actions with prompt the user to authenticate first",
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
                            playerOneId = playerOneId,
                            playerTwoId = playerTwoId
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
            "Retrieve Games Matchup",
            "A prompt will show to set the matchup_id. Authentication required.",
            apiSection,
            AppCompatResources.getDrawable(this, R.drawable.ic_api)
        ) {
            retrieveMatch()
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
                        "Set League Filter",
                        "View Recent IDs of games, leagues and players"
                    )
                ) { dialog, which ->
                    when (which) {
                        0 -> {
                            val editText =
                                layoutInflater.inflate(R.layout.main_option_setting_edit_text, null)
                            val leagueFilter = LucraClient().getPublicFeedLeagueIdFilter()
                            editText.findViewById<TextInputLayout>(R.id.option_setting_edit_text_layout)
                                .setHint("Add a league filter id")
                            if (leagueFilter.currentIdFilters.value.isNotEmpty()) {
                                editText.findViewById<TextView>(R.id.option_setting_edit_text_supporting_text)
                                    .setText(
                                        "Active ids: " + leagueFilter.currentIdFilters.value.joinToString(
                                            ",\n"
                                        )
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

                        1 -> {
                            // TODO expose an internal list which contains list of recent
                            //  Games Ids, League Ids, and Player Ids
                            //  It should be a capped list and for testing purposes only
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Recent Game Data")
                                .setMessage("This feature is not yet implemented")
                                .setPositiveButton("Close", null)
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }
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
                    setNegativeButton("Close") { dialog, id ->
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
            "Verify User Identity",
            "Navigate user identity verification screen. Authentication required",
            flowsSection
        ) {
            launchFlow(LucraUiProvider.LucraFlow.VerifyIdentity)
        }
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

                SDKUserResult.NotLoggedIn -> {
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

    private fun retrieveMatch() {
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

                    // Step 2: Update the sdk user's username
                    LucraClient().updateUsername(lucraSDKUser!!, newUsername) {
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
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        builder.show()
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
}