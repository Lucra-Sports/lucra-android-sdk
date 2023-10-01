package com.lucrasports.sdk.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.ui.LucraUi

class MainActivitySdk : AppCompatActivity(), LucraClient.LucraClientListener {

    companion object {
        private const val LUCRA_FRAGMENT_TAG = "lucraFragment"
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

    private val verifyIdentityButton: AppCompatButton by lazy {
        findViewById(R.id.verifyIdentityButton)
    }

    private val withdrawFundsButton: AppCompatButton by lazy {
        findViewById(R.id.withdrawFundsButton)
    }

    private val allButtons: List<AppCompatButton> by lazy {
        listOf(profileButton, addFundsButton, createGamesButton)
    }

    private val fragmentContainerView: FragmentContainerView
        get() = findViewById(R.id.lucraFragment)

    private var lucraDialog: DialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_sdk)

        LucraClient.initialize(
            application = application,
            lucraUiProvider = LucraUi(),
            lucraClientListener = this,
            authClientId = BuildConfig.TESTING_AUTH_ID,
            environment = Environment.STAGING,
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

        addFundsButton.setOnClickListener {
//            showLucraFragment(LucraUiProvider.LucraFlow.AddFunds)
            showLucraDialogFragment(LucraUiProvider.LucraFlow.AddFunds)
        }

        profileButton.setOnClickListener {
//            showLucraFragment(LucraUiProvider.LucraFlow.Profile)
            showLucraDialogFragment(LucraUiProvider.LucraFlow.Profile)
        }

        createGamesButton.setOnClickListener {
//            showLucraFragment(LucraUiProvider.LucraFlow.CreateGamesMatchup)
            showLucraDialogFragment(LucraUiProvider.LucraFlow.CreateGamesMatchup)
        }

        withdrawFundsButton.setOnClickListener {
//            showLucraFragment(LucraUiProvider.LucraFlow.WithdrawFunds)
            showLucraDialogFragment(LucraUiProvider.LucraFlow.WithdrawFunds)
        }

        verifyIdentityButton.setOnClickListener {
//            showLucraFragment(LucraUiProvider.LucraFlow.VerifyIdentity)
            LucraClient().checkUsersKYCStatus(
                "user-id",
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

            showLucraDialogFragment(LucraUiProvider.LucraFlow.VerifyIdentity)
        }
    }

    private fun getInflatedLucraFragment(): Fragment? =
        supportFragmentManager.findFragmentByTag(LUCRA_FRAGMENT_TAG)

    private fun showLucraFragment(lucraFlow: LucraUiProvider.LucraFlow) {
        fragmentContainerView.visibility = View.VISIBLE
        allButtons.forEach {
            // Material buttons float over the container for some reason even w/ elevation 0
            it.visibility = View.GONE
        }
        if (getInflatedLucraFragment() != null) supportFragmentManager.beginTransaction()
            .replace(
                R.id.lucraFragment, LucraClient().getLucraFragment(lucraFlow), LUCRA_FRAGMENT_TAG
            ).commit()
        else supportFragmentManager.beginTransaction().add(
            R.id.lucraFragment, LucraClient().getLucraFragment(lucraFlow), LUCRA_FRAGMENT_TAG
        ).commit()
    }

    private fun showLucraDialogFragment(lucraFlow: LucraUiProvider.LucraFlow) {
        lucraDialog = LucraClient().getLucraDialogFragment(lucraFlow)
        lucraDialog?.show(supportFragmentManager, LUCRA_FRAGMENT_TAG)
    }

    override fun onDestroy() {
        super.onDestroy()
        LucraClient.release()
    }

    override fun onLucraExit() {
        // TODO fragment setup
//        getInflatedLucraFragment()?.let {
//            supportFragmentManager.beginTransaction().remove(it).commit()
//        }
//        allButtons.forEach {
//            it.visibility = View.VISIBLE
//        }
        lucraDialog?.dismiss()
    }
}