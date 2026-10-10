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

import android.graphics.Point

import kotlin.math.roundToInt


/**
 * A finger with all its positions and durations resolved into the gesture timeline.
 *
 * Randomization is already applied, those values are final. Instances can only be obtained from a
 * [ResolvedFingerBuilder], which guarantees that consecutive segments share their junction point: a continued stroke
 * is refused by the system unless it starts exactly where the previous one ended.
 *
 * @param index the position of the finger in its gesture, for logging and slicing purposes.
 * @param segments the ordered motions of the finger, from its press to its release.
 */
data class ResolvedFinger internal constructor(
    val index: Int,
    val segments: List<ResolvedSegment>,
) {
    /** Time at which the finger touches the screen, relative to the gesture start. */
    val downMs: Long get() = segments.first().startMs
    /** Time at which the finger leaves the screen, relative to the gesture start. */
    val upMs: Long get() = segments.last().endMs

    /** The segment being played at [timeMs], or null if the finger isn't touching the screen at that time. */
    fun segmentAt(timeMs: Long): ResolvedSegment? =
        segments.firstOrNull { segment -> timeMs >= segment.startMs && timeMs < segment.endMs }
}

/**
 * A single move or wait of a finger, placed on the gesture timeline. A wait has the same [from] and [to].
 *
 * @param startMs the time at which the motion starts, relative to the gesture start.
 * @param endMs the time at which the motion ends, relative to the gesture start.
 * @param from the position of the finger when the motion starts.
 * @param to the position of the finger when the motion ends.
 */
data class ResolvedSegment internal constructor(
    val startMs: Long,
    val endMs: Long,
    val from: Point,
    val to: Point,
) {

    /** Position of the finger at [timeMs], the motion being played at a constant speed. */
    fun positionAt(timeMs: Long): Point {
        if (from == to || timeMs >= endMs) return to
        if (timeMs <= startMs) return from

        val progress = (timeMs - startMs).toDouble() / (endMs - startMs)
        return Point(
            from.x + ((to.x - from.x) * progress).roundToInt(),
            from.y + ((to.y - from.y) * progress).roundToInt(),
        )
    }
}
