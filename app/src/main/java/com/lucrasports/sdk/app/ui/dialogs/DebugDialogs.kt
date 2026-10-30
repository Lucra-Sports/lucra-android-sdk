package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import com.lucrasports.sdk.app.notifications.DEBUG_PUSH_NOTIFICATIONS
import com.lucrasports.sdk.app.notifications.scheduleDebugNotification

/** Hidden in release builds by the catalog entry's `debugOnly` flag. */
internal class DebugDialogs(activity: Activity) : DialogManager(activity) {

    fun showPushDebugPicker() {
        val density = context.resources.displayMetrics.density
        val horizontalPad = (24 * density).toInt()
        val verticalPad = (8 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
        }

        container.addView(
            TextView(context).apply {
                text =
                    "Tap a notification to schedule it. The banner fires after 2 seconds — tap it to validate in-app handling."
                textSize = 13f
                setPadding(0, 0, 0, (12 * density).toInt())
            }
        )

        val dialog = createDialogBuilder()
            .setTitle("Push Debug")
            .setView(container)
            .setNegativeButton("Done", null)
            .create()

        DEBUG_PUSH_NOTIFICATIONS.forEach { notification ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
                setOnClickListener {
                    dialog.dismiss()
                    context.scheduleDebugNotification(notification)
                }
            }
            row.addView(
                TextView(context).apply {
                    text = notification.label
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            row.addView(
                TextView(context).apply {
                    text = notification.body
                    textSize = 13f
                }
            )
            container.addView(row)
        }

        dialog.show()
    }
}
