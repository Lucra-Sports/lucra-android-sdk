package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.widget.LinearLayout

/**
 * Manages dialogs for component interactions.
 */
internal class ComponentDialogs(activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to select players for the Mini Public Feed component.
     */
    fun showMiniPublicFeedDialog(onPlayersSelected: (String, String) -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val playerOneInput = createEditText("Set player 1 ID (optional)")
        val playerTwoInput = createEditText("Set player 2 ID (optional)")

        layout.addView(playerOneInput)
        layout.addView(playerTwoInput)

        createDialogBuilder()
            .setTitle("Add player ids")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val playerOneId = playerOneInput.text.toString()
                val playerTwoId = playerTwoInput.text.toString()
                onPlayersSelected(playerOneId, playerTwoId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to enter a contest ID for the Contest Card component.
     */
    fun showContestCardDialog(onContestIdEntered: (String) -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val contestInput = createEditText(
            "Set contest ID",
            "000cfba7-17cd-4702-9d04-ed23c84a89fe"
        )

        layout.addView(contestInput)

        createDialogBuilder()
            .setTitle("Add Contest Id")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val contestId = contestInput.text.toString()
                onContestIdEntered(contestId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

