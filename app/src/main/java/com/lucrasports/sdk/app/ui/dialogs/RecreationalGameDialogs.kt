package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.lucrasports.sdk.app.fake_resources.fakeLucraRewards
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.recreational.RecreationalGameInteractions
import com.lucrasports.sdk.core.contest.recreational.RecreationalGameInteractions.AcceptRecreationalGameResult
import com.lucrasports.sdk.core.contest.recreational.RecreationalGameInteractions.CancelGamesMatchupResult
import com.lucrasports.sdk.core.contest.recreational.RecreationalGameInteractions.CreateGamesMatchupResult

/**
 * Manages dialogs related to recreational games.
 */
internal class RecreationalGameDialogs(private val activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to create a recreational game.
     */
    fun showCreateGameDialog() {
        val layout = createVerticalLayout()

        // Game Type ID
        val (gameTypeIdLayout, gameTypeIdInput) = createTextInputLayout("Game Type ID", "CORNHOLE")
        layout.addView(gameTypeIdLayout)

        // PlayStyle selection spinner
        val playStyleSpinner = createSpinner(arrayOf("GROUP_VS_GROUP", "FREE_FOR_ALL"))
        val playStyleContainer = LinearLayout(context).apply {
            addView(TextView(context).apply {
                text = "Play Style:"
                setPadding(0, 30, 20, 0)
            })
            addView(playStyleSpinner)
            setPadding(0, 20, 0, 20)
        }
        layout.addView(playStyleContainer)

        // Reward Type selection spinner
        val rewardTypeSpinner = createSpinner(arrayOf("CASH", "FREE"))
        val rewardTypeContainer = LinearLayout(context).apply {
            addView(TextView(context).apply {
                text = "Reward Type:"
                setPadding(0, 30, 20, 0)
            })
            addView(rewardTypeSpinner)
            setPadding(0, 20, 0, 20)
        }
        layout.addView(rewardTypeContainer)

        val dialog = createDialogBuilder()
            .setTitle("Create Recreational Game")
            .setView(layout)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val gameTypeId = gameTypeIdInput.text.toString()
                val playStyleValue = when (playStyleSpinner.selectedItem.toString()) {
                    "GROUP_VS_GROUP" -> RecreationalGameInteractions.PlayStyle.GroupVsGroup
                    "FREE_FOR_ALL" -> RecreationalGameInteractions.PlayStyle.FreeForAll
                    else -> RecreationalGameInteractions.PlayStyle.GroupVsGroup
                }
                val rewardTypeValue = when (rewardTypeSpinner.selectedItem.toString()) {
                    "CASH" -> RecreationalGameInteractions.RewardType.Cash(5.00)
                    else -> fakeLucraRewards.first().run {
                        RecreationalGameInteractions.RewardType.TenantReward(
                            rewardId = rewardId,
                            title = title,
                            descriptor = descriptor,
                            iconUrl = iconUrl,
                            bannerIconUrl = bannerIconUrl,
                            disclaimer = disclaimer,
                            metadata = metadata
                        )
                    }
                }

                if (gameTypeId.isBlank()) {
                    Toast.makeText(context, "Game Type ID is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                LucraClient().createRecreationalGame(
                    gameTypeId = gameTypeId,
                    atStake = rewardTypeValue,
                    playStyle = playStyleValue
                ) { result ->
                    activity.runOnUiThread {
                        displayCreateGameResult("Create Game Result", result)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to accept a versus recreational game.
     */
    fun showAcceptVersusGameDialog() {
        val layout = createVerticalLayout()

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID")
        layout.addView(matchupIdLayout)

        val (teamIdLayout, teamIdInput) = createTextInputLayout("Team ID")
        layout.addView(teamIdLayout)

        val dialog = createDialogBuilder()
            .setTitle("Accept Versus Recreational Game")
            .setView(layout)
            .setPositiveButton("Accept", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val matchupId = matchupIdInput.text.toString()
                val teamId = teamIdInput.text.toString()

                if (matchupId.isBlank() || teamId.isBlank()) {
                    Toast.makeText(
                        context,
                        "Both Matchup ID and Team ID are required",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                LucraClient().acceptVersusRecreationalGame(
                    matchupId = matchupId,
                    teamId = teamId
                ) { result ->
                    activity.runOnUiThread {
                        displayAcceptGameResult("Accept Versus Game Result", result)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to accept a free-for-all recreational game.
     */
    fun showAcceptFreeForAllGameDialog() {
        val layout = createVerticalLayout()

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID")
        layout.addView(matchupIdLayout)

        val dialog = createDialogBuilder()
            .setTitle("Accept Free-For-All Recreational Game")
            .setView(layout)
            .setPositiveButton("Accept", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val matchupId = matchupIdInput.text.toString()

                if (matchupId.isBlank()) {
                    Toast.makeText(context, "Matchup ID is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                LucraClient().acceptFreeForAllRecreationalGame(
                    matchupId = matchupId
                ) { result ->
                    activity.runOnUiThread {
                        displayAcceptGameResult("Accept Free-For-All Game Result", result)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to cancel a recreational game.
     */
    fun showCancelGameDialog() {
        val layout = createVerticalLayout()

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID")
        layout.addView(matchupIdLayout)

        val dialog = createDialogBuilder()
            .setTitle("Cancel Recreational Game")
            .setView(layout)
            .setPositiveButton("Cancel Game", null)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val matchupId = matchupIdInput.text.toString()

                if (matchupId.isBlank()) {
                    Toast.makeText(context, "Matchup ID is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                LucraClient().cancelRecreationalGame(
                    matchupId = matchupId
                ) { result ->
                    activity.runOnUiThread {
                        displayCancelGameResult("Cancel Game Result", result)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun displayCreateGameResult(title: String, result: CreateGamesMatchupResult) {
        val message = when (result) {
            is CreateGamesMatchupResult.Success -> {
                "Success!\nMatchup ID: ${result.matchupId}"
            }
            is CreateGamesMatchupResult.Failure -> {
                "Failed: ${result.failure}"
            }
        }
        showMessageDialog(title, message)
    }

    private fun displayAcceptGameResult(title: String, result: AcceptRecreationalGameResult) {
        val message = when (result) {
            is AcceptRecreationalGameResult.Success -> {
                "Success!\nMatchup accepted successfully."
            }
            is AcceptRecreationalGameResult.Failure -> {
                "Failed: ${result.failure}"
            }
        }
        showMessageDialog(title, message)
    }

    private fun displayCancelGameResult(title: String, result: CancelGamesMatchupResult) {
        val message = when (result) {
            is CancelGamesMatchupResult.Success -> {
                "Success!\nMatchup cancelled successfully."
            }
            is CancelGamesMatchupResult.Failure -> {
                "Failed: ${result.failure}"
            }
        }
        showMessageDialog(title, message)
    }
}

