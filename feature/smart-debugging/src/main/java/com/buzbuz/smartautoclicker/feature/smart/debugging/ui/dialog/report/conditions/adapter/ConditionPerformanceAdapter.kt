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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.ui.utils.setColorIndicatorDrawable
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemConditionPerformanceBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemConditionPerformanceFooterBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.ConditionPerformanceEntry
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.formatAverageDuration
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.formatCount
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.formatPercentage
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions.formatTotalDuration
import kotlinx.coroutines.Job

internal sealed interface ConditionPerformanceListItem {
    data class Condition(val entry: ConditionPerformanceEntry) : ConditionPerformanceListItem
    data object Footer : ConditionPerformanceListItem
}

internal class ConditionPerformanceAdapter(
    private val bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job,
) : ListAdapter<ConditionPerformanceListItem, RecyclerView.ViewHolder>(DiffCallback) {

    fun submitEntries(entries: List<ConditionPerformanceEntry>, commitCallback: (() -> Unit)? = null) {
        submitList(entries.map(ConditionPerformanceListItem::Condition) + ConditionPerformanceListItem.Footer, commitCallback)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ConditionPerformanceListItem.Condition -> R.layout.item_condition_performance
        ConditionPerformanceListItem.Footer -> R.layout.item_condition_performance_footer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_condition_performance -> ConditionViewHolder(parent, bitmapProvider)
            R.layout.item_condition_performance_footer -> FooterViewHolder(parent)
            else -> error("Unknown condition performance view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ConditionViewHolder) {
            holder.bind((getItem(position) as ConditionPerformanceListItem.Condition).entry)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ConditionViewHolder) holder.unbind()
        super.onViewRecycled(holder)
    }
}

private class ConditionViewHolder private constructor(
    private val binding: ItemConditionPerformanceBinding,
    private val bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job,
) : RecyclerView.ViewHolder(binding.root) {

    constructor(
        parent: ViewGroup,
        bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job,
    ) : this(
        ItemConditionPerformanceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        bitmapProvider,
    )

    private var bitmapLoadingJob: Job? = null

    fun bind(entry: ConditionPerformanceEntry) = binding.apply {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
        conditionImage.setImageDrawable(null)
        conditionNameText.text = entry.condition.name
        eventNameText.text = entry.eventName
        totalTimeText.text = root.context.getString(
            R.string.item_condition_performance_total_time,
            formatTotalDuration(entry.totalDurationNs),
        )

        val fulfilledCount = formatCount(entry.fulfilledCount)
        val checkCount = formatCount(entry.checkCount)
        fulfilledText.text = root.context.getString(
            R.string.item_condition_performance_fulfilled,
            fulfilledCount,
            root.resources.getQuantityString(R.plurals.item_condition_performance_time, entry.fulfilledCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
            checkCount,
            root.resources.getQuantityString(R.plurals.item_condition_performance_check, entry.checkCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
        )
        averageText.text = formatAverageDuration(entry.totalDurationNs, entry.checkCount)?.let { average ->
            root.context.getString(R.string.item_condition_performance_average, average)
        } ?: root.context.getString(R.string.item_condition_performance_average_unavailable)
        percentageText.text = formatPercentage(entry.totalDurationNs, entry.totalMeasuredDurationNs)

        when (val condition = entry.condition) {
            is ScreenCondition.Color -> conditionImage.setColorIndicatorDrawable(condition.color)
            is ScreenCondition.Image -> bitmapLoadingJob = bitmapProvider(condition) { bitmap ->
                if (bitmap != null) conditionImage.setImageBitmap(bitmap)
                else conditionImage.setImageDrawable(ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                    setTint(Color.RED)
                })
            }
            is ScreenCondition.Number -> conditionImage.setImageResource(R.drawable.ic_number_condition)
            is ScreenCondition.Text -> conditionImage.setImageResource(R.drawable.ic_text_condition)
            is TriggerCondition.OnBroadcastReceived -> conditionImage.setImageResource(R.drawable.ic_broadcast_received)
            is TriggerCondition.OnCounterCountReached -> conditionImage.setImageResource(R.drawable.ic_counter_reached)
            is TriggerCondition.OnTimerReached -> conditionImage.setImageResource(R.drawable.ic_timer_reached)
        }
    }

    fun unbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

private class FooterViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    ItemConditionPerformanceFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false).root,
)

private object DiffCallback : DiffUtil.ItemCallback<ConditionPerformanceListItem>() {
    override fun areItemsTheSame(
        oldItem: ConditionPerformanceListItem,
        newItem: ConditionPerformanceListItem,
    ): Boolean = when {
        oldItem is ConditionPerformanceListItem.Condition && newItem is ConditionPerformanceListItem.Condition ->
            oldItem.entry.condition.id == newItem.entry.condition.id
        oldItem is ConditionPerformanceListItem.Footer && newItem is ConditionPerformanceListItem.Footer -> true
        else -> false
    }

    override fun areContentsTheSame(
        oldItem: ConditionPerformanceListItem,
        newItem: ConditionPerformanceListItem,
    ): Boolean = oldItem == newItem
}
