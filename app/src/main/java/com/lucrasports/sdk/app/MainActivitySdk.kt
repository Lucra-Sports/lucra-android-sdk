package com.lucrasports.sdk.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.LucraClient.Companion.Environment
import com.lucrasports.sdk.core.style_guide.ClientTheme
import com.lucrasports.sdk.core.style_guide.ColorStyle
import com.lucrasports.sdk.core.style_guide.Font
import com.lucrasports.sdk.core.style_guide.FontFamily
import com.lucrasports.sdk.core.style_guide.FontWeight
import com.lucrasports.sdk.core.ui.LucraFlowListener
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUserResult
import com.lucrasports.sdk.ui.LucraUi

class MainActivitySdk : AppCompatActivity() {

    private val header: TextView by lazy {
        findViewById(R.id.header)
    }

    private val fullScreenSwitch: SwitchCompat by lazy {
        findViewById(R.id.fullScreenSwitch)
    }

    private val logoutButton: AppCompatButton by lazy {
        findViewById(R.id.logoutButton)
    }

    private val loginButton: AppCompatButton by lazy {
        findViewById(R.id.loginButton)
    }

    private val profileButton: AppCompatButton by lazy {
        findViewById(R.id.profileButton)
    }

    private val addFundsButton: AppCompatButton by lazy {
        findViewById(R.id.addFundsButton)
    }

    private val createGamesButton: AppCompatButton by lazy {
        findViewById(R.id.createGamesButton)
    }

    private val createSportsButton: AppCompatButton by lazy {
        findViewById(R.id.createSportsButton)
    }

    private val contestFeedButton: AppCompatButton by lazy {
        findViewById(R.id.contestFeedButton)
    }

    private val verifyIdentityButton: AppCompatButton by lazy {
        findViewById(R.id.verifyIdentityButton)
    }

    private val withdrawFundsButton: AppCompatButton by lazy {
        findViewById(R.id.withdrawFundsButton)
    }

    private val myContestsButton: AppCompatButton by lazy {
        findViewById(R.id.myContestsButton)
    }

    private val updateUsernameButton: AppCompatButton by lazy {
        findViewById(R.id.updateUsernameButton)
    }

    private val allButtons: List<AppCompatButton> by lazy {
        listOf(profileButton, addFundsButton, createGamesButton)
    }

    private val fragmentContainerView: FragmentContainerView
        get() = findViewById(R.id.lucraFragment)

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

        header.text = "Lucra SDK (${BuildConfig.BUILD_TYPE})"

        loginButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.Login)
        }

        createSportsButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.CreateSportsMatchup)
        }

        logoutButton.setOnClickListener {
            LucraClient().logout(this)
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_LONG).show()
        }

        addFundsButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.AddFunds)
        }

        profileButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.Profile)
        }

        createGamesButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.CreateGamesMatchup)
        }

        withdrawFundsButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.WithdrawFunds)
        }

        myContestsButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.MyMatchup)
        }

        contestFeedButton.setOnClickListener {
            launchFlow(LucraUiProvider.LucraFlow.PublicFeed)
        }

        updateUsernameButton.setOnClickListener {
            updateUsernameDialog()
        }

        verifyIdentityButton.setOnClickListener {
            LucraClient().checkUsersKYCStatus(
                "<insert user id>", // TODO use LucraClient().getSDKUser {  }
                object : LucraClient.LucraKYCStatusListener {
                    override fun onKYCStatusCheckFailed(exception: Exception) {
                        Toast.makeText(
                            this@MainActivitySdk,
                            "Verified Failed ${exception}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onKYCStatusAvailable(isVerified: Boolean) {
                        Toast.makeText(this@MainActivitySdk, "Verified Success", Toast.LENGTH_LONG)
                            .show()
                    }
                })

            launchFlow(LucraUiProvider.LucraFlow.VerifyIdentity)
        }
    }

    private fun getEnvironmentFromBuildType(): Environment {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> Environment.DEVELOPMENT
            "staging" -> Environment.STAGING
            "sandbox" -> Environment.SANDBOX
            "release" -> Environment.PRODUCTION
            else -> Environment.STAGING
        }
    }

    private fun updateUsernameDialog() {

        // Step 1: Fetch the current SDK user
        LucraClient().getSDKUser {
            when (it) {
                is SDKUserResult.Error -> {
                    Log.e("Lucra SDK Sample", "Unable to get username ${it.error}")
                    Toast.makeText(
                        this@MainActivitySdk,
                        "Unable to get user name",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }

                SDKUserResult.InvalidUsername -> {
                    // Shouldn't happen here
                }

                SDKUserResult.NotLoggedIn -> {
                    Log.e(
                        "Lucra SDK Sample",
                        "User not logged in yet! Close this dialog and try again"
                    )
                    Toast.makeText(
                        this@MainActivitySdk,
                        "Not logged in yet!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }

                is SDKUserResult.Success -> {
                    val sdkUser = it.sdkUser

                    val builder = AlertDialog.Builder(this)

                    // Create the EditText for the dialog.
                    val input = EditText(this).apply {
                        if (sdkUser?.username.isNullOrEmpty()) {
                            hint = "No username set yet"
                        } else {
                            hint = "Current username: ${sdkUser.username}"
                            setText(sdkUser.username!!)
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
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                return@setPositiveButton
                            } else {

                                // Step 2: Update the sdk user's username
                                LucraClient().updateUsername(sdkUser, newUsername) {
                                    when (it) {
                                        is SDKUserResult.Error -> {
                                            Log.e(
                                                "Lucra SDK Sample",
                                                "Unable to update username ${it.error}"
                                            )
                                            Toast.makeText(
                                                this@MainActivitySdk,
                                                "Unable to update username",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }

                                        SDKUserResult.InvalidUsername -> {
                                            Toast.makeText(
                                                this@MainActivitySdk,
                                                "Invalid username, try a different one",
                                                Toast.LENGTH_LONG
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
                                                "Not logged in yet!",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }

                                        is SDKUserResult.Success -> {
                                            Toast.makeText(
                                                this@MainActivitySdk,
                                                "Username updated to ${it.sdkUser.username}",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                            dialog.dismiss()
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

    private fun getInflatedLucraFragment(tag: String): Fragment? =
        supportFragmentManager.findFragmentByTag(tag)

    private fun showLucraFragment(lucraFlow: LucraUiProvider.LucraFlow) {
        fragmentContainerView.visibility = View.VISIBLE
        allButtons.forEach {
            // Material buttons float over the container for some reason even w/ elevation 0
            it.visibility = View.GONE
        }
        if (getInflatedLucraFragment(lucraFlow.toString()) != null) supportFragmentManager.beginTransaction()
            .replace(
                R.id.lucraFragment,
                LucraClient().getLucraFragment(lucraFlow),
                lucraFlow.toString()
            ).commit()
        else supportFragmentManager.beginTransaction().add(
            R.id.lucraFragment,
            LucraClient().getLucraFragment(lucraFlow),
            lucraFlow.toString()
        ).commit()
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