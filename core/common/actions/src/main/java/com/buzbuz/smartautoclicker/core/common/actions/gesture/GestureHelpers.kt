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
import android.os.Build
import androidx.annotation.RequiresApi
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

/**
 * Randomization must be applied once, when a position is resolved, and never again when the paths are built: a
 * continued stroke is rejected by the system unless its first point is exactly the last point of the stroke it
 * continues. Clamping is applied here for the same reason, [safeMoveTo] would otherwise silently move the point.
 */
fun Point.randomizedPosition(random: Random?): Point =
    if (random == null) Point(max(0, x), max(0, y))
    else Point(
        max(0, random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX)),
        max(0, random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX)),
    )

/** Same as [randomizedPosition], durations are randomized once when the gesture timeline is resolved. */
fun Long.randomizedDurationMs(random: Random?): Long =
    random?.nextLongInOffset(this, RANDOMIZATION_DURATION_MAX_OFFSET_MS) ?: this

fun Path.moveTo(position: Point): Unit =
    safeMoveTo(position.x, position.y)

fun Path.lineTo(position: Point): Unit =
    safeLineTo(position.x, position.y)

fun Path.line(from: Point, to: Point) {
    moveTo(from)
    lineTo(to)
}

/**
 * A zero length path. The framework handles it as a touch that doesn't move: the pointer stays down at [position]
 * for the whole stroke duration, without emitting a single move event.
 */
fun Path.hold(position: Point): Unit =
    moveTo(position)

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

fun buildStroke(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): GestureDescription.StrokeDescription {
    val normalizedStart = startTimeMs.toNormalizedStrokeStartTime()
    val normalizedDuration = durationMs.toNormalizedStrokeDurationMs()

    return if (willContinue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        GestureDescription.StrokeDescription(path, normalizedStart, normalizedDuration, true)
    } else {
        GestureDescription.StrokeDescription(path, normalizedStart, normalizedDuration)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun GestureDescription.StrokeDescription.continueWith(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): GestureDescription.StrokeDescription =
    continueStroke(
        path,
        startTimeMs.toNormalizedStrokeStartTime(),
        durationMs.toNormalizedStrokeDurationMs(),
        willContinue,
    )

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))
