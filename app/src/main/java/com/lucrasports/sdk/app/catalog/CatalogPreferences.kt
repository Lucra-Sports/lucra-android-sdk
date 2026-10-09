package com.lucrasports.sdk.app.catalog

import android.content.SharedPreferences
import androidx.core.content.edit

private const val KEY_RECENT_IDS = "CATALOG_RECENT_IDS"
private const val KEY_EXPANDED = "CATALOG_EXPANDED"
private const val KEY_SURFACE = "CATALOG_SURFACE"
private const val SEPARATOR = ","

/**
 * The query is deliberately absent: a stale filter on cold start reads as a broken app, so it
 * survives rotation only, via saved instance state.
 *
 * Ordered strings rather than `StringSet` because a set has no order, and order is the point for
 * recents. Unknown ids and names are dropped on read, so a rename degrades quietly.
 */
internal class CatalogPreferences(private val preferences: SharedPreferences) {

    fun load(): CatalogUiState = CatalogUiState(
        surface = preferences.getString(KEY_SURFACE, null)
            ?.let { name -> SampleSurface.entries.firstOrNull { it.name == name } },
        expanded = preferences.getString(KEY_EXPANDED, null)
            .orEmpty()
            .split(SEPARATOR)
            .mapNotNull { name -> SampleCategory.entries.firstOrNull { it.name == name } }
            .toSet(),
        recentIds = preferences.getString(KEY_RECENT_IDS, null)
            .orEmpty()
            .split(SEPARATOR)
            .filter { it.isNotBlank() },
    )

    fun save(state: CatalogUiState) {
        preferences.edit {
            putString(KEY_RECENT_IDS, state.recentIds.joinToString(SEPARATOR))
            putString(KEY_EXPANDED, state.expanded.joinToString(SEPARATOR) { it.name })
            putString(KEY_SURFACE, state.surface?.name)
        }
    }
}
