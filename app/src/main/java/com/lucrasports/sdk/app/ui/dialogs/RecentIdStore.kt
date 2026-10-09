package com.lucrasports.sdk.app.ui.dialogs

import android.content.Context
import androidx.core.content.edit
import com.lucrasports.sdk.app.SAMPLE_PREFS

internal enum class RecallKey(val label: String) {
    MATCHUP_ID("Matchup IDs"),
    TOURNAMENT_ID("Tournament IDs"),
    GAME_ID("Game IDs"),
    CONTEST_ID("Contest IDs"),
    PLAYER_ID("Player IDs"),
    LOCATION_ID("Location IDs"),
}

private const val MAX_PER_KEY = 5

/** UUIDs and game slugs never contain a pipe. */
private const val SEPARATOR = "|"

/**
 * The IDs the sample was last driven with, so retesting a flow does not mean hunting for a UUID.
 *
 * Opaque test identifiers only — never phone numbers, emails or tokens.
 */
internal class RecentIdStore(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(SAMPLE_PREFS, Context.MODE_PRIVATE)

    fun recent(key: RecallKey): List<String> =
        preferences.getString(key.prefsKey(), null)
            .orEmpty()
            .split(SEPARATOR)
            .filter { it.isNotBlank() }

    fun remember(key: RecallKey, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || SEPARATOR in trimmed) return
        val updated = (listOf(trimmed) + recent(key)).distinct().take(MAX_PER_KEY)
        preferences.edit { putString(key.prefsKey(), updated.joinToString(SEPARATOR)) }
    }

    fun total(): Int = RecallKey.entries.sumOf { recent(it).size }

    fun clear() {
        preferences.edit { RecallKey.entries.forEach { remove(it.prefsKey()) } }
    }

    fun summary(): String = RecallKey.entries
        .mapNotNull { key ->
            val values = recent(key)
            if (values.isEmpty()) null else "${key.label}\n" + values.joinToString("\n") { "  $it" }
        }
        .joinToString("\n\n")
        .ifEmpty { "Nothing remembered yet." }

    private fun RecallKey.prefsKey() = "RECENT_ID_$name"
}
