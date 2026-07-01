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

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString

sealed class UiStaticOrCounterSelection {
    data class StaticValue(val value: Double): UiStaticOrCounterSelection()
    data class CounterValue(val counter: Counter?): UiStaticOrCounterSelection()
}

enum class UiOperandType {
    STATIC,
    COUNTER,
}

fun UiStaticOrCounterSelection.toDisplayValue(): String =
    when (this) {
        is UiStaticOrCounterSelection.CounterValue -> counter?.counterName ?: "?"
        is UiStaticOrCounterSelection.StaticValue -> value.toNaturalDisplayString()
    }

