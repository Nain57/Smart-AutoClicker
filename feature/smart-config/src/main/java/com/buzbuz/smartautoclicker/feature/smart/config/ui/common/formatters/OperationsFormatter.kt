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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters

import android.content.Context
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.R

@StringRes
internal fun ComparisonOperation.toNameRes(): Int =
    when (this) {
        ComparisonOperation.GREATER -> R.string.comparison_operator_greater
        ComparisonOperation.GREATER_OR_EQUALS -> R.string.comparison_operator_greater_or_equals
        ComparisonOperation.EQUALS -> R.string.comparison_operator_equals
        ComparisonOperation.LOWER_OR_EQUALS -> R.string.comparison_operator_lower_or_equals
        ComparisonOperation.LOWER -> R.string.comparison_operator_lower
    }

@StringRes
internal fun ComparisonOperation.toFullNameRes(): Int =
    when (this) {
        ComparisonOperation.GREATER -> R.string.comparison_operator_greater_full
        ComparisonOperation.GREATER_OR_EQUALS -> R.string.comparison_operator_greater_or_equals_full
        ComparisonOperation.EQUALS -> R.string.comparison_operator_equals_full
        ComparisonOperation.LOWER_OR_EQUALS -> R.string.comparison_operator_lower_or_equals_full
        ComparisonOperation.LOWER -> R.string.comparison_operator_lower_full
    }

@StringRes
internal fun ChangeCounter.OperationType.toNameRes(): Int =
    when (this) {
        ChangeCounter.OperationType.ADD -> R.string.dropdown_counter_operation_item_add
        ChangeCounter.OperationType.MINUS -> R.string.dropdown_counter_operation_item_minus
        ChangeCounter.OperationType.SET -> R.string.dropdown_counter_operation_item_set
    }

fun ComparisonOperation.toEffectDescription(context: Context, counterName: String? = null, operand: String) =
    context.getString(
        R.string.field_change_counter_check_effect_desc,
        counterName ?: "",
        context.getString(toNameRes()),
        operand,
    )

internal fun ChangeCounter.OperationType.toEffectDescription(context: Context, counterName: String, operand: String) =
    when (this) {
        ChangeCounter.OperationType.ADD,
        ChangeCounter.OperationType.MINUS -> {
            context.getString(
                R.string.field_change_counter_effect_desc_operation,
                counterName.ifEmpty { "?" },
                context.getString(toNameRes()),
                operand.ifEmpty { "?" },
            )
        }

        ChangeCounter.OperationType.SET ->
            context.getString(
                R.string.field_change_counter_effect_desc_set,
                counterName.ifEmpty { "?" },
                operand.ifEmpty { "?" },
            )
    }


internal fun CounterOperationValue.toEffectDescription(context: Context, operation: ComparisonOperation): String =
    when (this) {
        is CounterOperationValue.Counter -> context.getString(
            R.string.message_number_condition_counter_value_desc,
            context.getString(operation.toFullNameRes()),
            value.ifEmpty { "?" },
        )
        is CounterOperationValue.Number -> context.getString(
            R.string.message_number_condition_static_value_desc,
            context.getString(operation.toFullNameRes()),
            value.toNaturalDisplayString(),
        )
    }
