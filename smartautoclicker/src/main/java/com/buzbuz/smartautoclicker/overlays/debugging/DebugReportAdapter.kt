/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.debugging

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.MergeDebugInfoBinding
import com.buzbuz.smartautoclicker.databinding.MergeDebugTimingBinding
import com.buzbuz.smartautoclicker.databinding.ItemConditionDebugInfoCardBinding
import com.buzbuz.smartautoclicker.databinding.ItemEventDebugInfoCardBinding
import com.buzbuz.smartautoclicker.databinding.ItemScenarioDebugInfoCardBinding

class DebugReportAdapter : ListAdapter<DebugReportItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DebugReportItem.ScenarioReportItem -> R.layout.item_scenario_debug_info_card
            is DebugReportItem.EventReportItem -> R.layout.item_event_debug_info_card
            is DebugReportItem.ConditionReportItem -> R.layout.item_condition_debug_info_card
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_scenario_debug_info_card -> ScenarioDebugInfoViewHolder(
                ItemScenarioDebugInfoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_event_debug_info_card -> EventDebugInfoViewHolder(
                ItemEventDebugInfoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_condition_debug_info_card -> ConditionDebugInfoViewHolder(
                ItemConditionDebugInfoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScenarioDebugInfoViewHolder -> holder.onBind(getItem(position) as DebugReportItem.ScenarioReportItem)
            is EventDebugInfoViewHolder -> holder.onBind(getItem(position) as DebugReportItem.EventReportItem)
            is ConditionDebugInfoViewHolder -> holder.onBind(getItem(position) as DebugReportItem.ConditionReportItem)
        }
    }
}

/** DiffUtil Callback comparing two items when updating the [DebugReportAdapter] list. */
private object DiffUtilCallback: DiffUtil.ItemCallback<DebugReportItem>() {

    override fun areItemsTheSame(oldItem: DebugReportItem, newItem: DebugReportItem): Boolean =
        if (oldItem::class == newItem::class) oldItem.id == newItem.id
        else false

    override fun areContentsTheSame(oldItem: DebugReportItem, newItem: DebugReportItem): Boolean = oldItem == newItem
}

class ScenarioDebugInfoViewHolder(
    private val viewBinding: ItemScenarioDebugInfoCardBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.ScenarioReportItem) {
        viewBinding.apply {
            title.text = item.name
            durationRoot.setValue(
                R.string.dialog_debug_report_total_duration,
                item.duration,
            )
            imgProcCountRoot.setValue(
                R.string.dialog_debug_report_image_processed,
                item.imageProcessed,
            )
            avgImgProcDurRoot.setValue(
                R.string.dialog_debug_report_avg_image_processing_duration,
                item.averageImageProcessingTime,
            )
            evtTriggerCountRoot.setValue(
                R.string.dialog_debug_report_total_event_trigger_count,
                item.eventsTriggered,
            )
            condTriggerCountRoot.setValue(
                R.string.dialog_debug_report_detection_count,
                item.conditionsDetected,
            )
        }
    }
}

class EventDebugInfoViewHolder(
    private val viewBinding: ItemEventDebugInfoCardBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.EventReportItem) {
        viewBinding.apply {
            title.text = item.name
            triggerCountRoot.setValue(
                R.string.dialog_debug_report_event_trigger_count,
                item.triggerCount,
            )
            processedCountRoot.setValue(
                R.string.dialog_debug_report_event_processing_count,
                item.processingCount,
            )
            processingTimingRoot.setValues(
                item.minProcessingDuration,
                item.avgProcessingDuration,
                item.maxProcessingDuration,
            )
        }
    }
}

class ConditionDebugInfoViewHolder(
    private val viewBinding: ItemConditionDebugInfoCardBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.ConditionReportItem) {
        viewBinding.apply {
            title.text = item.name
            matchCountRoot.setValue(
                R.string.dialog_debug_report_event_trigger_count,
                item.matchCount,
            )
            processedCountRoot.setValue(
                R.string.dialog_debug_report_event_processing_count,
                item.processingCount,
            )
            processingTimingRoot.setValues(
                item.minProcessingDuration,
                item.avgProcessingDuration,
                item.maxProcessingDuration,
            )
        }
    }
}

private fun MergeDebugInfoBinding.setValue(@StringRes desc: Int, v: String) {
    description.setText(desc)
    value.text = v
}

private fun MergeDebugTimingBinding.setValues(min: String, avg: String, max: String) {
    minValue.text = min
    avgValue.text = avg
    maxValue.text = max
}