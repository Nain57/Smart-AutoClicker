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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.PointF
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.GESTURE_DURATION_MAX_VALUE

import kotlin.math.max
import kotlin.math.min

import kotlin.random.Random

fun Random.nextFloat(from: Float, until: Float): Float =
    (until - from) * nextFloat()

fun Random.getNextPositionIn(area: RectF): PointF =
    PointF(nextFloat(area.left, area.right), nextFloat(area.top, area.bottom))

fun Random.getRandomizedPosition(position: Int): Float = nextInt(
    from = max(position - RANDOMIZATION_POSITION_MAX_OFFSET_PX, 0),
    until = position + RANDOMIZATION_POSITION_MAX_OFFSET_PX + 1,
).toFloat()

fun Random.getRandomizedDuration(duration: Long): Long = nextLong(
    from = max(duration - RANDOMIZATION_DURATION_MAX_OFFSET_MS, 1),
    until = duration + RANDOMIZATION_DURATION_MAX_OFFSET_MS + 1,
)

fun Random.getRandomizedGestureDuration(duration: Long): Long = nextLong(
    from = max(duration - RANDOMIZATION_DURATION_MAX_OFFSET_MS, 1),
    until = min(duration + RANDOMIZATION_DURATION_MAX_OFFSET_MS + 1, GESTURE_DURATION_MAX_VALUE),
)

/** */
private const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
/** */
private const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5