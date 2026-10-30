package com.lucrasports.sdk.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.app.catalog.CatalogListItem
import com.lucrasports.sdk.app.catalog.SampleCategory
import com.lucrasports.sdk.app.catalog.SampleSurface

private const val TYPE_RECENTS = 0
private const val TYPE_HEADER = 1
private const val TYPE_ENTRY = 2
private const val TYPE_EMPTY = 3
private const val TYPE_COMPONENT_HOST = 4

/** Selection is dispatched by entry id; see [CatalogListItem.Entry] for why items hold no
 *  [com.lucrasports.sdk.app.catalog.SampleEntry]. */
internal class SampleCatalogAdapter(
    private val onEntryClick: (entryId: String) -> Unit,
    private val onCategoryClick: (SampleCategory) -> Unit,
    private val onClearSearch: () -> Unit,
    /** The live SDK view for an expanded component, owned by the Activity so it outlives binding. */
    private val hostedComponent: (entryId: String) -> View?,
) : ListAdapter<CatalogListItem, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is CatalogListItem.Recents -> TYPE_RECENTS
        is CatalogListItem.CategoryHeader -> TYPE_HEADER
        is CatalogListItem.Entry -> TYPE_ENTRY
        is CatalogListItem.ComponentHost -> TYPE_COMPONENT_HOST
        is CatalogListItem.Empty -> TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_RECENTS -> RecentsHolder(
                inflater.inflate(R.layout.item_catalog_recents, parent, false), onEntryClick
            )

            TYPE_HEADER -> HeaderHolder(
                inflater.inflate(R.layout.item_catalog_header, parent, false), onCategoryClick
            )

            TYPE_ENTRY -> EntryHolder(
                inflater.inflate(R.layout.item_catalog_entry, parent, false), onEntryClick
            )

            TYPE_COMPONENT_HOST -> ComponentHostHolder(
                inflater.inflate(R.layout.item_catalog_component_host, parent, false),
                hostedComponent,
            )

            else -> EmptyHolder(
                inflater.inflate(R.layout.item_catalog_empty, parent, false), onClearSearch
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CatalogListItem.Recents -> (holder as RecentsHolder).bind(item)
            is CatalogListItem.CategoryHeader -> (holder as HeaderHolder).bind(item)
            is CatalogListItem.Entry -> (holder as EntryHolder).bind(item)
            is CatalogListItem.ComponentHost -> (holder as ComponentHostHolder).bind(item)
            is CatalogListItem.Empty -> (holder as EmptyHolder).bind(item)
        }
    }

    private object Diff : DiffUtil.ItemCallback<CatalogListItem>() {
        override fun areItemsTheSame(oldItem: CatalogListItem, newItem: CatalogListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CatalogListItem, newItem: CatalogListItem) =
            oldItem == newItem
    }

    private class EntryHolder(
        itemView: View,
        onEntryClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.entry_title)
        private val subtitle: TextView = itemView.findViewById(R.id.entry_subtitle)
        private val lock: ImageView = itemView.findViewById(R.id.entry_lock)
        private val badge: TextView = itemView.findViewById(R.id.entry_badge)
        private val chevron: ImageView = itemView.findViewById(R.id.entry_chevron)
        private var entryId: String? = null

        init {
            itemView.setOnClickListener { entryId?.let(onEntryClick) }
        }

        fun bind(item: CatalogListItem.Entry) {
            entryId = item.entryId
            title.text = item.title
            lock.isVisible = item.locked
            // Search results are shown outside their section, so the category places them.
            subtitle.text = item.categoryLabel ?: item.description
            subtitle.isVisible = subtitle.text.isNotEmpty()
            badge.applySurface(item.surface)
            chevron.isVisible = item.surface == SampleSurface.Ui
            chevron.rotation = if (item.expanded) 180f else 0f
        }
    }

    private class HeaderHolder(
        itemView: View,
        onCategoryClick: (SampleCategory) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.header_category_title)
        private val count: TextView = itemView.findViewById(R.id.header_category_count)
        private val chevron: ImageView = itemView.findViewById(R.id.header_category_chevron)
        private var category: SampleCategory? = null

        init {
            itemView.setOnClickListener { category?.let(onCategoryClick) }
        }

        fun bind(item: CatalogListItem.CategoryHeader) {
            category = item.category
            title.text = item.category.title
            count.text = item.count.toString()
            chevron.rotation = if (item.expanded) 180f else 0f
        }
    }

    private class RecentsHolder(
        itemView: View,
        private val onEntryClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val row: LinearLayout = itemView.findViewById(R.id.recents_row)

        fun bind(item: CatalogListItem.Recents) {
            row.removeAllViews()
            val inflater = LayoutInflater.from(row.context)
            item.entries.forEach { entry ->
                val chip = inflater.inflate(R.layout.item_catalog_recent_chip, row, false) as TextView
                chip.text = entry.title
                chip.setOnClickListener { onEntryClick(entry.entryId) }
                row.addView(chip)
            }
        }
    }

    /** A View has one parent, so the Activity-owned component is pulled off the previous holder
     *  before being added here. */
    private class ComponentHostHolder(
        itemView: View,
        private val hostedComponent: (String) -> View?,
    ) : RecyclerView.ViewHolder(itemView) {

        private val host: FrameLayout = itemView.findViewById(R.id.component_host)

        fun bind(item: CatalogListItem.ComponentHost) {
            host.removeAllViews()
            val view = hostedComponent(item.entryId) ?: return
            (view.parent as? ViewGroup)?.removeView(view)
            host.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    private class EmptyHolder(
        itemView: View,
        onClearSearch: () -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.empty_title)

        init {
            itemView.findViewById<Button>(R.id.empty_clear).setOnClickListener { onClearSearch() }
        }

        fun bind(item: CatalogListItem.Empty) {
            title.text = itemView.context.getString(R.string.catalog_no_matches, item.query)
        }
    }
}

private fun TextView.applySurface(surface: SampleSurface) {
    val (textColor, backgroundColor) = when (surface) {
        SampleSurface.Flow -> R.color.badge_flow_text to R.color.badge_flow_bg
        SampleSurface.Api -> R.color.badge_api_text to R.color.badge_api_bg
        SampleSurface.Ui -> R.color.badge_ui_text to R.color.badge_ui_bg
    }
    text = surface.badge
    setTextColor(ContextCompat.getColor(context, textColor))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, backgroundColor))
}
