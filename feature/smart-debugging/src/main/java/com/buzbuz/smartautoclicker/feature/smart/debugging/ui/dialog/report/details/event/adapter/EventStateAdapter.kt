/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.event.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.event.DebugEventStateItem


class EventStateAdapter : ListAdapter<DebugEventStateItem, RecyclerView.ViewHolder>(EventStateDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DebugEventStateItem.Header -> R.layout.item_event_state_header
            is DebugEventStateItem.EventState -> R.layout.item_event_state_event
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_event_state_header -> EventStateHeaderViewHolder(parent)
            R.layout.item_event_state_event -> EventStateEventViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventStateHeaderViewHolder -> holder.bind(getItem(position) as DebugEventStateItem.Header)
            is EventStateEventViewHolder -> holder.bind(getItem(position) as DebugEventStateItem.EventState)
        }
    }
}

private object EventStateDiffUtilCallback : DiffUtil.ItemCallback<DebugEventStateItem>() {

    override fun areItemsTheSame(oldItem: DebugEventStateItem, newItem: DebugEventStateItem): Boolean =
        when {
            oldItem is DebugEventStateItem.Header && newItem is DebugEventStateItem.Header ->
                true
            oldItem is DebugEventStateItem.EventState && newItem is DebugEventStateItem.EventState ->
                oldItem.eventId == newItem.eventId
            else ->
                false
        }

    override fun areContentsTheSame(oldItem: DebugEventStateItem, newItem: DebugEventStateItem): Boolean =
        oldItem == newItem

}