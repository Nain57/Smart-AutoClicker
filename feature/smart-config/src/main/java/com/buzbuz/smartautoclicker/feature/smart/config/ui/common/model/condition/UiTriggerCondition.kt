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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition

import android.content.Context
import androidx.annotation.DrawableRes

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.*
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R


data class UiTriggerCondition internal constructor (
    override val condition: TriggerCondition,
    override val name: String,
    override val haveError: Boolean,
    @DrawableRes val iconRes: Int,
    val description: String,
) : UiCondition()

internal fun TriggerCondition.toUiTriggerCondition(context: Context, inError: Boolean) = UiTriggerCondition(
    condition = this,
    name = name,
    iconRes = getIconRes(),
    description = getTriggerConditionDescription(context),
    haveError = inError,
)

@DrawableRes
internal fun TriggerCondition.getIconRes(): Int =
    when (this) {
        is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
        is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
        is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
        else -> throw UnsupportedOperationException("Unsupported condition type")
    }

private fun TriggerCondition.getTriggerConditionDescription(context: Context): String =
    when (this) {
        is TriggerCondition.OnBroadcastReceived -> context.getString(
            R.string.item_broadcast_received_details,
            toBroadcastActionDisplayName(),
        )

        is TriggerCondition.OnCounterCountReached -> context.getString(
            R.string.item_counter_reached_details,
            counterName,
            getComparisonOperationDisplayName(context),
            counterValue,
        )

        is TriggerCondition.OnTimerReached -> context.getString(
            R.string.item_timer_reached_details,
            formatDuration(durationMs),
        )

        else -> throw UnsupportedOperationException("Scenario Start and End Conditions are not supported here")
    }

private fun TriggerCondition.OnCounterCountReached.getComparisonOperationDisplayName(context: Context): String =
    when (comparisonOperation) {
        GREATER -> context.getString(R.string.dropdown_comparison_operator_item_greater)
        GREATER_OR_EQUALS -> context.getString(R.string.dropdown_comparison_operator_item_greater_or_equals)
        EQUALS -> context.getString(R.string.dropdown_comparison_operator_item_equals)
        LOWER_OR_EQUALS -> context.getString(R.string.dropdown_comparison_operator_item_lower_or_equals)
        LOWER -> context.getString(R.string.dropdown_comparison_operator_item_lower)
    }

private fun TriggerCondition.OnBroadcastReceived.toBroadcastActionDisplayName(): String {
    val lastDotIndex = intentAction.lastIndexOf('.')

    return if (lastDotIndex != -1 && lastDotIndex != intentAction.lastIndex)
        intentAction.substring(lastDotIndex + 1)
    else intentAction
}