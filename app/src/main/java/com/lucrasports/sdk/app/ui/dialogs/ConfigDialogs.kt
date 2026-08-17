package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.ui.theming.SampleColorStore
import com.lucrasports.sdk.app.ui.theming.ThemeManager
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.convert_credit.LucraConvertToCreditProvider
import com.lucrasports.sdk.core.convert_credit.LucraConvertToCreditWithdrawMethod
import com.lucrasports.sdk.core.convert_credit.LucraWithdrawCardTheme
import com.lucrasports.sdk.core.reward.LucraReward
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID

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
     * Shows a simple not implemented message.
     */
    fun showNotImplementedDialog(feature: String) {
        showMessageDialog(feature, "This feature is not yet implemented")
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

    /**
     * Shows dialog for configuring convert-to-credit provider.
     */
    fun showConvertToCreditDialog(onUpdate: (LucraConvertToCreditProvider?) -> Unit) {
        val convertToCredit = layoutInflater.inflate(R.layout.main_option_convert_to_credit, null)

        val onOffSwitch = convertToCredit.findViewById<SwitchCompat>(R.id.csc_enabled)
        onOffSwitch.isChecked = LucraClient().isConvertToCreditAvailable()

        val idEditText = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_id)
        idEditText.setText(UUID.randomUUID().toString())
        val convertedAmountEditText = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_converted_amount)
        convertedAmountEditText.setText("10.00")
        val convertedAmountDisplayEditText = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_converted_amount_display)
        convertedAmountDisplayEditText.setText("$10.00 Credits")
        val iconUrl = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_icon_url)
        iconUrl.setText("https://lucrasports.com/images/homepage/lucra-l.svg")
        val metaData = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_meta_data)
        val longDescription = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_long_description)
        longDescription.setText("This is a really really really really really really long description")
        val shortDescription = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_short_description)
        shortDescription.setText("Short description")
        val title = convertToCredit.findViewById<TextInputEditText>(R.id.et_c2c_title)
        title.setText("Awesome Credits")

        val spinner: Spinner = convertToCredit.findViewById(R.id.meta_spinner)
        val items = arrayOf(
            "{\"showError\":\"blank\"}",
            "{\"showError\":\"extraShort\"}",
            "{\"showError\":\"short\"}",
            "{\"showError\":\"medium\"}",
            "{\"showError\":\"long\"}",
            "{\"showError\":\"extraLong\"}",
            "{\"showError\":\"superLong\"}",
            "{\"showSuccess\":\"blank\"}",
            "{\"showSuccess\":\"extraShort\"}",
            "{\"showSuccess\":\"short\"}",
            "{\"showSuccess\":\"medium\"}",
            "{\"showSuccess\":\"long\"}",
            "{\"showSuccess\":\"extraLong\"}",
            "{\"showSuccess\":\"superLong\"}"
        )
        val adapter = ArrayAdapter(activity, R.layout.meta_dropdown_list_item, items)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                metaData.setText(items[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        createDialogBuilder()
            .setTitle("Convert to Credit Options")
            .setView(convertToCredit)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { dialog, _ ->
                if (onOffSwitch.isChecked) {
                    val metaMap = try {
                        val jsonObject = JSONObject(metaData.text.toString())
                        mutableMapOf<String, Any>().apply {
                            for (key in jsonObject.keys()) {
                                this[key] = jsonObject.get(key)
                            }
                        }.mapValues { it.value.toString() }
                    } catch (e: Exception) {
                        null
                    }

                    onUpdate(object : LucraConvertToCreditProvider {
                        override suspend fun getCreditAmount(cashAmount: Double): LucraConvertToCreditWithdrawMethod? {
                            delay(2000L)
                            return LucraConvertToCreditWithdrawMethod(
                                id = idEditText.text.toString(),
                                conversionTerms = "No Fee  |  Instant transfer",
                                title = title.text.toString(),
                                amount = cashAmount,
                                convertedAmount = convertedAmountEditText.text.toString().toDouble(),
                                convertedAmountDisplay = convertedAmountDisplayEditText.text.toString(),
                                shortDescription = shortDescription.text.toString(),
                                longDescription = longDescription.text.toString(),
                                iconUrl = if (iconUrl.text.isNullOrBlank()) null else iconUrl.text.toString(),
                                metaData = metaMap,
                                theme = LucraWithdrawCardTheme(
                                    cardColor = "#5A1668",
                                    cardTextColor = "#FFFFFF",
                                    pillColor = "#5A1668",
                                    pillTextColor = "#FFFFFF",
                                )
                            )
                        }
                    })
                } else {
                    onUpdate(null)
                }
                dialog.dismiss()
            }
            .show()
    }
}

