package com.lucrasports.sdk.app.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Base class for dialog management with common utilities.
 * Provides helper methods for creating consistent dialogs across the sample app.
 */
internal abstract class DialogManager(protected val context: Context) {

    protected val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(context)

    /**
     * Creates a MaterialAlertDialogBuilder with consistent styling.
     */
    protected fun createDialogBuilder(): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
    }

    /**
     * Creates a vertical LinearLayout for dialog content.
     */
    protected fun createVerticalLayout(padding: Int = 50): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 30, padding, 30)
        }
    }

    /**
     * Creates a TextInputLayout with an EditText.
     */
    protected fun createTextInputLayout(
        hint: String,
        defaultText: String = ""
    ): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(context).apply {
            this.hint = hint
        }
        val editText = TextInputEditText(context).apply {
            setText(defaultText)
        }
        layout.addView(editText)
        return Pair(layout, editText)
    }

    /**
     * Creates a simple EditText with hint.
     */
    protected fun createEditText(
        hint: String,
        defaultText: String = ""
    ): EditText {
        return EditText(context).apply {
            this.hint = hint
            setText(defaultText)
        }
    }

    /**
     * Creates a Spinner with the given items.
     */
    protected fun createSpinner(items: Array<String>): Spinner {
        return Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                items
            )
        }
    }

    /**
     * Shows a simple message dialog.
     */
    protected fun showMessageDialog(
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        onPositiveClick: (() -> Unit)? = null
    ) {
        createDialogBuilder()
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onPositiveClick?.invoke()
                dialog.dismiss()
            }
            .show()
    }
}

