package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.ui.theming.SampleColorStore
import com.lucrasports.sdk.app.ui.theming.ThemeManager
import com.lucrasports.sdk.core.reward.LucraReward

/**
 * Manages configuration-related dialogs.
 */
internal class ConfigDialogs(private val activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to override API key.
     */
    fun showOverrideApiKeyDialog(
        currentApiKeyOverride: String?,
        buildApiKeySet: Boolean,
        onSave: (String?) -> Unit,
        onReset: () -> Unit
    ) {
        val layout = layoutInflater.inflate(R.layout.main_option_configure_api, null)
        val apiKeyEt = layout.findViewById<TextInputEditText>(R.id.et_api_key)
        val apiKeyTil = layout.findViewById<TextInputLayout>(R.id.til_api_key)
        
        currentApiKeyOverride?.let {
            apiKeyTil.hint = "API Key (overrode)"
            apiKeyEt.setText(it)
        } ?: run {
            apiKeyEt.setText(
                if (buildApiKeySet) {
                    apiKeyTil.hint = "API Key (default)"
                    BuildConfig.TESTING_API_KEY
                } else {
                    "Add an API Key!"
                }
            )
        }

        val configureApiDialog = createDialogBuilder()
            .setTitle("Update Api Url ")
            .setView(layout)
            .setNeutralButton("Reset") { dialog, _ ->
                onReset()
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .setPositiveButton("Save", null)
            .create()

        configureApiDialog.setOnShowListener {
            val positiveButton = configureApiDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val enteredApiKey = apiKeyEt.text.toString()
                if (enteredApiKey.isEmpty()) {
                    apiKeyEt.error = "Cannot be blank"
                    return@setOnClickListener
                }

                val newApiKeyOverride = if (enteredApiKey != BuildConfig.TESTING_API_KEY) {
                    enteredApiKey
                } else {
                    null
                }

                onSave(newApiKeyOverride)
                configureApiDialog.dismiss()
            }
        }
        configureApiDialog.show()
    }

    fun showWalletWebAppUrlDialog(
        currentUrl: String?,
        onSave: (String?) -> Unit
    ) {
        val layout = layoutInflater.inflate(R.layout.main_option_setting_edit_text, null)
        val textInputLayout = layout.findViewById<TextInputLayout>(R.id.option_setting_edit_text_layout)
        textInputLayout.hint = "Wallet web app URL"
        val editText = layout.findViewById<EditText>(R.id.option_setting_edit_text)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        editText.setText(currentUrl.orEmpty())
        layout.findViewById<android.widget.TextView>(R.id.option_setting_edit_text_supporting_text).text =
            "Non-release builds only, and only while the release_wallet_web_app gate is on: " +
                "the wallet flows load this URL instead of asking the backend for one."

        createDialogBuilder()
            .setTitle("Wallet Web App URL Override")
            .setView(layout)
            .setNeutralButton("Clear") { dialog, _ ->
                onSave(null)
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .setPositiveButton("Save") { dialog, _ ->
                onSave(editText.text.toString().trim().takeIf { it.isNotEmpty() })
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to view SDK configuration.
     */
    fun showViewConfigurationDialog(configurationHtml: String?) {
        createDialogBuilder()
            .setTitle("Configuration")
            .setMessage(
                android.text.Html.fromHtml(
                    configurationHtml ?: "Not initialized yet...",
                    android.text.Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setPositiveButton("Dismiss") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Shows dialog to set league filter.
     */
    fun showLeagueFilterDialog(
        currentFilters: Set<String>,
        onAddFilter: (String) -> Unit,
        onClearFilters: () -> Unit
    ) {
        val editText = layoutInflater.inflate(R.layout.main_option_setting_edit_text, null)
        val textInputLayout = editText.findViewById<TextInputLayout>(R.id.option_setting_edit_text_layout)
        textInputLayout.hint = "Add a league filter id"
        
        if (currentFilters.isNotEmpty()) {
            editText.findViewById<android.widget.TextView>(R.id.option_setting_edit_text_supporting_text).text =
                "Active ids: " + currentFilters.joinToString(",\n")
        }

        createDialogBuilder()
            .setTitle("Add League Filter ID")
            .setView(editText)
            .setNeutralButton("Clear Filter(s)") { dialog, _ ->
                onClearFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .setPositiveButton("Add ID") { dialog, _ ->
                val id = editText.findViewById<EditText>(R.id.option_setting_edit_text).text.toString()
                onAddFilter(id)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog for configuring SDK theming.
     */
    fun showThemingDialog(
        themeManager: ThemeManager,
        onApply: () -> Unit
    ) {
        val colorLayout = layoutInflater.inflate(R.layout.main_theming_options_layout, null)
        val themingOptionsSection = colorLayout.findViewById<LinearLayout>(R.id.ll_theming_options_section)
        val btnThemeDefault: Button = colorLayout.findViewById(R.id.btn_theme_default)
        val btnThemeDandb: Button = colorLayout.findViewById(R.id.btn_theme_dandb)
        val btnThemeDupr: Button = colorLayout.findViewById(R.id.btn_theme_dupr)
        val btnThemeChaos: Button = colorLayout.findViewById(R.id.btn_theme_chaos)
        val btnThemePsf: Button = colorLayout.findViewById(R.id.btn_theme_psf)
        val btnThemeT1: Button = colorLayout.findViewById(R.id.btn_theme_t1)
        val btnThemeTrackman: Button = colorLayout.findViewById(R.id.btn_theme_trackman)

        btnThemeDefault.setOnClickListener {
            SampleColorStore.applyTheme(
                SampleColorStore.defaultLightModeTheme,
                SampleColorStore.defaultDarkModeTheme
            )
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemeDandb.setOnClickListener {
            SampleColorStore.applyTheme(
                SampleColorStore.dandbLightTheme,
                SampleColorStore.dandbDarkTheme
            )
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemeDupr.setOnClickListener {
            SampleColorStore.applyTheme(SampleColorStore.duprTheme)
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemeChaos.setOnClickListener {
            SampleColorStore.applyTheme(SampleColorStore.chaosTheme)
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemeT1.setOnClickListener {
            SampleColorStore.applyTheme(SampleColorStore.t1Theme)
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemePsf.setOnClickListener {
            SampleColorStore.applyTheme(SampleColorStore.psfTheme)
            themeManager.resetThemingOptions(themingOptionsSection)
        }
        btnThemeTrackman.setOnClickListener {
            SampleColorStore.applyTheme(SampleColorStore.trackmanTheme)
            themeManager.resetThemingOptions(themingOptionsSection)
        }

        themeManager.appendThemingOptions(themingOptionsSection)
        createDialogBuilder()
            .setTitle("Lucra Theming")
            .setView(colorLayout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { dialog, _ ->
                dialog.dismiss()
                onApply()
            }
            .show()
    }

    /**
     * Shows dialog for configuring reward provider.
     */
    fun showRewardProviderDialog(
        currentRewards: List<LucraReward>,
        enabled: Boolean,
        onUpdate: (Boolean, List<LucraReward>) -> Unit
    ) {
        val rewardProviderLayout = layoutInflater.inflate(R.layout.main_option_reward_provider, null)
        val lucraReward = currentRewards.first()
        val etRewardTitle = rewardProviderLayout.findViewById<TextInputEditText>(R.id.et_reward_title).apply {
            setText(lucraReward.title)
        }
        val etRewardDescriptor = rewardProviderLayout.findViewById<TextInputEditText>(R.id.et_reward_descriptor).apply {
            setText(lucraReward.descriptor)
        }
        val etRewardIconUrl = rewardProviderLayout.findViewById<TextInputEditText>(R.id.et_reward_icon_url).apply {
            setText(lucraReward.iconUrl)
        }
        val etRewardBannerUrl = rewardProviderLayout.findViewById<TextInputEditText>(R.id.et_reward_banner_url).apply {
            setText(lucraReward.bannerIconUrl)
        }
        val etRewardDisclaimer = rewardProviderLayout.findViewById<TextInputEditText>(R.id.et_reward_disclaimer).apply {
            setText(lucraReward.disclaimer)
        }
        val rewardEnabledSwitch = rewardProviderLayout.findViewById<SwitchMaterial>(R.id.enable_provider).apply {
            isChecked = enabled
        }

        createDialogBuilder()
            .setTitle("Lucra Reward Provider")
            .setView(rewardProviderLayout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { dialog, _ ->
                dialog.dismiss()
                val updatedRewards = listOf(
                    lucraReward.copy(
                        title = etRewardTitle.text.toString(),
                        descriptor = etRewardDescriptor.text.toString(),
                        iconUrl = etRewardIconUrl.text.toString(),
                        bannerIconUrl = etRewardBannerUrl.text.toString(),
                        disclaimer = etRewardDisclaimer.text.toString()
                    )
                ) + currentRewards
                onUpdate(rewardEnabledSwitch.isChecked, updatedRewards)
            }
            .show()
    }
}

