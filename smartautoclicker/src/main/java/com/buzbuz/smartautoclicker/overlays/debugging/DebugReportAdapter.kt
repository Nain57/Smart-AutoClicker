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
import android.view.View
import android.view.ViewGroup

import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.IncludeDebugCollapsibleHeaderBinding
import com.buzbuz.smartautoclicker.databinding.IncludeDebugInfoBinding
import com.buzbuz.smartautoclicker.databinding.IncludeDebugMinAvgMaxBinding
import com.buzbuz.smartautoclicker.databinding.IncludeDebugTriggeredProcessedBinding
import com.buzbuz.smartautoclicker.databinding.ItemConditionDebugInfoBinding
import com.buzbuz.smartautoclicker.databinding.ItemEventDebugInfoBinding
import com.buzbuz.smartautoclicker.databinding.ItemScenarioDebugInfoBinding

class DebugReportAdapter(
    private val onCollapseExpandEvent: (eventId: Long) -> Unit,
    private val onCollapseExpandCondition: (conditionId: Long) -> Unit,
) : ListAdapter<DebugReportItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DebugReportItem.ScenarioReportItem -> R.layout.item_scenario_debug_info
            is DebugReportItem.EventReportItem -> R.layout.item_event_debug_info
            is DebugReportItem.ConditionReportItem -> R.layout.item_condition_debug_info
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_scenario_debug_info -> ScenarioDebugInfoViewHolder(
                ItemScenarioDebugInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

            R.layout.item_event_debug_info -> EventDebugInfoViewHolder(
                ItemEventDebugInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onCollapseExpandEvent,
            )

            R.layout.item_condition_debug_info -> ConditionDebugInfoViewHolder(
                ItemConditionDebugInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onCollapseExpandCondition,
            )
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
    private val viewBinding: ItemScenarioDebugInfoBinding,
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
    private val viewBinding: ItemEventDebugInfoBinding,
    private val onCollapseExpandEvent: (eventId: Long) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.EventReportItem) {
        viewBinding.apply {
            header.apply {
                setValues(item.name, item.triggerCount, item.processingCount, item.isExpanded)
                root.setOnClickListener { onCollapseExpandEvent(item.id) }
            }

            if (item.isExpanded) {
                triggerCountRoot.apply {
                    setValues(
                        R.string.dialog_debug_report_event_trigger_count,
                        item.triggerCount,
                        R.string.dialog_debug_report_event_processing_count,
                        item.processingCount,
                    )
                }

                processingTimingRoot.setValues(
                    R.string.dialog_debug_report_timing_title,
                    item.minProcessingDuration,
                    item.avgProcessingDuration,
                    item.maxProcessingDuration,
                )

                collapsibleLayout.visibility = View.VISIBLE
            } else {
                collapsibleLayout.visibility = View.GONE
            }
        }
    }
}

class ConditionDebugInfoViewHolder(
    private val viewBinding: ItemConditionDebugInfoBinding,
    private val onCollapseExpandCondition: (eventId: Long) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: DebugReportItem.ConditionReportItem) {
        viewBinding.apply {
            header.apply {
                setValues(item.name, item.matchCount, item.processingCount, item.isExpanded)
                root.setOnClickListener { onCollapseExpandCondition(item.id) }
            }

            if (item.isExpanded) {
                triggerCountRoot.apply {
                    setValues(
                        R.string.dialog_debug_report_condition_detected_count,
                        item.matchCount,
                        R.string.dialog_debug_report_condition_processing_count,
                        item.processingCount,
                    )
                }

                processingTimingRoot.setValues(
                    R.string.dialog_debug_report_timing_title,
                    item.minProcessingDuration,
                    item.avgProcessingDuration,
                    item.maxProcessingDuration,
                )

                confidenceRoot.setValues(
                    R.string.dialog_debug_report_confidence_title,
                    item.minConfidence,
                    item.avgConfidence,
                    item.maxConfidence,
                )

                collapsibleLayout.visibility = View.VISIBLE
            } else {
                collapsibleLayout.visibility = View.GONE
            }
        }
    }
}

private fun IncludeDebugCollapsibleHeaderBinding.setValues(
    title: String,
    leftVal: String,
    rightVal: String,
    isExpanded: Boolean,
) {
    name.text = title
    triggeredProcessed.apply {
        text = triggeredProcessed.context.getString(
            R.string.dialog_debug_report_trigger_processed,
            leftVal,
            rightVal,
        )
        visibility = if (isExpanded) View.GONE else View.VISIBLE
    }
    collapseExpand.setImageResource(
        if (isExpanded) R.drawable.ic_chevron_top
        else R.drawable.ic_chevron_bottom
    )
}

private fun IncludeDebugInfoBinding.setValue(@StringRes desc: Int, v: String) {
    description.setText(desc)
    value.text = v
}

private fun IncludeDebugTriggeredProcessedBinding.setValues(
    @StringRes leftDesc: Int,
    leftVal: String,
    @StringRes rightDesc: Int,
    rightVal: String,
) {
    triggeredTitle.setText(leftDesc)
    triggeredCount.text = leftVal
    processedTitle.setText(rightDesc)
    processedCount.text = rightVal
}

private fun IncludeDebugMinAvgMaxBinding.setValues(@StringRes descId: Int, min: String, avg: String, max: String) {
    description.setText(descId)
    minValue.text = min
    avgValue.text = avg
    maxValue.text = max
}