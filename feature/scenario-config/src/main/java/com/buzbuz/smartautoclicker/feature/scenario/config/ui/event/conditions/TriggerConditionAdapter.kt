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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.conditions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.*
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemTriggerConditionBinding

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 */
class TriggerConditionAdapter(
    private val conditionClickedListener: (TriggerCondition) -> Unit,
) : ListAdapter<TriggerCondition, TriggerConditionViewHolder>(TriggerConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TriggerConditionViewHolder(
            ItemTriggerConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(holder: TriggerConditionViewHolder, position: Int) {
        holder.onBindCondition((getItem(position)), conditionClickedListener)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [TriggerConditionAdapter] list. */
object TriggerConditionDiffUtilCallback: DiffUtil.ItemCallback<TriggerCondition>() {
    override fun areItemsTheSame(oldItem: TriggerCondition, newItem: TriggerCondition): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: TriggerCondition, newItem: TriggerCondition): Boolean = oldItem == newItem
}

/**
 * View holder displaying a condition in the [TriggerConditionAdapter].
 * @param viewBinding the view binding for this item.
 */
class TriggerConditionViewHolder(
    private val viewBinding: ItemTriggerConditionBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: TriggerCondition, conditionClickedListener: (TriggerCondition) -> Unit) {
        viewBinding.apply {
            conditionName.text = condition.name
            conditionDetails.text = conditionDetails.context.getTriggerConditionDescription(condition)

            conditionTypeIcon.setImageResource(
                when (condition) {
                    is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
                    is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
                    is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
                    else -> throw UnsupportedOperationException("Unsupported condition type")
                }
            )
            root.setOnClickListener { conditionClickedListener(condition) }
        }
    }

    private fun Context.getTriggerConditionDescription(condition: TriggerCondition): String =
        when (condition) {
            is TriggerCondition.OnBroadcastReceived -> getString(
                R.string.item_desc_on_broadcast_received,
                condition.toBroadcastActionDisplayName(),
            )
            is TriggerCondition.OnCounterCountReached -> getString(
                R.string.item_desc_on_counter_reached,
                condition.counterName,
                condition.comparisonOperation.toDisplayName(),
                condition.counterValue,
            )
            is TriggerCondition.OnTimerReached -> getString(
                R.string.item_desc_on_timer_reached,
                formatDuration(condition.durationMs),
            )
            else -> throw UnsupportedOperationException("Scenario Start and End Conditions are not supported here")
        }

    private fun TriggerCondition.OnBroadcastReceived.toBroadcastActionDisplayName(): String {
        val lastDotIndex = intentAction.lastIndexOf('.')

        return if (lastDotIndex != -1 && lastDotIndex != intentAction.lastIndex)
            intentAction.substring(lastDotIndex + 1)
        else intentAction
    }

    private fun TriggerCondition.OnCounterCountReached.ComparisonOperation.toDisplayName(): String =
        when (this) {
            EQUALS -> "="
            LOWER -> "<"
            LOWER_OR_EQUALS -> "<="
            GREATER -> ">"
            GREATER_OR_EQUALS -> ">="
        }
}