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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.extensions.nextIntInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.nextLongInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.safeLineTo
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo
import com.buzbuz.smartautoclicker.core.common.actions.utils.MAXIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.MINIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_DURATION_MAX_OFFSET_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_POSITION_MAX_OFFSET_PX
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


fun Path.moveTo(position: Point, random: Random?) {
    if (random == null) safeMoveTo(position.x, position.y)
    else safeMoveTo(
        random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
    )
}

fun Path.line(from: Point?, to: Point?, random: Random?) {
    if (from == null || to == null) return

    moveTo(from, random)
    lineTo(to, random)
}

private fun Path.lineTo(position: Point, random: Random?) {
    if (random == null) safeLineTo(position.x, position.y)
    else safeLineTo(
        random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
    )
}

fun GestureDescription.Builder.buildSingleStroke(
    path: Path,
    durationMs: Long,
    startTime: Long = 0,
    random: Random?,
): GestureDescription {

    val actualDurationMs = random
        ?.nextLongInOffset(durationMs, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        ?: durationMs

    try {
        addStroke(
            GestureDescription.StrokeDescription(
                path,
                startTime.toNormalizedStrokeStartTime(),
                actualDurationMs.toNormalizedStrokeDurationMs(),
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
