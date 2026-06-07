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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNameRes


sealed class UiCounterOperatorDropdownItem(title: Int) : DropdownItem(title) {

    sealed class Comparison {
        data object GreaterItem : UiCounterOperatorDropdownItem(ComparisonOperation.GREATER.toNameRes())
        data object GreaterOrEqualsItem : UiCounterOperatorDropdownItem(ComparisonOperation.GREATER_OR_EQUALS.toNameRes())
        data object EqualsItem : UiCounterOperatorDropdownItem(ComparisonOperation.EQUALS.toNameRes())
        data object LowerOrEqualsItem : UiCounterOperatorDropdownItem(ComparisonOperation.LOWER_OR_EQUALS.toNameRes())
        data object LowerItem : UiCounterOperatorDropdownItem(ComparisonOperation.LOWER.toNameRes())
    }

    sealed class Affectation {
        data object Add : UiCounterOperatorDropdownItem(ChangeCounter.OperationType.ADD.toNameRes())
        data object Set : UiCounterOperatorDropdownItem(ChangeCounter.OperationType.SET.toNameRes())
        data object Minus : UiCounterOperatorDropdownItem(ChangeCounter.OperationType.MINUS.toNameRes())
    }
}

internal fun allCounterComparisonOperatorDropdownItems(): List<UiCounterOperatorDropdownItem> = listOf(
    UiCounterOperatorDropdownItem.Comparison.GreaterItem,
    UiCounterOperatorDropdownItem.Comparison.GreaterOrEqualsItem,
    UiCounterOperatorDropdownItem.Comparison.EqualsItem,
    UiCounterOperatorDropdownItem.Comparison.LowerOrEqualsItem,
    UiCounterOperatorDropdownItem.Comparison.LowerItem,
)

internal fun ComparisonOperation.toCounterOperatorDropdownItem(): UiCounterOperatorDropdownItem =
    when (this) {
        ComparisonOperation.GREATER -> UiCounterOperatorDropdownItem.Comparison.GreaterItem
        ComparisonOperation.GREATER_OR_EQUALS -> UiCounterOperatorDropdownItem.Comparison.GreaterOrEqualsItem
        ComparisonOperation.EQUALS -> UiCounterOperatorDropdownItem.Comparison.EqualsItem
        ComparisonOperation.LOWER_OR_EQUALS -> UiCounterOperatorDropdownItem.Comparison.LowerOrEqualsItem
        ComparisonOperation.LOWER -> UiCounterOperatorDropdownItem.Comparison.LowerItem
    }

internal fun UiCounterOperatorDropdownItem.toComparisonOperation(): ComparisonOperation =
    when (this) {
        UiCounterOperatorDropdownItem.Comparison.GreaterItem -> ComparisonOperation.GREATER
        UiCounterOperatorDropdownItem.Comparison.GreaterOrEqualsItem -> ComparisonOperation.GREATER_OR_EQUALS
        UiCounterOperatorDropdownItem.Comparison.EqualsItem -> ComparisonOperation.EQUALS
        UiCounterOperatorDropdownItem.Comparison.LowerOrEqualsItem -> ComparisonOperation.LOWER_OR_EQUALS
        UiCounterOperatorDropdownItem.Comparison.LowerItem -> ComparisonOperation.LOWER

        UiCounterOperatorDropdownItem.Affectation.Add,
        UiCounterOperatorDropdownItem.Affectation.Minus,
        UiCounterOperatorDropdownItem.Affectation.Set -> throw UnsupportedOperationException()
    }


internal fun allCounterAffectationOperatorDropdownItems(): List<UiCounterOperatorDropdownItem> = listOf(
    UiCounterOperatorDropdownItem.Affectation.Add,
    UiCounterOperatorDropdownItem.Affectation.Minus,
    UiCounterOperatorDropdownItem.Affectation.Set,
)

internal fun UiCounterOperatorDropdownItem.toAffectationOperation(): ChangeCounter.OperationType =
    when (this) {
        UiCounterOperatorDropdownItem.Affectation.Add -> ChangeCounter.OperationType.ADD
        UiCounterOperatorDropdownItem.Affectation.Minus -> ChangeCounter.OperationType.MINUS
        UiCounterOperatorDropdownItem.Affectation.Set -> ChangeCounter.OperationType.SET

        UiCounterOperatorDropdownItem.Comparison.EqualsItem,
        UiCounterOperatorDropdownItem.Comparison.GreaterItem,
        UiCounterOperatorDropdownItem.Comparison.GreaterOrEqualsItem,
        UiCounterOperatorDropdownItem.Comparison.LowerItem,
        UiCounterOperatorDropdownItem.Comparison.LowerOrEqualsItem -> throw UnsupportedOperationException("")
    }

internal fun ChangeCounter.OperationType.toCounterOperatorDropdownItem(): UiCounterOperatorDropdownItem =
    when (this) {
        ChangeCounter.OperationType.ADD -> UiCounterOperatorDropdownItem.Affectation.Add
        ChangeCounter.OperationType.MINUS -> UiCounterOperatorDropdownItem.Affectation.Minus
        ChangeCounter.OperationType.SET -> UiCounterOperatorDropdownItem.Affectation.Set
    }