/*
 * Copyright (C) 2025 Kevin Buzeau
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

package com.buzbuz.smartautoclicker.core.processing.data.scaling

import android.graphics.Point
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun Point.toArea(): Rect =
    Rect(0, 0, x, y)

internal fun Point.scale(scalingRatio: Double): Point =
    if (scalingRatio == 1.0) this
    else Point(
        (x * scalingRatio).roundToInt(),
        (y * scalingRatio).roundToInt()
    )

internal fun Rect.scale(scalingRatio: Double): Rect =
    if (scalingRatio == 1.0) this
    else {
        val x = (left * scalingRatio).roundToInt()
        val y = (top * scalingRatio).roundToInt()
        Rect(
            x,
            y,
            x + (width() * scalingRatio).roundToInt(),
            y + (height() * scalingRatio).roundToInt()
        )
    }

internal fun Rect.grow(bounds: Rect, growValue: Int = 1): Rect =
    Rect(
        (left - growValue).coerceIn(bounds.left, bounds.right),
        (top - growValue).coerceIn(bounds.top, bounds.bottom),
        (right + growValue).coerceIn(bounds.left, bounds.right),
        (bottom + growValue).coerceIn(bounds.top, bounds.bottom),
    )
