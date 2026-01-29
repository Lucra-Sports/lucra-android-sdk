package com.lucrasports.sdk.app.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.lucrasports.sdk.app.R

/**
 * Helper class for managing metadata key-value pairs in dialogs.
 */
internal object MetadataHelper {

    /**
     * Adds a metadata row (key-value pair) to the container.
     */
    fun addMetadataRow(
        context: Context,
        container: LinearLayout,
        key: String? = null,
        value: String? = null
    ) {
        val layoutInflater = LayoutInflater.from(context)
        val row = layoutInflater.inflate(R.layout.view_metadata_row, container, false)
        val keyInput = row.findViewById<TextInputEditText>(R.id.metadata_key)
        val valueInput = row.findViewById<TextInputEditText>(R.id.metadata_value)
        val removeButton = row.findViewById<ImageButton>(R.id.remove_metadata_row)

        keyInput.setText(key)
        valueInput.setText(value)

        removeButton.setOnClickListener { container.removeView(row) }
        container.addView(row)
    }

    /**
     * Collects all metadata from the container into a map.
     */
    fun collectMetadata(container: LinearLayout): Map<String, String> {
        val metaMap = mutableMapOf<String, String>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            val key = row.findViewById<TextInputEditText>(R.id.metadata_key)?.text
                ?.toString()?.takeIf { it.isNotBlank() }
            val value = row.findViewById<TextInputEditText>(R.id.metadata_value)?.text
                ?.toString()

            if (key != null) {
                metaMap[key] = value.orEmpty()
            }
        }
        return metaMap
    }
}
