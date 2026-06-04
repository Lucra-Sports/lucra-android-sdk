package com.lucrasports.sdk.app.ui.dialogs

import android.app.Activity
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lucrasports.data.demographic.DemographicInteractions.DemographicFormError
import com.lucrasports.data.demographic.DemographicInteractions.SubmitDemographicFormResult
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.utils.MetadataHelper
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.user.SDKUser
import com.lucrasports.sdk.core.user.SDKUserResult

/**
 * Manages dialogs related to user configuration and management.
 */
internal class UserDialogs(private val activity: Activity) : DialogManager(activity) {

    /**
     * Shows dialog to configure user details.
     */
    fun showConfigureUserDialog(
        currentUser: SDKUser?,
        onUserUpdated: (SDKUser) -> Unit
    ) {
        val userForm = layoutInflater.inflate(R.layout.main_configure_user_options, null).apply {
            findViewById<TextInputEditText>(R.id.username).setText(currentUser?.username.orEmpty())
            findViewById<TextInputEditText>(R.id.email).setText(currentUser?.email.orEmpty())
            findViewById<TextInputEditText>(R.id.avatar).setText(currentUser?.avatarUrl.orEmpty())
            findViewById<TextInputEditText>(R.id.phone).setText(currentUser?.phoneNumber.orEmpty())
            findViewById<TextInputEditText>(R.id.firstName).setText(currentUser?.firstName.orEmpty())
            findViewById<TextInputEditText>(R.id.lastName).setText(currentUser?.lastName.orEmpty())
            findViewById<TextInputEditText>(R.id.address).setText(currentUser?.address.orEmpty())
            findViewById<TextInputEditText>(R.id.addresCont).setText(currentUser?.addressCont.orEmpty())
            findViewById<TextInputEditText>(R.id.city).setText(currentUser?.city.orEmpty())
            findViewById<TextInputEditText>(R.id.state).setText(currentUser?.state.orEmpty())
            findViewById<TextInputEditText>(R.id.zip).setText(currentUser?.zip.orEmpty())

            val metadataContainer = findViewById<LinearLayout>(R.id.metadata_container)
            val addMetadataButton = findViewById<MaterialButton>(R.id.add_metadata_row)

            val existingMetadata = currentUser?.metadata.orEmpty()
            if (existingMetadata.isEmpty()) {
                MetadataHelper.addMetadataRow(context, metadataContainer)
            } else {
                existingMetadata.forEach { (key, value) ->
                    MetadataHelper.addMetadataRow(context, metadataContainer, key, value)
                }
            }

            addMetadataButton.setOnClickListener {
                MetadataHelper.addMetadataRow(context, metadataContainer)
            }
        }

        createDialogBuilder()
            .setTitle("Configure the user")
            .setView(userForm)
            .setPositiveButton("Configure") { dialog, _ ->
                val metadataContainer = userForm.findViewById<LinearLayout>(R.id.metadata_container)
                val metadata = MetadataHelper.collectMetadata(metadataContainer)

                val newSdkUser = SDKUser(
                    username = userForm.findViewById<TextInputEditText>(R.id.username).text
                        .toString().takeIf { it.isNotBlank() },
                    email = userForm.findViewById<TextInputEditText>(R.id.email).text
                        .toString().takeIf { it.isNotBlank() },
                    avatarUrl = userForm.findViewById<TextInputEditText>(R.id.avatar).text
                        .toString().takeIf { it.isNotBlank() },
                    phoneNumber = userForm.findViewById<TextInputEditText>(R.id.phone).text
                        .toString().takeIf { it.isNotBlank() },
                    firstName = userForm.findViewById<TextInputEditText>(R.id.firstName).text
                        .toString().takeIf { it.isNotBlank() },
                    lastName = userForm.findViewById<TextInputEditText>(R.id.lastName).text
                        .toString().takeIf { it.isNotBlank() },
                    address = userForm.findViewById<TextInputEditText>(R.id.address).text
                        .toString().takeIf { it.isNotBlank() },
                    addressCont = userForm.findViewById<TextInputEditText>(R.id.addresCont).text
                        .toString().takeIf { it.isNotBlank() },
                    city = userForm.findViewById<TextInputEditText>(R.id.city).text
                        .toString().takeIf { it.isNotBlank() },
                    state = userForm.findViewById<TextInputEditText>(R.id.state).text
                        .toString().takeIf { it.isNotBlank() },
                    zip = userForm.findViewById<TextInputEditText>(R.id.zip).text
                        .toString().takeIf { it.isNotBlank() },
                    metadata = metadata,
                )

                onUserUpdated(newSdkUser)
                LucraClient().configure(newSdkUser) { result ->
                    activity.runOnUiThread {
                        when (result) {
                            is SDKUserResult.Error -> {
                                Log.e("Lucra SDK Sample", "Unable to configure user ${result.error}")
                                Toast.makeText(
                                    context,
                                    "Error!: " + result.error.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            SDKUserResult.InvalidUsername -> {
                                Toast.makeText(
                                    context,
                                    "Invalid Username!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            SDKUserResult.Loading -> {
                                dialog.dismiss()
                            }
                            is SDKUserResult.Success -> {
                                dialog.dismiss()
                                Toast.makeText(
                                    context,
                                    "User configured!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            SDKUserResult.WaitingForLogin -> {
                                dialog.dismiss()
                                Toast.makeText(
                                    context,
                                    "Waiting for login prior to configuration",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Shows dialog to update username.
     */
    fun showUpdateUsernameDialog(currentUser: SDKUser?, onUserUpdated: (SDKUser) -> Unit) {
        if (currentUser == null) {
            Toast.makeText(context, "Not logged in yet!", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(context).apply {
            if (currentUser.username.isNullOrEmpty()) {
                hint = "No username set yet"
            } else {
                hint = "Current username: ${currentUser.username}"
                setText(currentUser.username)
            }
        }

        createDialogBuilder()
            .setTitle("Update Username")
            .setView(input)
            .setPositiveButton("OK") { dialog, _ ->
                val newUsername = input.text.toString()
                if (newUsername.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Username cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val newSdkUser = currentUser.copy(username = newUsername)
                LucraClient().configure(newSdkUser) {
                    activity.runOnUiThread {
                        when (it) {
                            is SDKUserResult.Error -> {
                                Log.e("Lucra SDK Sample", "Unable to update username ${it.error}")
                                Toast.makeText(
                                    context,
                                    "Unable to update username",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            SDKUserResult.InvalidUsername -> {
                                Toast.makeText(
                                    context,
                                    "Invalid username, try a different one",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            SDKUserResult.NotLoggedIn -> {
                                Log.e("Lucra SDK Sample", "User not logged in yet!")
                                Toast.makeText(
                                    context,
                                    "User not logged in!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is SDKUserResult.Success -> {
                                Toast.makeText(
                                    context,
                                    "Username updated to ${it.sdkUser.username}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onUserUpdated(it.sdkUser)
                                dialog.dismiss()
                            }
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Shows dialog to submit demographic form data headlessly.
     */
    fun showSubmitDemographicFormDialog() {
        val padding = 32
        val fieldSpacing = 16

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val dobDayInput = EditText(context).apply {
            hint = "Day of Birth (1-31)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val dobMonthInput = EditText(context).apply {
            hint = "Month of Birth (1-12)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val dobYearInput = EditText(context).apply {
            hint = "Year of Birth (e.g. 1990)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val emailInput = EditText(context).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val zipInput = EditText(context).apply {
            hint = "Zip (optional)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val firstNameInput = EditText(context).apply {
            hint = "First Name (optional)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val lastNameInput = EditText(context).apply {
            hint = "Last Name (optional)"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        listOf(
            dobDayInput, dobMonthInput, dobYearInput,
            emailInput, zipInput, firstNameInput, lastNameInput
        ).forEach { field ->
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = fieldSpacing }
            layout.addView(field, params)
        }

        createDialogBuilder()
            .setTitle("Submit Demographic Form")
            .setView(layout)
            .setPositiveButton("Submit") { dialog, _ ->
                val dobDay = dobDayInput.text.toString().toIntOrNull()
                val dobMonth = dobMonthInput.text.toString().toIntOrNull()
                val dobYear = dobYearInput.text.toString().toIntOrNull()
                val email = emailInput.text.toString()

                if (dobDay == null || dobMonth == null || dobYear == null || email.isBlank()) {
                    Toast.makeText(context, "Day, Month, Year, and Email are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                LucraClient().submitDemographicForm(
                    dobDay = dobDay,
                    dobMonth = dobMonth,
                    dobYear = dobYear,
                    email = email,
                    zip = zipInput.text.toString().takeIf { it.isNotBlank() },
                    firstName = firstNameInput.text.toString().takeIf { it.isNotBlank() },
                    lastName = lastNameInput.text.toString().takeIf { it.isNotBlank() },
                ) { result ->
                    activity.runOnUiThread {
                        when (result) {
                            is SubmitDemographicFormResult.Success -> {
                                Toast.makeText(context, "Demographic form submitted successfully!", Toast.LENGTH_SHORT).show()
                            }
                            is SubmitDemographicFormResult.Failure -> {
                                val message = when (val error = result.error) {
                                    is DemographicFormError.APIError -> "API Error: ${error.message}"
                                    is DemographicFormError.NotInitialized -> "SDK not initialized"
                                    is DemographicFormError.NotAllowed -> "Account is blocked or suspended"
                                    is DemographicFormError.InvalidDateOfBirth -> "Invalid date of birth"
                                    is DemographicFormError.Unknown -> "Unknown error"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

