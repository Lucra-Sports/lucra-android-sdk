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
import com.lucrasports.sdk.core.contest.GameInteractions.SearchMatchupsByMetadataResult
import com.lucrasports.sdk.core.contest.tournament.PoolTournament
import com.lucrasports.sdk.core.contest.tournament.PoolTournament.SubmitTournamentScoreMatchingResult
import com.lucrasports.sdk.core.contest.tournament.PoolTournament.SubmitTournamentScoreResult
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.tournament.LucraTournamentDetails

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

        val (tournamentIdLayout, tournamentIdInput) = createTextInputLayout("Tournament ID", recallKey = RecallKey.TOURNAMENT_ID)
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

        val (gameIdLayout, gameIdInput) = createTextInputLayout("Game ID (optional)", recallKey = RecallKey.GAME_ID)
        layout.addView(gameIdLayout)

        val (locationIdLayout, locationIdInput) = createTextInputLayout("Location ID (optional)", recallKey = RecallKey.LOCATION_ID)
        layout.addView(locationIdLayout)

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID (optional)", recallKey = RecallKey.MATCHUP_ID)
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

        val (gameIdLayout, gameIdInput) = createTextInputLayout("Game ID (optional)", recallKey = RecallKey.GAME_ID)
        layout.addView(gameIdLayout)

        val (locationIdLayout, locationIdInput) = createTextInputLayout("Location ID (optional)", recallKey = RecallKey.LOCATION_ID)
        layout.addView(locationIdLayout)

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout("Matchup ID (optional)", recallKey = RecallKey.MATCHUP_ID)
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
     * Shows dialog to retrieve a tournament's details.
     */
    fun showRetrieveTournamentDetailsDialog() {
        val input = createRecallField("Tournament ID", "eb77921c-aad1-4ac3-b64b-916c45c1373d", RecallKey.TOURNAMENT_ID)

        createDialogBuilder()
            .setTitle("Retrieve Tournament Details")
            .setView(input)
            .setPositiveButton("Retrieve") { _, _ ->
                val tournamentId = input.text.toString()
                if (tournamentId.isBlank()) {
                    Toast.makeText(context, "Tournament ID is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                LucraClient().retrieveTournamentDetails(tournamentId = tournamentId) { result ->
                    activity.runOnUiThread {
                        when (result) {
                            is PoolTournament.RetrieveTournamentDetailsResult.TournamentDetailsOutput -> {
                                displayTournamentDetails(result.details)
                            }
                            is PoolTournament.RetrieveTournamentDetailsResult.Failure -> {
                                showMessageDialog(
                                    "Failed to find tournament details",
                                    result.failure.toString()
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
     * Shows dialog to retrieve a page of a tournament's leaderboard.
     */
    fun showRetrieveTournamentLeaderboardDialog() {
        val scrollView = ScrollView(context)
        val layout = createVerticalLayout()
        scrollView.addView(layout)

        val (tournamentIdLayout, tournamentIdInput) =
            createTextInputLayout("Tournament ID", "eb77921c-aad1-4ac3-b64b-916c45c1373d", RecallKey.TOURNAMENT_ID)
        layout.addView(tournamentIdLayout)

        val (limitLayout, limitInput) = createTextInputLayout("Limit", "10")
        limitInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(limitLayout)

        val (offsetLayout, offsetInput) = createTextInputLayout("Offset", "0")
        offsetInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(offsetLayout)

        createDialogBuilder()
            .setTitle("Retrieve Tournament Leaderboard")
            .setView(scrollView)
            .setPositiveButton("Retrieve") { _, _ ->
                val tournamentId = tournamentIdInput.text.toString()
                if (tournamentId.isBlank()) {
                    Toast.makeText(context, "Tournament ID is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                LucraClient().retrieveTournamentLeaderboard(
                    tournamentId = tournamentId,
                    limit = limitInput.text.toString().toIntOrNull() ?: 10,
                    offset = offsetInput.text.toString().toIntOrNull() ?: 0,
                ) { result ->
                    activity.runOnUiThread {
                        when (result) {
                            is PoolTournament.RetrieveTournamentLeaderboardResult.TournamentLeaderboardOutput -> {
                                displayTournamentLeaderboard(result)
                            }
                            is PoolTournament.RetrieveTournamentLeaderboardResult.Failure -> {
                                showMessageDialog(
                                    "Failed to find tournament leaderboard",
                                    result.failure.toString()
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
    @Suppress("DEPRECATION")
    fun showRecommendedTournamentsDialog() {
        LucraClient().queryRecommendedTournaments(
            limit = 50,
            offset = 0,
            includeCompletedTournaments = false
        ) {
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
     * Shows dialog to retrieve lightweight recommended tournaments.
     */
    fun showRecommendedTournamentsLightDialog() {
        val scrollView = ScrollView(context)
        val layout = createVerticalLayout()
        scrollView.addView(layout)

        val (locationIdLayout, locationIdInput) = createTextInputLayout("Location ID (optional)", recallKey = RecallKey.LOCATION_ID)
        layout.addView(locationIdLayout)

        val (limitLayout, limitInput) = createTextInputLayout("Limit", "20")
        limitInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(limitLayout)

        val (offsetLayout, offsetInput) = createTextInputLayout("Offset", "0")
        offsetInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(offsetLayout)

        val includeCompletedCheckbox = AppCompatCheckBox(context).apply {
            text = "Include Completed Tournaments"
            isChecked = false
        }
        layout.addView(includeCompletedCheckbox)

        createDialogBuilder()
            .setTitle("Recommended Tournaments Light")
            .setView(scrollView)
            .setPositiveButton("Query") { _, _ ->
                val locationId = locationIdInput.text.toString().takeIf { it.isNotBlank() }
                val limit = limitInput.text.toString().toIntOrNull() ?: 20
                val offset = offsetInput.text.toString().toIntOrNull() ?: 0
                LucraClient().queryRecommendedTournaments(
                    locationId = locationId,
                    limit = limit,
                    offset = offset,
                    includeCompletedTournaments = includeCompletedCheckbox.isChecked
                ) {
                    activity.runOnUiThread {
                        when (it) {
                            is PoolTournament.QueryRecommendedTournamentsLightResult.RecommendedTournamentsOutput -> {
                                displayRecommendedTournamentsLight(it)
                            }
                            is PoolTournament.QueryRecommendedTournamentsLightResult.Failure -> {
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
     * Shows dialog to join a tournament.
     */
    fun showJoinTournamentDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val input = createRecallField("Tournament ID", "d0b78c81-a22b-4f54-b1fe-2fadc8354c3b", RecallKey.TOURNAMENT_ID)
        
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

    private fun displayTournamentDetails(details: LucraTournamentDetails) {
        val displayString = buildString {
            append("Id > ${details.id}\n")
            append("Title > ${details.title}\n")
            append("Description > ${details.description}\n")
            append("Image Url > ${details.imageUrl}\n")
            append("Display Image Url > ${details.displayImageUrl}\n")
            append("Status > ${details.status} (${details.matchupStatus})\n")
            append("Type > ${details.type} (${details.matchupRewardType})\n")
            append("Visibility > ${details.visibility} (private: ${details.isPrivate})\n")
            append("Completed > ${details.isCompleted}\n")
            append("Not Started > ${details.isNotStarted}\n")
            append("Expired > ${details.isExpired}\n")
            append("Joinable > ${details.isJoinable}\n")
            append("Free Buy In > ${details.freeBuyIn}\n")
            append("Buy In > ${details.buyIn} (${details.buyInAmount}), fee ${details.feePercent}%\n")
            append("Pool Net > ${details.poolNetAmount} (complete ${details.poolNetAmountComplete})\n")
            append("Participants > ${details.participantsCount}/${details.maxParticipants}\n")
            append("Max Attempts > ${details.maxAttempts}\n")
            append("Starts At > ${details.startsAt}\n")
            append("Expires At > ${details.expiresAt}\n")
            append("Sign Up > ${details.signUpStart} – ${details.signUpEnd}\n")
            append("Game Id > ${details.gameId}\n")
            details.game?.let { append("Game > ${it.gameId}, minigame ${it.minigameEnabled}, unit ${it.scoringUnit}\n") }
            append("Timer > ${details.timer.caption} (${details.timer.state}, target ${details.timer.targetAt})\n")

            details.howToPlay?.let { steps ->
                append("\n===How To Play===\n")
                steps.forEach { append("${it.step}. ${it.text}\n") }
            }

            val payout = details.payoutStructure
            append("\n===Payout Structure===\n")
            append("${payout.title} — ${payout.description}\n")
            append("Jackpot > ${payout.jackpotDescriptor} ${payout.jackpotAmount}\n")
            append("No Payout > ${payout.noPayout}, Percentage > ${payout.isPercentagePayout}, Show Amount > ${payout.showAmount}\n")
            payout.rewards.forEach { reward ->
                append("${reward.positionLabel} > ${reward.rewardLabel} (${reward.amountLabel})")
                reward.catalogReward?.let { append(" prize: ${it.title} [${it.type}]") }
                append("\n")
            }

            append("\n===Leaderboard Columns===\n")
            append("${details.leaderboardColumns.joinToString { it.label }}\n")

            if (details.notices.isNotEmpty()) {
                append("\n===Notices===\n")
                details.notices.forEach { append("${it.type}: ${it.message} ${it.affectedFields}\n") }
            }

            if (details.terms.isNotEmpty()) {
                append("\n===Terms===\n")
                details.terms.forEach { append("${it.title}: ${it.description}\n") }
            }
        }

        showScrollingResult("Tournament Details Result", displayString)
    }

    private fun displayTournamentLeaderboard(
        result: PoolTournament.RetrieveTournamentLeaderboardResult.TournamentLeaderboardOutput,
    ) {
        val displayString = buildString {
            val leaderboard = result.leaderboard
            append("Total > ${leaderboard.totalCount}\n\n")
            leaderboard.entries.forEach { row ->
                append("${row.rank ?: "-"}: ${row.name} points=${row.points ?: "-"} payout=${row.payout ?: "-"}")
                if (row.isCurrentUser) append(" (you)")
                append("\n")
            }
            append("\n===Your Entry===\n")
            leaderboard.currentUserEntry?.let { entry ->
                append("Position > ${entry.positionOverride ?: entry.position}\n")
                append("Score > ${entry.score}\n")
                append("Last Updated > ${entry.lastUpdatedAt}\n")
            } ?: append("None\n")
        }

        showScrollingResult("Tournament Leaderboard Result", displayString)
    }

    private fun showScrollingResult(title: String, text: String) {
        val textView = TextView(context).apply {
            this.text = text
            setPadding(50, 50, 50, 50)
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }

        createDialogBuilder()
            .setTitle(title)
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

    private fun displayRecommendedTournamentsLight(result: PoolTournament.QueryRecommendedTournamentsLightResult.RecommendedTournamentsOutput) {
        val displayString = buildString {
            result.recommendedTournaments.forEach { tournament ->
                append("Title: ${tournament.title}\n")
                append("Type: ${tournament.type}\n")
                append("Status: ${tournament.status}\n")
                append("Can Join: ${tournament.canJoinTournament}\n")
                append("User Present: ${tournament.isUserPresent}\n")
                append("Fee: ${tournament.fee}\n")
                append("Buy In: ${tournament.buyInAmount}\n")
                append("Pot Total: ${tournament.potTotal}\n")
                append("Pot Net Amount: ${tournament.potNetAmount}\n")
                append("Max Participants: ${tournament.maxParticipants}\n")
                append("Total Participants: ${tournament.totalParticipants}\n")
                append("Reward Type: ${tournament.rewardType}\n")
                append("Visibility: ${tournament.visibilityLevel}\n")
                append("Starts At: ${tournament.startsAt}\n")
                append("Expires At: ${tournament.expiresAt}\n")
                append("Icon: ${tournament.iconUrl}\n")
                append("Description: ${tournament.description}\n")
                append("Game: ${tournament.game?.name} (${tournament.game?.id})\n")
                append("Game Description: ${tournament.game?.description}\n")
                append("Game Icon: ${tournament.game?.iconUrl}\n")
                append("Game Image: ${tournament.game?.imageUrl}\n")
                append("Game BG Image: ${tournament.game?.imageBgUrl}\n")
                append("Game Categories: ${tournament.game?.categoryIds?.joinToString()}\n")
                append("Track Results: ${tournament.game?.trackResults?.name}\n")
                append("Scoring Unit: ${tournament.game?.scoringUnit}\n")
                append("Rewards: ${tournament.rewardStructure.size}\n")
                append("===Leaderboard===\n")
                tournament.leaderboard.forEach { row ->
                    append("${row.place ?: "-"}: ${row.username} (${row.score ?: "0"})\n")
                }
                append("\n")
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
            .setTitle("Recommended Tournaments Light Result")
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
