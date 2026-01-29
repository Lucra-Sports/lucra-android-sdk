package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.GameInteractions.GetMatchupResult

/**
 * Handles matchup-related API interactions.
 */
internal class MatchupApiHandler(private val activity: Activity) {

    /**
     * Shows dialog to retrieve a matchup by ID.
     */
    fun showRetrieveMatchupDialog() {
        val builder = MaterialAlertDialogBuilder(activity)
        val input = EditText(activity)
        builder.setTitle("Set Matchup Id")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                retrieveMatchup(input.text.toString())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Retrieves and displays matchup details.
     */
    private fun retrieveMatchup(matchupId: String) {
        LucraClient().getMatchup(matchupId) { result ->
            activity.runOnUiThread {
                val displayString: String
                val title: String
                when (result) {
                    is GetMatchupResult.Failure -> {
                        title = "Failed to find matchup"
                        displayString = ""
                    }
                    is GetMatchupResult.Success -> {
                        title = "Matchup Results"
                        displayString = "${result.matchup}\n"
                    }
                }

                val textView = TextView(activity).apply {
                    text = displayString
                    setPadding(50, 50, 50, 50)
                }

                MaterialAlertDialogBuilder(activity)
                    .setTitle(title)
                    .setView(textView)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}

