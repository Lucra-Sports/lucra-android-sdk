package com.lucrasports.sdk.app.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lucrasports.sdk.app.R

/** [restarts] marks a row whose change only takes effect once the SDK is reinitialised. */
internal sealed interface SettingsRow {

    data class Group(val title: String) : SettingsRow

    data class Action(
        val title: String,
        val value: String? = null,
        val restarts: Boolean = false,
        val destructive: Boolean = false,
        val onClick: () -> Unit,
    ) : SettingsRow

    data class Toggle(
        val title: String,
        val checked: Boolean,
        val restarts: Boolean = false,
        val onChange: (Boolean) -> Unit,
    ) : SettingsRow

    data class Info(val title: String, val value: String) : SettingsRow
}

/** Rows are a lambda, not a list, so the sheet rebuilds in place after a toggle. */
internal class SampleSettingsSheet(private val activity: Activity) {

    fun show(rows: () -> List<SettingsRow>) {
        val dialog = BottomSheetDialog(activity)
        val content = LayoutInflater.from(activity).inflate(R.layout.sheet_settings, null)
        dialog.setContentView(content)
        val container = content.findViewById<LinearLayout>(R.id.settings_rows)

        fun render() {
            container.removeAllViews()
            rows().forEach { row -> container.addView(view(row, container, dialog, ::render)) }
        }
        render()
        dialog.show()
    }

    private fun view(
        row: SettingsRow,
        parent: LinearLayout,
        dialog: BottomSheetDialog,
        rerender: () -> Unit,
    ): View {
        val inflater = LayoutInflater.from(activity)
        if (row is SettingsRow.Group) {
            return inflater.inflate(R.layout.item_settings_group, parent, false).apply {
                findViewById<TextView>(R.id.settings_group_title).text = row.title
            }
        }

        val itemView = inflater.inflate(R.layout.item_settings_row, parent, false)
        val title = itemView.findViewById<TextView>(R.id.settings_row_title)
        val restarts = itemView.findViewById<TextView>(R.id.settings_row_restarts)
        val value = itemView.findViewById<TextView>(R.id.settings_row_value)
        val toggle = itemView.findViewById<SwitchCompat>(R.id.settings_row_switch)
        val chevron = itemView.findViewById<ImageView>(R.id.settings_row_chevron)

        fun markRestarts(shows: Boolean) {
            restarts.isVisible = shows
            if (!shows) return
            restarts.setTextColor(ContextCompat.getColor(activity, R.color.badge_api_text))
            restarts.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.badge_api_bg))
        }

        when (row) {
            is SettingsRow.Group -> Unit

            is SettingsRow.Action -> {
                title.text = row.title
                if (row.destructive) {
                    title.setTextColor(ContextCompat.getColor(activity, R.color.warning))
                }
                markRestarts(row.restarts)
                value.text = row.value.orEmpty()
                value.isVisible = !row.value.isNullOrEmpty()
                chevron.isVisible = row.value.isNullOrEmpty()
                itemView.setOnClickListener {
                    dialog.dismiss()
                    row.onClick()
                }
            }

            is SettingsRow.Toggle -> {
                title.text = row.title
                markRestarts(row.restarts)
                value.isVisible = false
                toggle.isVisible = true
                toggle.isChecked = row.checked
                val flip = {
                    row.onChange(!row.checked)
                    rerender()
                }
                toggle.setOnClickListener { flip() }
                itemView.setOnClickListener { flip() }
            }

            is SettingsRow.Info -> {
                title.text = row.title
                title.setTextColor(ContextCompat.getColor(activity, R.color.text_muted))
                title.textSize = 13f
                value.text = row.value
                value.setTextColor(ContextCompat.getColor(activity, R.color.text_faint))
                itemView.isClickable = false
                itemView.background = null
            }
        }
        return itemView
    }
}
