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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.accessibilityservice.GestureDescription
import android.graphics.Path

import kotlin.math.max
import kotlin.math.min

fun GestureDescription.Builder.buildSingleStroke(path: Path, durationMs: Long, startTime: Long = 0): GestureDescription {
    try {
        addStroke(
            GestureDescription.StrokeDescription(
                path,
                startTime.toNormalizedStrokeStartTime(),
                durationMs.toNormalizedStrokeDurationMs(),
            )
        )
    } catch (ex: IllegalStateException) {
        throw IllegalStateException("Invalid gesture; Duration=$durationMs", ex)
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid gesture; Duration=$durationMs", ex)
    }

    return build()
}

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))


private const val MINIMUM_STROKE_DURATION_MS = 1L
private const val MAXIMUM_STROKE_DURATION_MS = 59_999L