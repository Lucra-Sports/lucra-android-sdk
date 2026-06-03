package com.lucrasports.sdk.app.headless_api

import android.app.Activity
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.auth.ResendCodeResult
import com.lucrasports.sdk.core.auth.SubmitPhoneNumberResult
import com.lucrasports.sdk.core.auth.SubmitVerificationCodeResult

/**
 * Handles headless phone authentication API interactions.
 *
 * Demonstrates the two-step phone auth flow:
 * 1. Submit phone number to receive SMS verification code
 * 2. Submit verification code to complete authentication
 */
internal class PhoneAuthApiHandler(private val activity: Activity) {

    /**
     * Shows dialog to enter a phone number and start the phone auth flow.
     */
    fun showPhoneAuthDialog() {
        val input = EditText(activity).apply {
            hint = "Phone number (e.g. 5551234567)"
            setPadding(50, 50, 50, 50)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Phone Authentication")
            .setMessage("Enter a US phone number to receive a verification code.")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                submitPhoneNumber(input.text.toString())
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun submitPhoneNumber(phoneNumber: String) {
        LucraClient().submitPhoneNumber(phoneNumber) { result ->
            activity.runOnUiThread {
                when (result) {
                    is SubmitPhoneNumberResult.Success -> {
                        showVerificationCodeDialog()
                    }

                    is SubmitPhoneNumberResult.Failure -> {
                        showResultDialog(
                            "Submit Phone Number Failed",
                            "Error: ${result.error::class.simpleName}\n${result.error}"
                        )
                    }
                }
            }
        }
    }

    private fun showVerificationCodeDialog() {
        val input = EditText(activity).apply {
            hint = "Verification code"
            setPadding(50, 50, 50, 50)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Enter Verification Code")
            .setMessage("Enter the code sent to your phone.")
            .setView(input)
            .setPositiveButton("Verify") { _, _ ->
                submitVerificationCode(input.text.toString())
            }
            .setNeutralButton("Resend Code") { _, _ ->
                resendCode()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun submitVerificationCode(code: String) {
        LucraClient().submitVerificationCode(code) { result ->
            activity.runOnUiThread {
                when (result) {
                    is SubmitVerificationCodeResult.Success -> {
                        showResultDialog(
                            "Authentication Successful",
                            "Signed in as: ${result.sdkUser.username}\n" +
                                "User ID: ${result.sdkUser.userId}"
                        )
                    }

                    is SubmitVerificationCodeResult.Failure -> {
                        showResultDialog(
                            "Verification Failed",
                            "Error: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    private fun resendCode() {
        LucraClient().resendCode { result ->
            activity.runOnUiThread {
                when (result) {
                    is ResendCodeResult.Success -> {
                        showVerificationCodeDialog()
                    }

                    is ResendCodeResult.Failure -> {
                        showResultDialog("Resend Code Failed", "Error: ${result.error}")
                    }
                }
            }
        }
    }

    private fun showResultDialog(title: String, message: String) {
        val textView = TextView(activity).apply {
            text = message
            setPadding(50, 50, 50, 50)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setView(textView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
