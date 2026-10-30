package com.lucrasports.sdk.app.ui.dialogs

import android.content.Context
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lucrasports.sdk.app.R

private const val CHIP_LABEL_MAX = 12
private const val CHIP_PADDING_H = 24
private const val CHIP_PADDING_V = 12
private const val CHIP_GAP = 16

/** Hidden when there is no history and no sample value. */
internal class RecallChips(
    context: Context,
    store: RecentIdStore,
    key: RecallKey,
    sampleValue: String,
    private val onPick: (String) -> Unit,
) : HorizontalScrollView(context) {

    init {
        isHorizontalScrollBarEnabled = false
        setPadding(0, CHIP_GAP, 0, 0)

        val recent = store.recent(key)
        val offered = buildList {
            addAll(recent)
            if (sampleValue.isNotBlank() && sampleValue !in recent) add(sampleValue)
        }
        if (offered.isEmpty()) {
            visibility = GONE
        } else {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            offered.forEach { value -> row.addView(chip(value)) }
            addView(row)
        }
    }

    private fun chip(value: String) = TextView(context).apply {
        text = if (value.length > CHIP_LABEL_MAX) value.take(8) + "..." else value
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        textSize = 12f
        setBackgroundResource(R.drawable.bg_recent_chip)
        setPadding(CHIP_PADDING_H, CHIP_PADDING_V, CHIP_PADDING_H, CHIP_PADDING_V)
        isClickable = true
        isFocusable = true
        setOnClickListener { onPick(value) }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = CHIP_GAP }
    }
}

/**
 * [text] has the same type as `EditText.text`, so call sites read it unchanged.
 *
 * Recorded on detach, not on confirm: a cancelled dialog still remembers what was typed, which is
 * what you want when cancelling means fixing something and coming straight back.
 */
internal class RecallField(
    context: Context,
    store: RecentIdStore,
    key: RecallKey,
    hint: String,
    sampleValue: String,
) : LinearLayout(context) {

    private val editText = EditText(context).apply {
        this.hint = hint
        setText(store.recent(key).firstOrNull() ?: sampleValue)
    }

    val text: Editable? get() = editText.text

    init {
        orientation = VERTICAL
        addView(editText)
        addView(RecallChips(context, store, key, sampleValue) { editText.setText(it) })
        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                store.remember(key, editText.text?.toString().orEmpty())
            }
        })
    }
}
