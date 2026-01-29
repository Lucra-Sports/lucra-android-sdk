package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.widget.LinearLayout
import android.widget.Toast
import com.lucrasports.sdk.app.utils.LocationSelector
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.ui.LucraUiProvider

/**
 * Manages dialogs for launching Lucra flows.
 */
internal class FlowDialogs(activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to launch the Home Page flow with optional location ID.
     */
    fun showHomePageDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val (scrollView, radioGroup) = LocationSelector.createLocationSelector(context)

        createDialogBuilder()
            .setTitle("Select a Location (Optional)")
            .setView(scrollView)
            .setPositiveButton("Continue") { _, _ ->
                val locationId = LocationSelector.getSelectedLocationId(radioGroup)
                onLaunchFlow(LucraUiProvider.LucraFlow.HomePage(locationId))
            }
            .setNeutralButton("Skip") { _, _ ->
                onLaunchFlow(LucraUiProvider.LucraFlow.HomePage(null))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to create a games matchup with optional game ID or location ID.
     */
    fun showCreateGamesMatchupDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val inputGameId = createEditText("Game ID Ex: CORNHOLE")
        
        val (locationScrollView, locationRadioGroup) = LocationSelector.createLocationSelector(context)

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(inputGameId)
            addView(android.widget.TextView(context).apply {
                text = "and/or select a location:"
                setPadding(0, 30, 0, 10)
                textSize = 14f
            })
            addView(locationScrollView)
        }

        createDialogBuilder()
            .setTitle("Provide a Game ID and/or Location")
            .setView(mainLayout)
            .setPositiveButton("Continue") { _, _ ->
                when {
                    !inputGameId.text.isNullOrBlank() -> {
                        onLaunchFlow(
                            LucraUiProvider.LucraFlow.CreateGamesMatchupById(
                                gameId = inputGameId.text.toString(),
                            )
                        )
                    }
                    else -> {
                        val locationId = LocationSelector.getSelectedLocationId(locationRadioGroup)
                        onLaunchFlow(LucraUiProvider.LucraFlow.CreateGamesMatchup(locationId))
                    }
                }
            }
            .setNeutralButton("Skip ID") { _, _ ->
                onLaunchFlow(LucraUiProvider.LucraFlow.CreateGamesMatchup())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to navigate to matchup details.
     */
    fun showMatchupDetailsDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val input = createEditText("ID of Any Matchup")

        createDialogBuilder()
            .setTitle("Provide a Matchup ID")
            .setView(input)
            .setPositiveButton("Continue") { _, _ ->
                onLaunchFlow(LucraUiProvider.LucraFlow.MatchupDetails(input.text.toString()))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to navigate to tournament details.
     */
    fun showTournamentDetailsDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val input = createEditText("ID of Tournament", "6e1c8e78-20f4-4f1b-a104-fa6f4925c657")

        createDialogBuilder()
            .setTitle("Provide a Tournament ID")
            .setView(input)
            .setPositiveButton("Continue") { _, _ ->
                onLaunchFlow(LucraUiProvider.LucraFlow.TournamentDetails(input.text.toString()))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Shows dialog to navigate via deeplink URI.
     */
    fun showDeeplinkDialog(onLaunchFlow: (LucraUiProvider.LucraFlow) -> Unit) {
        val input = createEditText("deeplink uri, Ex: lucra://referral/com.tennis...")

        createDialogBuilder()
            .setTitle("Provide Deeplink URI")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val lucraFlow = LucraClient().getLucraFlowForDeeplinkUri(input.text.toString())
                if (lucraFlow != null) {
                    onLaunchFlow(lucraFlow)
                } else {
                    Toast.makeText(
                        context,
                        "Invalid URI - could not parse!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

