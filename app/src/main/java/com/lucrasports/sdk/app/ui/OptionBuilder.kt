package com.lucrasports.sdk.app.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lucrasports.sdk.app.R

/**
 * Helper class for building option rows in the sample app UI.
 */
internal class OptionBuilder(private val context: Context) {

    private val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(context)

    /**
     * Appends an option UI to the root view group.
     */
    fun appendOption(
        title: String,
        description: String,
        root: ViewGroup,
        drawable: Drawable? = null,
        onClick: (ViewGroup) -> Unit
    ) {
        val optionView = layoutInflater.inflate(R.layout.main_option_click, root, false)
        optionView.findViewById<TextView>(R.id.option_title).text = title
        drawable?.let {
            optionView.findViewById<ImageView>(R.id.option_icon).setImageDrawable(it)
        }
        optionView.findViewById<TextView>(R.id.option_description).text = description

        optionView.setOnClickListener {
            onClick(optionView.findViewById(R.id.option_view_container))
        }
        root.addView(optionView)
    }
}

