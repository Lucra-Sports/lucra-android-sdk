package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.button.MaterialButton
import com.lucrasports.sdk.app.utils.MetadataHelper
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.tournament.PoolTournament
import com.lucrasports.sdk.core.contest.GameInteractions.SearchMatchupsByMetadataResult
import com.lucrasports.sdk.core.contest.tournament.PoolTournament.SubmitTournamentScoreMatchingResult
import com.lucrasports.sdk.core.contest.tournament.PoolTournament.SubmitTournamentScoreResult
import com.lucrasports.sdk.core.ui.LucraUiProvider

/**
 * Manages dialogs related to tournaments.
 */
internal class TournamentDialogs(private val activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to submit a tournament score.
     */
    fun showSubmitScoreDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val scrollView = ScrollView(context)
        val layout = createVerticalLayout()
        scrollView.addView(layout)

        val (scoreLayout, scoreInput) = createTextInputLayout("Score", "25")
        scoreInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(scoreLayout)

        val (tournamentIdLayout, tournamentIdInput) = createTextInputLayout("Tournament ID")
        layout.addView(tournamentIdLayout)

        val isFinalCheckbox = androidx.appcompat.widget.AppCompatCheckBox(context).apply {
            text = "Is Final Attempt?"
            isChecked = false
        }
        layout.addView(isFinalCheckbox)

        val metadataLabel = TextView(context).apply {
            text = "Metadata"
            textSize = 16f
            setPadding(0, 30, 0, 10)
        }
        layout.addView(metadataLabel)

        val metadataContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(metadataContainer)

        MetadataHelper.addMetadataRow(context, metadataContainer)

        val addMetadataButton = MaterialButton(context).apply {
            text = "Add Metadata Row"
            setOnClickListener {
                MetadataHelper.addMetadataRow(context, metadataContainer)
            }
        }
        layout.addView(addMetadataButton)

        val dialog = createDialogBuilder()
            .setTitle("Submit Tournament Score")
            .setView(scrollView)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val scoreText = scoreInput.text.toString()
                val tournamentId = tournamentIdInput.text.toString()
                val isFinal = isFinalCheckbox.isChecked

                if (scoreText.isBlank() || tournamentId.isBlank()) {
                    Toast.makeText(
                        context,
                        "Score and Tournament ID are required",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val score = scoreText.toDoubleOrNull()
                if (score == null) {
                    Toast.makeText(
                        context,
                        "Invalid score value",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val metaMap = MetadataHelper.collectMetadata(metadataContainer)

                LucraClient().submitUserScore(
                    score = score,
                    tournamentId = tournamentId,
                    metadata = metaMap,
                    isFinal = isFinal,
                ) { result ->
                    activity.runOnUiThread {
                        val message = when (result) {
                            is SubmitTournamentScoreResult.SubmitTournamentsScoreOutput -> {
                                "Success!\nScore submitted to tournament: ${result.tournament.title}"
                            }
                            is SubmitTournamentScoreResult.Failure -> {
                                "Failed: ${result.failure}"
                            }
                        }
                        showMessageDialog("Submit Score Result", message)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to search matchups by metadata.
     */
    fun showSearchMatchupsByMetadataDialog() {
        val scrollView = ScrollView(context)
        val layout = createVerticalLayout()
        scrollView.addView(layout)

        val (gameIdLayout, gameIdInput) = createTextInputLayout("Game ID (optional)")
        layout.addView(gameIdLayout)

        val (locationIdLayout, locationIdInput) = createTextInputLayout("Location ID (optional)")
        layout.addView(locationIdLayout)

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID (optional)")
        layout.addView(matchupIdLayout)

        val metadataLabel = TextView(context).apply {
            text = "Matchup Metadata (optional)"
        }

        layout.addView(metadataLabel)

        val metadataContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 30, 0, 30)
        }
        layout.addView(metadataContainer)

        MetadataHelper.addMetadataRow(context, metadataContainer)

        val addMetadataButton = MaterialButton(context).apply {
            text = "Add Metadata Row"
            setOnClickListener { MetadataHelper.addMetadataRow(context, metadataContainer) }
        }
        layout.addView(addMetadataButton)

        val dialog = createDialogBuilder()
            .setTitle("Search Matchups by Metadata")
            .setView(scrollView)
            .setPositiveButton("Search", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val gameId = gameIdInput.text.toString().takeIf { it.isNotBlank() }
                val locationId = locationIdInput.text.toString().takeIf { it.isNotBlank() }
                val matchupId = matchupIdInput.text.toString().takeIf { it.isNotBlank() }

                val matchupMetadata = MetadataHelper.collectMetadata(metadataContainer)
                    .takeIf { it.isNotEmpty() }

                LucraClient().searchMatchupsByMetadata(
                    gameId = gameId,
                    locationId = locationId,
                    matchupId = matchupId,
                    matchupMetadata = matchupMetadata,
                ) { result ->
                    activity.runOnUiThread {
                        val message = when (result) {
                            is SearchMatchupsByMetadataResult.Success -> {
                                if (result.matchupIds.isEmpty()) {
                                    "No matchups found."
                                } else {
                                    "Found ${result.matchupIds.size} matchup(s):\n${result.matchupIds.joinToString("\n")}"
                                }
                            }
                            is SearchMatchupsByMetadataResult.Failure -> {
                                "Failed: ${result.failure}"
                            }
                        }
                        showMessageDialog("Search Results", message)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to submit user score by metadata matching.
     */
    fun showSubmitUserScoreByMetadataDialog() {
        val scrollView = ScrollView(context)
        val layout = createVerticalLayout()
        scrollView.addView(layout)

        val (scoreLayout, scoreInput) = createTextInputLayout("Score", "25")
        layout.addView(scoreLayout)

        val (gameIdLayout, gameIdInput) = createTextInputLayout("Game ID (optional)")
        layout.addView(gameIdLayout)

        val (locationIdLayout, locationIdInput) = createTextInputLayout("Location ID (optional)")
        layout.addView(locationIdLayout)

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID (optional)")
        layout.addView(matchupIdLayout)

        val isFinalCheckbox = AppCompatCheckBox(context).apply {
            text = "Is Final Attempt?"
            isChecked = false
        }
        layout.addView(isFinalCheckbox)

        val matchupMetadataLabel = TextView(context).apply { text = "Matchup matching Metadata (optional)" }
        layout.addView(matchupMetadataLabel)

        val matchupMetadataContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(matchupMetadataContainer)
        MetadataHelper.addMetadataRow(context, matchupMetadataContainer)

        val addMatchupMetadataButton = MaterialButton(context).apply {
            text = "Add Matchup Metadata Row"
            setOnClickListener { MetadataHelper.addMetadataRow(context, matchupMetadataContainer) }
        }
        layout.addView(addMatchupMetadataButton)

        val metadataLabel = TextView(context).apply { text = "Score Metadata (optional) for tracking purposes only" }
        layout.addView(metadataLabel)

        val metadataContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 30, 0, 30)
        }
        layout.addView(metadataContainer)
        MetadataHelper.addMetadataRow(context, metadataContainer)

        val addMetadataButton = MaterialButton(context).apply {
            text = "Add Metadata Row"
            setOnClickListener { MetadataHelper.addMetadataRow(context, metadataContainer) }
        }
        layout.addView(addMetadataButton)

        val dialog = createDialogBuilder()
            .setTitle("Submit Score by Metadata")
            .setView(scrollView)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val score = scoreInput.text.toString().toDoubleOrNull()
                if (score == null) {
                    Toast.makeText(context, "Invalid score value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val gameId = gameIdInput.text.toString().takeIf { it.isNotBlank() }
                val locationId = locationIdInput.text.toString().takeIf { it.isNotBlank() }
                val matchupId = matchupIdInput.text.toString().takeIf { it.isNotBlank() }
                val isFinal = isFinalCheckbox.isChecked

                val metadataPayload = MetadataHelper.collectMetadata(metadataContainer)
                    .takeIf { it.isNotEmpty() }
                    ?.let { meta ->
                        mutableMapOf<String, Any?>().apply { meta.forEach { (k, v) -> this[k] = v } }
                    }

                val matchupMetadataPayload = MetadataHelper.collectMetadata(matchupMetadataContainer)
                    .takeIf { it.isNotEmpty() }
                    ?.let { meta ->
                        mutableMapOf<String, Any?>().apply { meta.forEach { (k, v) -> this[k] = v } }
                    }

                LucraClient().submitUserScoreByMetadata(
                    score = score,
                    metadata = metadataPayload,
                    matchupMetadata = matchupMetadataPayload,
                    gameId = gameId,
                    locationId = locationId,
                    matchupId = matchupId,
                    isFinal = isFinal
                ) { result ->
                    activity.runOnUiThread {
                        val message = when (result) {
                            is SubmitTournamentScoreMatchingResult.SubmitTournamentScoreMatchingOutput -> {
                                buildString {
                                    append("Success!\n")
                                    if (result.affectedMatchupIds.isNotEmpty()) {
                                        append("Affected matchups: ${result.affectedMatchupIds.joinToString(", ")}\n")
                                    }
                                    if (result.failedMatchupIds.isNotEmpty()) {
                                        append("Failed matchups: ${result.failedMatchupIds.joinToString(", ")}")
                                    }
                                }
                            }
                            is SubmitTournamentScoreMatchingResult.Failure -> {
                                "Failed: ${result.failure}"
                            }
                        }
                        showMessageDialog("Submit Score Result", message)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Shows dialog to retrieve tournament details.
     */
    fun showRetrieveTournamentDialog() {
        val input = createEditText("Tournament ID", "eb77921c-aad1-4ac3-b64b-916c45c1373d")
        
        createDialogBuilder()
            .setTitle("Set Tournament Id")
            .setView(input)
            .setPositiveButton("OK") { dialog, _ ->
                val tournamentId = input.text.toString()
                LucraClient().retrieveTournament(tournamentId) {
                    activity.runOnUiThread {
                        when (it) {
                            is PoolTournament.RetrieveTournamentResult.RetrieveTournamentOutput -> {
                                displayTournamentDetails(it)
                            }
                            is PoolTournament.RetrieveTournamentResult.Failure -> {
                                showMessageDialog(
                                    "Failed to find tournament",
                                    it.failure.toString()
                                )
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Shows dialog to retrieve recommended tournaments.
     */
    fun showRecommendedTournamentsDialog() {
        LucraClient().queryRecommendedTournaments(20, 0, true) {
            activity.runOnUiThread {
                when (it) {
                    is PoolTournament.QueryRecommendedTournamentsResult.RecommendedTournamentsOutput -> {
                        displayRecommendedTournaments(it)
                    }
                    is PoolTournament.QueryRecommendedTournamentsResult.Failure -> {
                        showMessageDialog(
                            "Failed to find tournament",
                            it.failure.toString()
                        )
                    }
                }
            }
        }
    }

    /**
     * Shows dialog to join a tournament.
     */
    fun showJoinTournamentDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val input = createEditText("Tournament ID", "d0b78c81-a22b-4f54-b1fe-2fadc8354c3b")
        
        createDialogBuilder()
            .setTitle("Set Tournament Id")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val tournamentId = input.text.toString()
                LucraClient().joinTournament(tournamentId) {
                    activity.runOnUiThread {
                        handleJoinTournamentResult(it, onLaunchFlow)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun displayTournamentDetails(result: PoolTournament.RetrieveTournamentResult.RetrieveTournamentOutput) {
        val displayString = buildString {
            append("Title > ${result.tournament.title}\n")
            append("Type > ${result.tournament.type}\n")
            append("Fee > ${result.tournament.fee}\n")
            append("Buy In Amount > ${result.tournament.buyInAmount}\n")
            append("Pot Total > ${result.tournament.potTotal}\n\n")
            append("Pot Net Amount > ${result.tournament.potNetAmount}\n\n")
            append("Expires at > ${result.tournament.expiresAt}\n\n")
            append("===Participants===\n")

            result.tournament.participants.forEach { participant ->
                append("UserId: ${participant.id}\n")
                append("Username: ${participant.username}\n")
                append("Place: ${participant.place}\n")
                append("Reward Value: ${participant.rewardValue}\n\n")
            }
        }

        val textView = TextView(context).apply {
            text = displayString
            setPadding(50, 50, 50, 50)
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }

        createDialogBuilder()
            .setTitle("Tournament Results")
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun displayRecommendedTournaments(result: PoolTournament.QueryRecommendedTournamentsResult.RecommendedTournamentsOutput) {
        val displayString = buildString {
            result.recommendedTournaments.forEach { tournament ->
                append("Title: ${tournament.title}\n")
                append("Status: ${tournament.status}\n")
                append("Expires At: ${tournament.expiresAt}\n")
                append("Buy In: ${tournament.buyInAmount}\n")
                append("Pot Total: ${tournament.potTotal}\n")
                append("Pot Net Amount: ${tournament.potNetAmount}\n\n")
            }
        }

        val textView = TextView(context).apply {
            text = displayString
            setPadding(50, 50, 50, 50)
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }

        createDialogBuilder()
            .setTitle("Recommended Tournaments Result")
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun handleJoinTournamentResult(
        result: PoolTournament.JoinTournamentResult,
        onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit
    ) {
        when (result) {
            is PoolTournament.JoinTournamentResult.Success -> {
                showMessageDialog("Join Tournament Result", "Tournament Joined")
            }
            is PoolTournament.JoinTournamentResult.Failure -> {
                when (result.failure) {
                    is PoolTournament.FailedTournamentCall.UserStateError.DemographicInformationMissing -> {
                        createDialogBuilder()
                            .setTitle("Demographic Information Required")
                            .setMessage("Please complete your demographic information to join free tournaments.")
                            .setPositiveButton("Complete Demographic Form") { dialog, _ ->
                                onLaunchFlow(LucraUiProvider.LucraFlow.DemographicForm)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    is PoolTournament.FailedTournamentCall.UserStateError.Unverified -> {
                        createDialogBuilder()
                            .setTitle("Verification Required")
                            .setMessage("Please complete identity verification to join paid tournaments.")
                            .setPositiveButton("Complete Verification") { dialog, _ ->
                                onLaunchFlow(LucraUiProvider.LucraFlow.VerifyIdentity)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    else -> {
                        showMessageDialog("Failed to join tournament", result.failure.toString())
                    }
                }
            }
        }
    }
}
