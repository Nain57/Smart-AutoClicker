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
package com.buzbuz.smartautoclicker.activity.list

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemEventCardBinding
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.activity.list.ScenarioListUiState.Item.Valid.Smart.EventItem

import kotlinx.coroutines.Job

class ScenarioEventsAdapter(
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<EventItem, EventCardViewHolder>(EventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventCardViewHolder =
        EventCardViewHolder(
            ItemEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
        )

    override fun onBindViewHolder(holder: EventCardViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onViewRecycled(holder: EventCardViewHolder) {
        holder.onUnbind()
    }
}

/** DiffUtil Callback comparing two EventItem when updating the [ScenarioEventsAdapter] list. */
object EventDiffUtilCallback: DiffUtil.ItemCallback<EventItem>() {
    override fun areItemsTheSame(oldItem: EventItem, newItem: EventItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: EventItem, newItem: EventItem): Boolean =
        oldItem == newItem
}

class EventCardViewHolder(
    private val viewBinding: ItemEventCardBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** The job for the condition bitmap loading. */
    private var bitmapJob: Job? = null

    fun onBind(item: EventItem) {
        viewBinding.apply {
            eventName.text = item.eventName
            eventActionsCount.text = item.actionsCount.toString()
            eventConditionsCount.text = item.conditionsCount.toString()

            if (item.firstCondition == null) {
                setErrorBitmap()
                return
            }

            bitmapJob = bitmapProvider(item.firstCondition) { bitmap ->
                if (bitmap != null) {
                    conditionImage.setImageBitmap(bitmap)
                } else {
                   setErrorBitmap()
                }
            }
        }
    }

    fun onUnbind() {
        bitmapJob?.cancel()
        bitmapJob = null
    }

    private fun setErrorBitmap() {
        viewBinding.conditionImage.setImageDrawable(
            ContextCompat.getDrawable(viewBinding.root.context, R.drawable.ic_cancel)?.apply {
                setTint(Color.RED)
            }
        )
    }
}