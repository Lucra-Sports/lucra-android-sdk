package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.Toast
import com.lucrasports.sdk.core.LucraClient

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
}
