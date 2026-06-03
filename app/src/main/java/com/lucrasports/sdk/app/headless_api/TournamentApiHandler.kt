package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.tournament.PoolTournament.AutoJoinTournamentsResult

/**
 * Handles tournament-related headless API interactions.
 */
internal class TournamentApiHandler(private val activity: Activity) {

    /**
     * Manually triggers auto-join for all eligible free tournaments
     * and displays the result in a dialog.
     */
    fun autoJoinTournaments() {
        LucraClient().autoJoinTournaments { result ->
            activity.runOnUiThread {
                val title: String
                val displayString: String
                when (result) {
                    is AutoJoinTournamentsResult.AutoJoinedTournamentsOutput -> {
                        title = "Auto-Join Result"
                        displayString = if (result.tournamentIds.isEmpty()) {
                            "No tournaments to auto-join."
                        } else {
                            "Joined ${result.tournamentIds.size} tournament(s):\n${result.tournamentIds.joinToString("\n")}"
                        }
                    }
                    is AutoJoinTournamentsResult.Failure -> {
                        title = "Auto-Join Failed"
                        displayString = "${result.failure}"
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
