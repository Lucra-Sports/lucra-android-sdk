package com.lucrasports.sdk.app.catalog

import android.view.View
import java.text.Normalizer
import java.util.Locale

internal enum class SampleSurface(val badge: String) {
    Flow("flow"),
    Api("api"),
    Ui("ui"),
}

/** Declaration order is display order. Titles live here, not strings.xml, to keep
 *  [buildListItems] Context-free and JVM-testable. */
internal enum class SampleCategory(val title: String) {
    AuthIdentity("Auth & identity"),
    ProfileWallet("Profile & wallet"),
    HomeFeeds("Home & feeds"),
    SportsContests("Sports contests"),
    GamesMinigames("Games & minigames"),
    Tournaments("Tournaments"),
    RewardsAchievements("Rewards & achievements"),
    RecreationalGames("Recreational games"),
    EmbeddedComponents("Embedded components"),
    DebugTools("Debug tools"),
}

internal enum class AuthRequirement {
    None,

    /** SDK shows its own login. The row is locked but always fires. */
    SdkPrompts,

    /** The sample itself refuses while signed out. */
    HostBlocks,
}

/** [onReady] is not called if the user cancels a parameter prompt. */
internal fun interface EmbeddedComponentProvider {
    fun provide(actions: SampleActions, onReady: (View) -> Unit)
}

/** [onSelect] takes its receiver at invoke time, so an entry never holds an Activity and the
 *  catalog can be built and inspected from a JVM test. */
internal data class SampleEntry(
    /** Stable and never reused: persisted in the recents list. */
    val id: String,
    val title: String,
    val description: String,
    val category: SampleCategory,
    val surface: SampleSurface,
    val auth: AuthRequirement = AuthRequirement.None,
    val keywords: List<String> = emptyList(),
    val debugOnly: Boolean = false,
    val embedded: EmbeddedComponentProvider? = null,
    val onSelect: SampleActions.() -> Unit = {},
) {
    /** A body val, so it stays out of the generated `equals`/`hashCode`/`copy`. */
    val searchIndex: String = normalizeForSearch(
        (listOf(title, description, category.title, surface.badge) + keywords).joinToString(" ")
    )

    init {
        require((surface == SampleSurface.Ui) == (embedded != null)) {
            "$id: a Ui entry must carry an embedded provider, and only a Ui entry may"
        }
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/** [Locale.ROOT] is deliberate: a Turkish default locale lower-cases `I` to `ı`, which would
 *  stop "API" ever matching. */
internal fun normalizeForSearch(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)

internal fun searchTokens(query: String): List<String> =
    normalizeForSearch(query).split(' ', '\t', '\n').filter { it.isNotBlank() }
