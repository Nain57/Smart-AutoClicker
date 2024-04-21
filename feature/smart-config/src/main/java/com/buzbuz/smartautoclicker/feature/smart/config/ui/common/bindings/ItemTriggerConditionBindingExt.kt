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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.content.Context
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.*
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerConditionBinding

/**
 * Bind this view holder as a condition item.
 *
 * @param condition the condition to be represented by this item.
 * @param conditionClickedListener listener notified upon user click on this item.
 */
fun ItemTriggerConditionBinding.bind(
    condition: TriggerCondition,
    conditionClickedListener: (TriggerCondition) -> Unit,
) {
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

private fun Context.getTriggerConditionDescription(condition: TriggerCondition): String =
    when (condition) {
        is TriggerCondition.OnBroadcastReceived -> getString(
            R.string.item_desc_on_broadcast_received,
            condition.toBroadcastActionDisplayName(),
        )
        is TriggerCondition.OnCounterCountReached -> getString(
            R.string.item_desc_on_counter_reached,
            condition.counterName,
            getComparisonOperationDisplayName(condition.comparisonOperation),
            condition.counterValue,
        )
        is TriggerCondition.OnTimerReached -> getString(
            R.string.item_desc_on_timer_reached,
            formatDuration(condition.durationMs),
        )
        else -> throw UnsupportedOperationException("Scenario Start and End Conditions are not supported here")
    }

private fun Context.getComparisonOperationDisplayName(operation: ComparisonOperation): String =
    when (operation) {
        GREATER -> getString(R.string.item_title_greater)
        GREATER_OR_EQUALS -> getString(R.string.item_title_greater_or_equals)
        EQUALS -> getString(R.string.item_title_equals)
        LOWER_OR_EQUALS -> getString(R.string.item_title_lower_or_equals)
        LOWER -> getString(R.string.item_title_lower)
    }

private fun TriggerCondition.OnBroadcastReceived.toBroadcastActionDisplayName(): String {
    val lastDotIndex = intentAction.lastIndexOf('.')

    return if (lastDotIndex != -1 && lastDotIndex != intentAction.lastIndex)
        intentAction.substring(lastDotIndex + 1)
    else intentAction
}
