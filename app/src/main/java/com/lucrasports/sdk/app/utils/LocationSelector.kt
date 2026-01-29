package com.lucrasports.sdk.app.utils

import android.content.Context
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView

/**
 * Helper class for creating location selection UI components.
 */
internal object LocationSelector {

    /**
     * Creates a scrollable RadioGroup with all available locations.
     * 
     * @param context The Android context
     * @param preselectedLocationId Optional location ID to pre-select
     * @return Pair of ScrollView (to add to dialog) and RadioGroup (to get selected value)
     */
    fun createLocationSelector(
        context: Context,
        preselectedLocationId: String? = null
    ): Pair<ScrollView, RadioGroup> {
        val scrollView = ScrollView(context)
        val radioGroup = RadioGroup(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // Add radio buttons for each location
        LocationData.ALL_LOCATIONS.forEach { location ->
            val radioButton = RadioButton(context).apply {
                text = location.getFormattedName()
                tag = location.id
                setPadding(10, 20, 10, 20)
                
                // Pre-select if this is the specified location
                if (location.id == preselectedLocationId) {
                    isChecked = true
                }
            }
            radioGroup.addView(radioButton)
        }

        scrollView.addView(radioGroup)
        return Pair(scrollView, radioGroup)
    }

    /**
     * Gets the selected location ID from a RadioGroup.
     * 
     * @param radioGroup The RadioGroup containing location radio buttons
     * @return The selected location ID, or null if nothing is selected
     */
    fun getSelectedLocationId(radioGroup: RadioGroup): String? {
        val selectedId = radioGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            radioGroup.findViewById<RadioButton>(selectedId)?.tag as? String
        } else {
            null
        }
    }
}
