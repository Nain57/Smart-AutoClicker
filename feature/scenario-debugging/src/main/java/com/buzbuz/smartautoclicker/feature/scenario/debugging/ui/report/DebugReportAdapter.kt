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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.debugging.R
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.ItemDebugReportEventBinding
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.ItemDebugReportScenarioBinding

import kotlinx.coroutines.Job

/** Manages the items displayed in the [DebugReportDialog]. */
class DebugReportAdapter(
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionClicked: (ConditionReport) -> Unit,
) : ListAdapter<DebugReportItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DebugReportItem.ScenarioReportItem -> R.layout.item_debug_report_scenario
            is DebugReportItem.EventReportItem -> R.layout.item_debug_report_event
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_debug_report_scenario -> ScenarioDebugInfoViewHolder(
                ItemDebugReportScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

            R.layout.item_debug_report_event -> EventDebugInfoViewHolder(
                ItemDebugReportEventBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider,
                onConditionClicked,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScenarioDebugInfoViewHolder -> holder.onBind(getItem(position) as DebugReportItem.ScenarioReportItem)
            is EventDebugInfoViewHolder -> holder.onBind(getItem(position) as DebugReportItem.EventReportItem)
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

/** ViewHolder for the debug report of a scenario. */
class ScenarioDebugInfoViewHolder(
    private val viewBinding: ItemDebugReportScenarioBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.ScenarioReportItem) {
        viewBinding.apply {
            textScenarioName.text = item.name
            rootTotalDuration.setValue(
                R.string.item_title_report_total_duration,
                item.duration,
            )
            rootImgProcCount.setValue(
                R.string.item_title_report_image_processed,
                item.imageProcessed,
            )
            rootAvgImgProcDur.setValue(
                R.string.item_title_report_avg_image_processing_duration,
                item.averageImageProcessingTime,
            )
            rootEvtTriggerCount.setValue(
                R.string.item_title_report_total_event_trigger_count,
                item.eventsTriggered,
            )
            rootCondTriggerCount.setValue(
                R.string.item_title_report_detection_count,
                item.conditionsDetected,
            )
        }
    }
}

/** ViewHolder for the debug report of an event. */
class EventDebugInfoViewHolder(
    private val viewBinding: ItemDebugReportEventBinding,
    bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    onConditionClicked: (ConditionReport) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    private val conditionReportsAdapter = DebugReportConditionsAdapter(
        bitmapProvider,
        onConditionClicked,
    )

    init {
        viewBinding.listConditions.adapter = conditionReportsAdapter
    }

    fun onBind(item: DebugReportItem.EventReportItem) {
        viewBinding.apply {
            textEventName.text = item.name

            triggerCountRoot.setValues(
                R.string.section_title_report_event_trigger_count,
                item.triggerCount,
                R.string.section_title_report_event_processing_count,
                item.processingCount,
            )

            processingTimingRoot.setValues(
                R.string.section_title_report_timing_title,
                item.minProcessingDuration,
                item.avgProcessingDuration,
                item.maxProcessingDuration,
            )

            conditionReportsAdapter.submitList(item.conditionReports)
        }
    }
}