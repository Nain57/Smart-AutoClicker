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
package com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.extensions.nextIntInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.nextLongInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_DURATION_MAX_OFFSET_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_POSITION_MAX_OFFSET_PX
import kotlin.math.max
import kotlin.random.Random

/**
 * Randomization must be applied once, when a position is resolved, and never again when the paths are built: a
 * continued stroke is rejected by the system unless its first point is exactly the last point of the stroke it
 * continues. Clamping is applied here for the same reason, [safeMoveTo] would otherwise silently move the point.
 */
internal fun Random?.randomizedPosition(position: Point): Point =
    if (this == null) Point(max(0, position.x), max(0, position.y))
    else Point(
        max(0, nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX)),
        max(0, nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX)),
    )

/** Same as [randomizedPosition], durations are randomized once when the gesture timeline is resolved. */
internal fun Random?.randomizedDurationMs(durationMs: Long): Long =
    (this?.nextLongInOffset(durationMs, RANDOMIZATION_DURATION_MAX_OFFSET_MS) ?: durationMs)
        .coerceAtLeast(MINIMUM_MOTION_DURATION_MS)

/** A motion can't be shorter than that, a stroke duration must be strictly positive. */
private const val MINIMUM_MOTION_DURATION_MS = 1L