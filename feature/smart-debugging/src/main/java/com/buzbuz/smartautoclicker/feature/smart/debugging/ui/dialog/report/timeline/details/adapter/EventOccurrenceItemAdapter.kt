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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details.adapter

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details.EventOccurrenceItem

import kotlinx.coroutines.Job

class EventOccurrenceItemAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<EventOccurrenceItem, RecyclerView.ViewHolder>(EventOccurrenceDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is EventOccurrenceItem.Header -> R.layout.item_condition_result_header
            is EventOccurrenceItem.Image -> R.layout.item_condition_result_image
            is EventOccurrenceItem.Trigger -> R.layout.item_condition_result_trigger
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_condition_result_header -> ItemEventOccurrenceHeaderViewHolder(parent)
            R.layout.item_condition_result_image -> ItemEventOccurrenceImageViewHolder(parent, bitmapProvider)
            R.layout.item_condition_result_trigger -> ItemEventOccurrenceTriggerViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemEventOccurrenceHeaderViewHolder -> holder.bind(getItem(position) as EventOccurrenceItem.Header)
            is ItemEventOccurrenceImageViewHolder -> holder.bind(getItem(position) as EventOccurrenceItem.Image)
            is ItemEventOccurrenceTriggerViewHolder -> holder.bind(getItem(position) as EventOccurrenceItem.Trigger)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is ItemEventOccurrenceImageViewHolder -> holder.unbind()
            is ItemEventOccurrenceHeaderViewHolder,
            is ItemEventOccurrenceTriggerViewHolder -> Unit
        }
    }
}

private object EventOccurrenceDiffUtilCallback : DiffUtil.ItemCallback<EventOccurrenceItem>() {

    override fun areItemsTheSame(oldItem: EventOccurrenceItem, newItem: EventOccurrenceItem): Boolean =
        when {
            oldItem is EventOccurrenceItem.Header && newItem is EventOccurrenceItem.Header -> true
            oldItem is EventOccurrenceItem.Image && newItem is EventOccurrenceItem.Image -> oldItem.id == newItem.id
            oldItem is EventOccurrenceItem.Trigger && newItem is EventOccurrenceItem.Trigger -> oldItem.id == newItem.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: EventOccurrenceItem, newItem: EventOccurrenceItem): Boolean =
        oldItem == newItem

}