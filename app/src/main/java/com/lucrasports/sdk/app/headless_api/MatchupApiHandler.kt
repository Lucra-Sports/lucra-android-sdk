package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.GameInteractions.GetMatchupDetailsResult

/**
 * Handles matchup-related API interactions.
 */
internal class MatchupApiHandler(private val activity: Activity) {

    /**
     * Shows dialog to retrieve a matchup by ID (one-shot query).
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
     * Shows dialog to subscribe to live matchup updates by ID.
     */
    fun showSubscribeMatchupDialog() {
        val builder = MaterialAlertDialogBuilder(activity)
        val input = EditText(activity)
        builder.setTitle("Subscribe to Matchup Id")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                subscribeToMatchup(input.text.toString())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Retrieves and displays matchup details (one-shot).
     */
    private fun retrieveMatchup(matchupId: String) {
        LucraClient().getMatchupDetails(matchupId) { result ->
            activity.runOnUiThread {
                val displayString: String
                val title: String
                when (result) {
                    is GetMatchupDetailsResult.Failure -> {
                        title = "Failed to find matchup"
                        displayString = "${result.error}"
                    }
                    is GetMatchupDetailsResult.Success -> {
                        title = "Matchup Results"
                        displayString = "${result.details}\n"
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

    /**
     * Cancels any active matchup subscription.
     */
    fun cancelMatchupDetailsSubscription() {
        LucraClient().cancelMatchupSubscription()
    }

    /**
     * Subscribes to live matchup updates and displays each update.
     */
    private fun subscribeToMatchup(matchupId: String) {
        LucraClient().getMatchupDetails(matchupId, subscribe = true) { result ->
            activity.runOnUiThread {
                val displayString: String
                val title: String
                when (result) {
                    is GetMatchupDetailsResult.Failure -> {
                        title = "Subscription Error"
                        displayString = "${result.error}"
                    }
                    is GetMatchupDetailsResult.Success -> {
                        val details = result.details
                        title = "Matchup Update"
                        displayString = "Status: ${details.matchup.status}\n" +
                            "Groups: ${details.groups.size}\n" +
                            "Ranking rows: ${details.participantScores.size}\n" +
                            "ID: ${details.matchup.id}"
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
