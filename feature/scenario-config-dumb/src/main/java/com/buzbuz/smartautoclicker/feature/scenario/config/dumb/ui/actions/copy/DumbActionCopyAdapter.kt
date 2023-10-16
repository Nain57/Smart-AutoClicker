/*
 * Copyright (C) 2023 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.copy

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.databinding.ItemCopyHeaderBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.ItemDumbActionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.onBind

/**
 * Adapter displaying all actions in a list.
 * @param onActionSelected Called when the user presses an action.
 */
class DumbActionCopyAdapter(
    private val onActionSelected: (DumbActionCopyItem.DumbActionItem) -> Unit
): ListAdapter<DumbActionCopyItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when(getItem(position)) {
            is DumbActionCopyItem.HeaderItem -> R.layout.item_copy_header
            is DumbActionCopyItem.DumbActionItem -> R.layout.item_dumb_action
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_copy_header -> HeaderViewHolder(
                ItemCopyHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_dumb_action -> DumbActionViewHolder(
                ItemDumbActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as DumbActionCopyItem.HeaderItem)
            is DumbActionViewHolder -> holder.onBind(getItem(position) as DumbActionCopyItem.DumbActionItem, onActionSelected)
        }
    }
}

/** DiffUtil Callback comparing two items when updating the [DumbActionCopyAdapter] list. */
object DiffUtilCallback: DiffUtil.ItemCallback<DumbActionCopyItem>(){
    override fun areItemsTheSame(oldItem: DumbActionCopyItem, newItem: DumbActionCopyItem): Boolean =
        when {
            oldItem is DumbActionCopyItem.HeaderItem && newItem is DumbActionCopyItem.HeaderItem -> true
            oldItem is DumbActionCopyItem.DumbActionItem && newItem is DumbActionCopyItem.DumbActionItem ->
                oldItem.dumbActionDetails.action.id == newItem.dumbActionDetails.action.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: DumbActionCopyItem, newItem: DumbActionCopyItem): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying a header in the [DumbActionCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class HeaderViewHolder(
    private val viewBinding: ItemCopyHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: DumbActionCopyItem.HeaderItem) {
        viewBinding.textHeader.setText(header.title)
    }
}

/**
 * View holder displaying an action in the [DumbActionCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
class DumbActionViewHolder(private val viewBinding: ItemDumbActionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param item the item to be represented by this view holder.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(item: DumbActionCopyItem.DumbActionItem, actionClickedListener: (DumbActionCopyItem.DumbActionItem) -> Unit) {
        viewBinding.onBind(item.dumbActionDetails) {
            actionClickedListener(item)
        }
    }
}