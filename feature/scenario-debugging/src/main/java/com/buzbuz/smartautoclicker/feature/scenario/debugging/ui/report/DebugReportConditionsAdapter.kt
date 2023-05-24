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
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.debugging.R
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.ItemDebugReportConditionBinding

import kotlinx.coroutines.Job

/** Adapter for the debug condition reports displayed in a event debug report item. */
class DebugReportConditionsAdapter(
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val conditionClickedListener: (ConditionReport) -> Unit,
) : ListAdapter<ConditionReport, ConditionDebugInfoViewHolder>(ConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ConditionDebugInfoViewHolder(
            ItemDebugReportConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            conditionClickedListener,
        )

    override fun onBindViewHolder(holder: ConditionDebugInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/** DiffUtil Callback comparing two items when updating the [DebugReportConditionsAdapter] list. */
private object ConditionDiffUtilCallback: DiffUtil.ItemCallback<ConditionReport>() {

    override fun areItemsTheSame(oldItem: ConditionReport, newItem: ConditionReport): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ConditionReport, newItem: ConditionReport): Boolean = oldItem == newItem
}

/** ViewHolder for a condition report. */
class ConditionDebugInfoViewHolder(
    private val viewBinding: ItemDebugReportConditionBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionClicked: (ConditionReport) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(conditionReport: ConditionReport) {
        viewBinding.apply {
            root.setOnClickListener { onConditionClicked(conditionReport) }

            conditionName.text = conditionReport.condition.name
            conditionTriggered.text = itemView.context.resources.getString(
                R.string.item_title_debug_report_trigger_processed,
                conditionReport.matchCount,
                conditionReport.processingCount,
            )

            bitmapProvider(conditionReport.condition) { bitmap ->
                if (bitmap != null) {
                    conditionImage.setImageBitmap(bitmap)
                } else {
                    conditionImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                }
            }
        }
    }
}