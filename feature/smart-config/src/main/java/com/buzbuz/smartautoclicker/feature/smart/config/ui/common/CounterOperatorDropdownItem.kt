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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common

import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toName

sealed class CounterOperatorDropdownItem(title: Int) : DropdownItem(title) {
    data object GreaterItem : CounterOperatorDropdownItem(ComparisonOperation.GREATER.toName())
    data object GreaterOrEqualsItem : CounterOperatorDropdownItem(R.string.comparison_operator_greater_or_equals)
    data object EqualsItem : CounterOperatorDropdownItem(R.string.comparison_operator_equals)
    data object LowerOrEqualsItem : CounterOperatorDropdownItem(R.string.comparison_operator_lower_or_equals)
    data object LowerItem : CounterOperatorDropdownItem(R.string.comparison_operator_lower)
}

internal fun allCounterOperatorDropdownItems(): List<CounterOperatorDropdownItem> = listOf(
    CounterOperatorDropdownItem.GreaterItem,
    CounterOperatorDropdownItem.GreaterOrEqualsItem,
    CounterOperatorDropdownItem.EqualsItem,
    CounterOperatorDropdownItem.LowerOrEqualsItem,
    CounterOperatorDropdownItem.LowerItem,
)

internal fun ComparisonOperation.toCounterOperatorDropdownItem(): CounterOperatorDropdownItem =
    when (this) {
        ComparisonOperation.GREATER -> CounterOperatorDropdownItem.GreaterItem
        ComparisonOperation.GREATER_OR_EQUALS -> CounterOperatorDropdownItem.GreaterOrEqualsItem
        ComparisonOperation.EQUALS -> CounterOperatorDropdownItem.EqualsItem
        ComparisonOperation.LOWER_OR_EQUALS -> CounterOperatorDropdownItem.LowerOrEqualsItem
        ComparisonOperation.LOWER -> CounterOperatorDropdownItem.LowerItem
    }

internal fun CounterOperatorDropdownItem.toComparisonOperation(): ComparisonOperation =
    when (this) {
        CounterOperatorDropdownItem.GreaterItem -> ComparisonOperation.GREATER
        CounterOperatorDropdownItem.GreaterOrEqualsItem -> ComparisonOperation.GREATER_OR_EQUALS
        CounterOperatorDropdownItem.EqualsItem -> ComparisonOperation.EQUALS
        CounterOperatorDropdownItem.LowerOrEqualsItem -> ComparisonOperation.LOWER_OR_EQUALS
        CounterOperatorDropdownItem.LowerItem -> ComparisonOperation.LOWER
    }