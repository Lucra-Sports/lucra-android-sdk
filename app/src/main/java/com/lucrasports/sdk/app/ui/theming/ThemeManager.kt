package com.lucrasports.sdk.app.ui.theming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.ui.theming.SampleColorStore.intToColorHex

/**
 * Manages theme-related UI and color picker operations.
 */
internal class ThemeManager(private val activity: FragmentActivity) {

    private val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(activity)

    private val themeOptionRowViewMap = mutableMapOf<Int, View>()

    /**
     * Resets theming options by clearing and re-appending them.
     */
    fun resetThemingOptions(root: ViewGroup) {
        root.removeAllViews()
        appendThemingOptions(root)
    }

    /**
     * Appends theming options to the root view group.
     */
    fun appendThemingOptions(root: ViewGroup) {
        // Light theme header
        layoutInflater.inflate(R.layout.theme_color_selector, root, false)
            .apply {
                findViewById<TextView>(R.id.colorDescriptorTv).text = "LightTheme options"
                findViewById<TextView>(R.id.colorHexTv).visibility = View.GONE
                findViewById<View>(R.id.colorPreview).visibility = View.GONE
            }
            .also { root.addView(it) }

        // Light theme options
        SampleColorStore.getColorIdHexIntForAllLightModeProperties { id, title, colorHex, colorInt ->
            appendThemingOption(
                title = title,
                colorHex = colorHex,
                id = id,
                defaultColor = colorInt,
                root = root
            ).also { themeOptionRowViewMap[id] = it }
        }

        // Dark theme header
        layoutInflater.inflate(R.layout.theme_color_selector, root, false)
            .apply {
                findViewById<TextView>(R.id.colorDescriptorTv).text = "DarkTheme options"
                findViewById<TextView>(R.id.colorHexTv).visibility = View.GONE
                findViewById<View>(R.id.colorPreview).visibility = View.GONE
            }
            .also { root.addView(it) }

        // Dark theme options
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

    /**
     * Appends a single theming option row.
     */
    private fun appendThemingOption(
        title: String,
        colorHex: String,
        id: Int,
        defaultColor: Int,
        root: ViewGroup,
    ): View {
        val optionView = layoutInflater.inflate(R.layout.theme_color_selector, root, false).apply {
            findViewById<TextView>(R.id.colorDescriptorTv).text = title
            findViewById<TextView>(R.id.colorHexTv).text = colorHex
            findViewById<View>(R.id.colorPreview).setBackgroundColor(defaultColor)
            setOnClickListener {
                showColorPickerDialog(id = id, defaultColor = defaultColor)
            }
        }

        root.addView(optionView)
        return optionView
    }

    /**
     * Shows color picker dialog.
     */
    private fun showColorPickerDialog(id: Int, defaultColor: Int) {
        ColorPickerDialog.newBuilder()
            .setDialogId(id)
            .setColor(defaultColor)
            .show(activity)
    }

    /**
     * Handles color selection from color picker.
     */
    fun onColorSelected(dialogId: Int, color: Int) {
        SampleColorStore.ColorIdMap.updateColorBasedOnId(dialogId, color)

        themeOptionRowViewMap[dialogId]?.run {
            findViewById<TextView>(R.id.colorHexTv).text = color.intToColorHex()
            findViewById<View>(R.id.colorPreview).setBackgroundColor(color)
        }
    }
}
