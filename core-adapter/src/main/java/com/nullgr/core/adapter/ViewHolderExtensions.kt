@file:Suppress("NOTHING_TO_INLINE")

package com.nullgr.core.adapter

import androidx.recyclerview.widget.RecyclerView
import com.nullgr.core.adapter.items.*

/**
 * Extension function that allows to get items from adapter via ViewHolder.
 */
inline fun RecyclerView.ViewHolder.items(): List<ListItem>? {
    val parent = itemView.parent as? RecyclerView ?: return null
    val adapter = parent.adapter as? DynamicAdapter ?: return null
    return adapter.items
}

/**
 * Extension function that invoke given [block] if position not equals [RecyclerView.NO_POSITION]
 */
inline fun <reified T : ListItem> RecyclerView.ViewHolder.withAdapterPosition(
    block: (item: T, position: Int) -> Unit
) {
    with(adapterPosition) {
        if (this != RecyclerView.NO_POSITION) {
            val items = items()
            if (items != null && this >= 0 && this < items.size) {
                block.invoke(items[this] as T, this)
            }
        }
    }
}