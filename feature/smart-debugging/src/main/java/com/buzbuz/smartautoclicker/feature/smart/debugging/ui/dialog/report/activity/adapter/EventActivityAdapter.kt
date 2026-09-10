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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemEventActivityBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemEventStateHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.EventActivityListItem
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.EventActivityType


class EventActivityAdapter : ListAdapter<EventActivityListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is EventActivityListItem.Header -> R.layout.item_event_state_header
        is EventActivityListItem.Event -> R.layout.item_event_activity
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_event_state_header -> HeaderViewHolder(parent)
            R.layout.item_event_activity -> EventViewHolder(parent)
            else -> error("Unknown Event Activity view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(getItem(position) as EventActivityListItem.Header)
            is EventViewHolder -> holder.bind(getItem(position) as EventActivityListItem.Event)
        }
    }
}

private class HeaderViewHolder private constructor(
    private val binding: ItemEventStateHeaderBinding,
) : RecyclerView.ViewHolder(binding.root) {

    constructor(parent: ViewGroup) : this(
        ItemEventStateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    fun bind(item: EventActivityListItem.Header) {
        when (item.type) {
            EventActivityType.SCREEN -> {
                binding.sectionText.setText(R.string.item_event_activity_screen_events)
                binding.sectionIcon.setImageResource(R.drawable.ic_screen_event)
            }
            EventActivityType.TRIGGER -> {
                binding.sectionText.setText(R.string.item_event_activity_trigger_events)
                binding.sectionIcon.setImageResource(R.drawable.ic_trigger_event)
            }
        }
    }
}

private class EventViewHolder private constructor(
    private val binding: ItemEventActivityBinding,
) : RecyclerView.ViewHolder(binding.root) {

    constructor(parent: ViewGroup) : this(
        ItemEventActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    fun bind(item: EventActivityListItem.Event) {
        val activity = item.activity
        binding.eventNameText.text = activity.name
        binding.occurrenceCountText.text = binding.root.context.getString(
            R.string.item_event_activity_occurrence_count,
            activity.occurrenceCount,
        )

        val alpha = if (activity.occurrenceCount == 0) UNREACHED_ALPHA else 1f
        binding.eventNameText.alpha = alpha
        binding.occurrenceCountText.alpha = alpha
    }
}

private object DiffCallback : DiffUtil.ItemCallback<EventActivityListItem>() {
    override fun areItemsTheSame(oldItem: EventActivityListItem, newItem: EventActivityListItem): Boolean =
        when {
            oldItem is EventActivityListItem.Header && newItem is EventActivityListItem.Header ->
                oldItem.type == newItem.type
            oldItem is EventActivityListItem.Event && newItem is EventActivityListItem.Event ->
                oldItem.activity.key == newItem.activity.key
            else -> false
        }

    override fun areContentsTheSame(oldItem: EventActivityListItem, newItem: EventActivityListItem): Boolean =
        oldItem == newItem
}

private const val UNREACHED_ALPHA = 0.6f
