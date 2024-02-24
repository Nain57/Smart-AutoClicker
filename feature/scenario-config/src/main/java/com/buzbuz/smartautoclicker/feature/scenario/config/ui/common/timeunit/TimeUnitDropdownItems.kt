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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.common.timeunit

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.scenario.config.R

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


val msItem = DropdownItem(R.string.dropdown_label_time_unit_ms)
val sItem = DropdownItem(R.string.dropdown_label_time_unit_s)
val minItem = DropdownItem(R.string.dropdown_label_time_unit_min)
val hItem = DropdownItem(R.string.dropdown_label_time_unit_h)

val timeUnitDropdownItems = listOf(msItem, sItem, minItem, hItem)

fun Long?.toDurationMs(unit: DropdownItem): Long =
    when {
        this == null -> -1
        unit == sItem -> this * 1.seconds.inWholeMilliseconds
        unit == minItem -> this * 1.minutes.inWholeMilliseconds
        unit == hItem -> this * 1.hours.inWholeMilliseconds
        else -> this
    }

fun Long?.findAppropriateTimeUnit(): DropdownItem =
    when {
        this == null || this <= 0L -> msItem
        this % 1.hours.inWholeMilliseconds == 0L -> hItem
        this % 1.minutes.inWholeMilliseconds == 0L -> minItem
        this % 1.seconds.inWholeMilliseconds == 0L -> sItem
        else -> msItem
    }

fun DropdownItem.formatDuration(durationMs: Long): String =
    when (this) {
        sItem -> durationMs / 1.seconds.inWholeMilliseconds
        minItem -> durationMs / 1.minutes.inWholeMilliseconds
        hItem -> durationMs / 1.hours.inWholeMilliseconds
        else -> durationMs
    }.toString()