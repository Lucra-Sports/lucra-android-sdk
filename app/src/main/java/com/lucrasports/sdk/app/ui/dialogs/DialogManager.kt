package com.lucrasports.sdk.app.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
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

    protected val recentIds: RecentIdStore by lazy { RecentIdStore(context) }

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
     *
     * A [recallKey] prefills the field with the last ID of that kind and offers the rest as chips.
     * The first element is [View] because it is the wrapper when recall is on; call sites only
     * ever add it to a layout.
     */
    protected fun createTextInputLayout(
        hint: String,
        defaultText: String = "",
        recallKey: RecallKey? = null,
    ): Pair<View, TextInputEditText> {
        val layout = TextInputLayout(context).apply {
            this.hint = hint
        }
        val editText = TextInputEditText(context).apply {
            setText(recallKey?.let { recentIds.recent(it).firstOrNull() } ?: defaultText)
        }
        layout.addView(editText)
        val root = recallKey?.let { key -> wrapWithRecall(layout, editText, key, defaultText) }
        return Pair(root ?: layout, editText)
    }

    protected fun createRecallField(
        hint: String,
        sampleValue: String = "",
        recallKey: RecallKey,
    ): RecallField = RecallField(context, recentIds, recallKey, hint, sampleValue)

    private fun wrapWithRecall(
        field: View,
        editText: EditText,
        key: RecallKey,
        sampleValue: String,
    ): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(field)
        addView(RecallChips(context, recentIds, key, sampleValue) { editText.setText(it) })
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                recentIds.remember(key, editText.text?.toString().orEmpty())
            }
        })
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

