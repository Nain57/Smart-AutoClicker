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

import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.toNaturalDisplayString(maxFractionDigits: Int? = null): String {
    if (!isFinite()) return toString()

    val value = maxFractionDigits
        ?.let { BigDecimal.valueOf(this).setScale(it, RoundingMode.HALF_UP) }
        ?: BigDecimal.valueOf(this)

    return value.stripTrailingZeros().toPlainString()
}

fun CounterOperationValue.toNaturalDisplayString(): String =
    when (this) {
        is CounterOperationValue.Counter -> value
        is CounterOperationValue.Number -> value.toNaturalDisplayString()
    }
