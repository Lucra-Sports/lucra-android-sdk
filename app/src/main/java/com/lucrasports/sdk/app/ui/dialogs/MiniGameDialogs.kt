package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.contest.LucraError
import com.lucrasports.sdk.core.contest.minigame.MiniGameInteractions.GetMiniGamesResult
import com.lucrasports.sdk.core.contest.minigame.MiniGameInteractions.StartMiniGameResult
import com.lucrasports.sdk.core.minigames.LucraMiniGameMode
import com.lucrasports.sdk.core.ui.LucraUiProvider

internal class MiniGameDialogs(private val activity: Activity) : DialogManager(activity) {

    private data class MiniGameInputs(
        val gameId: String,
        val gameMode: LucraMiniGameMode,
        val amount: Double?,
        val matchupId: String?,
        val handlePostNavigation: Boolean,
    )

    fun showLaunchMiniGameFlowDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        showInputDialog("Launch MiniGame Flow") { inputs ->
            onLaunchFlow(
                LucraUiProvider.LucraFlow.MiniGame(
                    gameId = inputs.gameId,
                    gameMode = inputs.gameMode,
                    amount = inputs.amount,
                    matchupId = inputs.matchupId,
                    handlePostNavigation = inputs.handlePostNavigation,
                )
            )
        }
    }

    fun showStartMiniGameApiDialog() {
        showInputDialog("Start MiniGame (Headless)") { inputs ->
            LucraClient().startMiniGame(
                gameId = inputs.gameId,
                gameMode = inputs.gameMode,
                amount = inputs.amount,
                matchupId = inputs.matchupId,
                onProgress = { step ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, step.displayText, Toast.LENGTH_SHORT).show()
                    }
                },
                onResult = { result ->
                    activity.runOnUiThread {
                        displayStartMiniGameResult(result)
                    }
                },
            )
        }
    }

    fun preloadGeoToken() {
        LucraClient().preloadGeoToken()
        Toast.makeText(
            activity,
            "Preloading geo token (fire-and-forget)…",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun showGetMiniGamesApiDialog() {
        LucraClient().getMiniGames { result ->
            activity.runOnUiThread {
                displayGetMiniGamesResult(result)
            }
        }
    }

    private fun showInputDialog(
        title: String,
        onSubmit: (MiniGameInputs) -> Unit,
    ) {
        val layout = createVerticalLayout()

        val (gameIdLayout, gameIdInput) = createTextInputLayout("Game ID", "runaway-web")
        layout.addView(gameIdLayout)

        val gameModeSpinner = createSpinner(
            LucraMiniGameMode.entries.map { it.name }.toTypedArray()
        )
        // Default to Practice
        gameModeSpinner.setSelection(LucraMiniGameMode.entries.indexOf(LucraMiniGameMode.Practice))
        val gameModeContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = "Game Mode:"
                setPadding(0, 30, 0, 0)
            })
            addView(gameModeSpinner)
        }
        layout.addView(gameModeContainer)

        val (amountLayout, amountInput) = createTextInputLayout(
            hint = "Amount (USD) — leave blank for Practice",
            defaultText = "",
        )
        layout.addView(amountLayout)

        val (matchupIdLayout, matchupIdInput) = createTextInputLayout(
            hint = "Matchup/Tournament ID (Tournament mode only)",
            defaultText = "",
        )
        layout.addView(matchupIdLayout)

        val handlePostNavigationCheckBox = CheckBox(context).apply {
            text = "Handle post navigation (1v1 / FFA → Matchup Details)"
            setPadding(0, 30, 0, 0)
        }
        layout.addView(handlePostNavigationCheckBox)

        val dialog = createDialogBuilder()
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Start", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val gameId = gameIdInput.text.toString().trim()
                if (gameId.isBlank()) {
                    Toast.makeText(context, "Game ID is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val gameMode = LucraMiniGameMode.valueOf(gameModeSpinner.selectedItem.toString())
                val amount = amountInput.text.toString().trim().toDoubleOrNull()
                val matchupId = matchupIdInput.text.toString().trim().ifBlank { null }

                onSubmit(
                    MiniGameInputs(
                        gameId = gameId,
                        gameMode = gameMode,
                        amount = amount,
                        matchupId = matchupId,
                        handlePostNavigation = handlePostNavigationCheckBox.isChecked,
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun displayStartMiniGameResult(result: StartMiniGameResult) {
        val (title, message) = when (result) {
            is StartMiniGameResult.Success -> "MiniGame Session Started" to buildString {
                append("Iframe URL:\n${result.session.iframeUrl}\n\n")
                append("Session ID: ${result.session.sessionId}\n")
                append("Matchup ID: ${result.session.matchupId ?: "—"}")
            }
            is StartMiniGameResult.Failure -> "Failed to Start MiniGame" to result.failure.describe()
        }

        val textView = TextView(context).apply {
            text = message
            setPadding(50, 50, 50, 50)
            setTextIsSelectable(true)
        }

        createDialogBuilder()
            .setTitle(title)
            .setView(textView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun displayGetMiniGamesResult(result: GetMiniGamesResult) {
        val (title, message) = when (result) {
            is GetMiniGamesResult.Success -> "Tenant MiniGames (${result.games.size})" to
                result.games.joinToString("\n\n") { game ->
                    buildString {
                        append("${game.name} (${game.gameId})\n")
                        append("Configs: ${game.config.size}")
                        game.config.forEach { config ->
                            append("\n  • ${config.mode}")
                            config.wagerAmount?.let { append(" — \$$it") }
                            config.groupSize?.let { append(" × $it") }
                        }
                    }
                }.ifBlank { "No minigames enabled for this tenant." }
            is GetMiniGamesResult.Failure -> "Failed to Get MiniGames" to result.failure.describe()
        }

        val textView = TextView(context).apply {
            text = message
            setPadding(50, 50, 50, 50)
            setTextIsSelectable(true)
        }

        createDialogBuilder()
            .setTitle(title)
            .setView(textView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun LucraError.describe(): String = "${this::class.simpleName}: $this"
}
