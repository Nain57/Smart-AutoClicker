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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

interface Repeatable {
    val repeatCount: Int
    val isRepeatInfinite: Boolean

    fun isRepeatCountValid(): Boolean =
        repeatCount > 0
}

interface RepeatableWithDelay : Repeatable {
    val repeatDelayMs: Long

    fun isRepeatDelayValid(): Boolean =
        repeatDelayMs >= 0
}

const val REPEAT_COUNT_MIN_VALUE: Int = 1
const val REPEAT_COUNT_MAX_VALUE: Int = 99999

const val REPEAT_DELAY_MIN_MS: Long = 0
const val REPEAT_DELAY_MAX_MS: Long = 3_600_000