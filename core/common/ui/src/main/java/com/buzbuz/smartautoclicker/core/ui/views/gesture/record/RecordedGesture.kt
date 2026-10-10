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
package com.buzbuz.smartautoclicker.core.ui.views.gesture.record

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureAnimationSegment
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypoint


sealed class RecordedGesture {

    data class Click(
        val position: PointF,
        val durationMs: Long,
    ) : RecordedGesture()

    data class Gesture(
        val fingers: List<RecordedGestureFinger>
    ) : RecordedGesture()
}

data class RecordedGestureFinger(
    val pressedAtMs: Long,
    val motions: List<RecordedGestureFingerMotion>
)

sealed class RecordedGestureFingerMotion {
    data class Press(val position: PointF) : RecordedGestureFingerMotion()
    data class Move(val moveToPosition: PointF, val moveToDurationMs: Long) : RecordedGestureFingerMotion()
    data class Wait(val waitDurationMs: Long) : RecordedGestureFingerMotion()
    data object Release : RecordedGestureFingerMotion()
}

fun RecordedGesture.toActionDescription(): ItemBriefDescription =
    when (this) {
        is RecordedGesture.Click -> ClickDescription(pressDurationMs = durationMs, position = position)
        is RecordedGesture.Gesture -> fingers.toGestureDescription()
    }

/** Express any recorded gesture as its fingers, a click being a single finger pressed and released in place. */
fun RecordedGesture.toRecordedFingers(): List<RecordedGestureFinger> =
    when (this) {
        is RecordedGesture.Gesture -> fingers
        is RecordedGesture.Click -> listOf(
            RecordedGestureFinger(
                pressedAtMs = 0L,
                motions = listOf(
                    RecordedGestureFingerMotion.Press(position),
                    RecordedGestureFingerMotion.Wait(durationMs),
                    RecordedGestureFingerMotion.Release,
                ),
            )
        )
    }

fun List<RecordedGestureFinger>.toGestureDescription(isEditable: Boolean = true): GestureDescription {
    val gestureStartMs = minOfOrNull { it.pressedAtMs } ?: 0L
    val paths = map { finger ->
        finger.toFingerPath(startTimeMs = finger.pressedAtMs - gestureStartMs, isEditable = isEditable)
    }

    return GestureDescription(
        fingers = paths,
        totalDurationMs = paths.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L,
    )
}

fun RecordedGestureFinger.toFingerPath(startTimeMs: Long = 0L, isEditable: Boolean = true): GestureFingerPath {
    val waypoints = mutableListOf<GestureWaypoint>()
    val segments = mutableListOf<GestureAnimationSegment>()
    var durationMs = 0L

    motions.forEach { motion ->
        when (motion) {
            is RecordedGestureFingerMotion.Press ->
                waypoints += GestureWaypoint(motion.position, isEditable)

            is RecordedGestureFingerMotion.Move -> {
                val fromIdx = waypoints.size - 1
                waypoints += GestureWaypoint(motion.moveToPosition, isEditable)
                segments += GestureAnimationSegment.Move(fromIdx, waypoints.size - 1, motion.moveToDurationMs)
                durationMs += motion.moveToDurationMs
            }

            is RecordedGestureFingerMotion.Wait -> {
                if (waypoints.isNotEmpty()) {
                    segments += GestureAnimationSegment.Wait(waypoints.size - 1, motion.waitDurationMs)
                    durationMs += motion.waitDurationMs
                }
            }

            RecordedGestureFingerMotion.Release -> Unit
        }
    }

    return GestureFingerPath(waypoints, startTimeMs, durationMs, segments)
}
