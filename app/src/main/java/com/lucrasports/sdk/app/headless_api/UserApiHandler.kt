package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.profile.ProfileInteractions

/**
 * Handles user-related API interactions.
 */
internal class UserApiHandler(private val activity: Activity) {

    /**
     * Checks KYC status for a user.
     */
    fun checkKYCStatus(userId: String) {
        LucraClient().checkUsersKYCStatus(
            userId,
            object : LucraClient.LucraKYCStatusListener {
                override fun onKYCStatusCheckFailed(exception: Exception) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "Verified Failed $exception",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onKYCStatusAvailable(isVerified: Boolean) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "Is user verified? $isVerified",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        LucraClient().logout(activity)
        Toast.makeText(activity, "Successfully logged out", Toast.LENGTH_LONG).show()
    }

    /**
     * Retrieves and displays user matchups.
     */
    fun getUserMatchups() {
        LucraClient().getUserMatchups(limit = 10) { result ->
            activity.runOnUiThread {
                val title: String
                val displayString: String
                when (result) {
                    is ProfileInteractions.GetUserMatchupsResult.Success -> {
                        title = "User Matchups"
                        val sections = result.matchups.types
                        displayString = if (sections.isEmpty()) {
                            "No matchups found."
                        } else {
                            sections.joinToString("\n\n") { typeSection ->
                                "${typeSection.label}:\n" + typeSection.statuses.joinToString("\n") { statusSection ->
                                    "  ${statusSection.label}: ${statusSection.matchups.size} of ${statusSection.totalMatchups} matchups"
                                }
                            }
                        }
                    }
                    is ProfileInteractions.GetUserMatchupsResult.Failure -> {
                        title = "Failed to retrieve matchups"
                        displayString = when (val failure = result.failure) {
                            is ProfileInteractions.FailedUserMatchupsCall.User -> "User error: ${failure.error}"
                            is ProfileInteractions.FailedUserMatchupsCall.CustomError -> failure.message
                            ProfileInteractions.FailedUserMatchupsCall.Unknown -> "Unknown error"
                        }
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
