package com.lucrasports.sdk.app.catalog

/** Imports nothing from `android.*`, so this is JVM-testable. Keep it that way: the module does
 *  not set `unitTests.isReturnDefaultValues`, so an android stub call here would throw. */

internal const val MAX_RECENTS = 8

internal sealed interface CatalogListItem {
    val id: String

    data class Recents(val entries: List<Entry>) : CatalogListItem {
        override val id: String get() = "recents"
    }

    data class CategoryHeader(
        val category: SampleCategory,
        val count: Int,
        val expanded: Boolean,
    ) : CatalogListItem {
        override val id: String get() = "hdr.${category.name}"
    }

    /** Render fields only. Holding a [SampleEntry] would drag `onSelect` into `equals`, whose
     *  identity-based equality makes every row look changed on every keystroke. */
    data class Entry(
        val entryId: String,
        val title: String,
        val description: String,
        val surface: SampleSurface,
        val locked: Boolean,
        /** Set only in search results, where rows are shown outside their section. */
        val categoryLabel: String?,
        /** Only meaningful for [SampleSurface.Ui], which expands in place. */
        val expanded: Boolean = false,
    ) : CatalogListItem {
        override val id: String get() = "entry.$entryId"
    }

    data class ComponentHost(val entryId: String) : CatalogListItem {
        override val id: String get() = "host.$entryId"
    }

    data class Empty(val query: String) : CatalogListItem {
        override val id: String get() = "empty"
    }
}

internal data class CatalogUiState(
    val query: String = "",
    /** null means "All". */
    val surface: SampleSurface? = null,
    val expanded: Set<SampleCategory> = emptySet(),
    val expandedComponents: Set<String> = emptySet(),
    /** Entry ids, most recent first. */
    val recentIds: List<String> = emptyList(),
    val signedIn: Boolean = false,
)

/** A non-blank query flattens everything: no headers, no recents, collapse ignored. */
internal fun buildListItems(
    catalog: List<SampleEntry>,
    state: CatalogUiState,
): List<CatalogListItem> {
    val bySurface = state.surface?.let { wanted -> catalog.filter { it.surface == wanted } } ?: catalog
    val tokens = searchTokens(state.query)

    if (tokens.isNotEmpty()) {
        val matches = bySurface.filter { entry -> tokens.all { it in entry.searchIndex } }
        if (matches.isEmpty()) return listOf(CatalogListItem.Empty(state.query))
        return buildList { matches.forEach { addRow(it, state, categoryLabel = it.category.title) } }
    }

    val items = ArrayList<CatalogListItem>(bySurface.size + SampleCategory.entries.size + 1)

    // Recents are hidden while a surface filter is active, otherwise the strip would contradict it.
    if (state.surface == null) {
        val byId = catalog.associateBy(SampleEntry::id)
        val recents = state.recentIds.asSequence()
            .mapNotNull(byId::get)
            .take(MAX_RECENTS)
            .map { it.toRow(state.signedIn, categoryLabel = null) }
            .toList()
        if (recents.isNotEmpty()) items += CatalogListItem.Recents(recents)
    }

    for (category in SampleCategory.entries) {
        val entries = bySurface.filter { it.category == category }
        if (entries.isEmpty()) continue
        val expanded = category in state.expanded
        items += CatalogListItem.CategoryHeader(category, entries.size, expanded)
        if (expanded) entries.forEach { items.addRow(it, state, categoryLabel = null) }
    }

    if (items.isEmpty()) items += CatalogListItem.Empty(state.query)
    return items
}

/** Counts per surface chip for the current query. The null key is "All". */
internal fun surfaceCounts(
    catalog: List<SampleEntry>,
    query: String,
): Map<SampleSurface?, Int> {
    val tokens = searchTokens(query)
    val matching =
        if (tokens.isEmpty()) catalog
        else catalog.filter { entry -> tokens.all { it in entry.searchIndex } }
    return buildMap {
        put(null, matching.size)
        SampleSurface.entries.forEach { surface ->
            put(surface, matching.count { it.surface == surface })
        }
    }
}

private fun MutableList<CatalogListItem>.addRow(
    entry: SampleEntry,
    state: CatalogUiState,
    categoryLabel: String?,
) {
    val expanded = entry.id in state.expandedComponents
    add(entry.toRow(state.signedIn, categoryLabel, expanded))
    if (expanded) add(CatalogListItem.ComponentHost(entry.id))
}

private fun SampleEntry.toRow(
    signedIn: Boolean,
    categoryLabel: String?,
    expanded: Boolean = false,
) = CatalogListItem.Entry(
    entryId = id,
    title = title,
    description = description,
    surface = surface,
    locked = auth != AuthRequirement.None && !signedIn,
    categoryLabel = categoryLabel,
    expanded = expanded,
)
