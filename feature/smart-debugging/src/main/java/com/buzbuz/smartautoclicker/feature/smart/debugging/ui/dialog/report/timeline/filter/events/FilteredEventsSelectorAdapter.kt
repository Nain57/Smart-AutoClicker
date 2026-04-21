/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemEventFilteredStateBinding

class FilteredEventsSelectorAdapter(
    private val onItemClicked: (Long, Boolean) -> Unit,
) : ListAdapter<FilteredEventsSelectorItem, FilteredEventsSelectorItemViewHolder>(FilteredEventsSelectorItemDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilteredEventsSelectorItemViewHolder =
        FilteredEventsSelectorItemViewHolder(
            viewBinding = ItemEventFilteredStateBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClicked = onItemClicked,
        )

    override fun onBindViewHolder(holder: FilteredEventsSelectorItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object FilteredEventsSelectorItemDiffUtilCallback: DiffUtil.ItemCallback<FilteredEventsSelectorItem>() {
    override fun areItemsTheSame(
        oldItem: FilteredEventsSelectorItem,
        newItem: FilteredEventsSelectorItem,
    ): Boolean = oldItem.eventId == newItem.eventId

    override fun areContentsTheSame(
        oldItem: FilteredEventsSelectorItem,
        newItem: FilteredEventsSelectorItem,
    ): Boolean = oldItem == newItem
}

class FilteredEventsSelectorItemViewHolder(
    private val viewBinding: ItemEventFilteredStateBinding,
    private val onItemClicked: (id: Long, state: Boolean) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(item: FilteredEventsSelectorItem) {
        viewBinding.apply {
            root.setOnItemClickedListener(item, onItemClicked)
            eventNameText.text = item.eventName

            stateCheckbox.apply {
                isChecked = item.eventState
                setOnItemClickedListener(item, onItemClicked)
            }
        }
    }

    private fun View.setOnItemClickedListener(item: FilteredEventsSelectorItem, onItemClicked: (id: Long, state: Boolean) -> Unit) {
        setOnClickListener { onItemClicked(item.eventId, !item.eventState) }
    }
}