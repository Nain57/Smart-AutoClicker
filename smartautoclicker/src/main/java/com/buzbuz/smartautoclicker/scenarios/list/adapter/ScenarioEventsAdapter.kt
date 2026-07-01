/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.scenarios.list.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.utils.setColorIndicatorDrawable
import com.buzbuz.smartautoclicker.databinding.ItemEventCardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState.Item.ScenarioItem.Valid.Smart.EventItem

import kotlinx.coroutines.Job

import java.util.Locale

class ScenarioEventsAdapter(
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
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
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** The job for the condition bitmap loading. */
    private var bitmapJob: Job? = null

    fun onBind(item: EventItem) {
        viewBinding.apply {
            eventName.text = item.eventName
            eventActionsCount.text = String.format(Locale.getDefault(), "%d", item.actionsCount)
            eventConditionsCount.text = String.format(Locale.getDefault(), "%d", item.conditionsCount)

            val condition = item.firstCondition
            if (condition == null) {
                setErrorBitmap()
                return
            }

            when (condition) {
                is ScreenCondition.Color -> {
                    conditionImage.visibility = View.VISIBLE
                    conditionText.visibility = View.GONE
                    conditionImage.setColorIndicatorDrawable(condition.color)
                }
                is ScreenCondition.Image -> {
                    conditionImage.visibility = View.VISIBLE
                    conditionText.visibility = View.GONE
                    bitmapJob = bitmapProvider(condition) { bitmap ->
                        if (bitmap != null) {
                            conditionImage.setImageBitmap(bitmap)
                        } else {
                            setErrorBitmap()
                        }
                    }
                }

                is ScreenCondition.Number -> {
                    conditionImage.visibility = View.GONE
                    conditionText.visibility = View.VISIBLE

                    conditionText.text = condition.comparisonOperation
                        .toEffectDescription(root.context, operand = condition.counterValue.toNaturalDisplayString())
                }

                is ScreenCondition.Text -> {
                    conditionImage.visibility = View.GONE
                    conditionText.visibility = View.VISIBLE

                    conditionText.text = condition.text
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
