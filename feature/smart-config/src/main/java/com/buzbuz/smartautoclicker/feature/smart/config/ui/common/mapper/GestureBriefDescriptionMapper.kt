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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper

import android.graphics.Point
import android.graphics.PointF

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureAnimationSegment
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypoint

/**
 * Maps a [Gesture] to the [GestureDescription] displayed by the gesture renderer and editor.
 *
 * All motion positions are resolved to screen coordinates: a condition based position is placed at the center of
 * the detection area of its condition, and a counter based coordinate at the middle of [screenSize] on its axis,
 * as its actual value is only known at execution time.
 *
 * @param conditions the screen conditions of the event owning the gesture, used to resolve condition positions.
 * @param screenSize the size of the screen, used to resolve counter coordinates.
 * @param singleEditableMotionId when non null, only the waypoint of this motion is editable, all others are fixed.
 */
internal fun Gesture.toGestureDescription(
    conditions: List<ScreenCondition>,
    screenSize: Point,
    singleEditableMotionId: Identifier? = null,
): GestureDescription {
    val fingerPaths = fingers.map { finger ->
        finger.toFingerPath(conditions, screenSize, singleEditableMotionId)
    }

    return GestureDescription(
        fingers = fingerPaths,
        totalDurationMs = fingerPaths.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L,
    )
}

private fun GestureFinger.toFingerPath(
    conditions: List<ScreenCondition>,
    screenSize: Point,
    singleEditableMotionId: Identifier?,
): GestureFingerPath {
    val waypoints = mutableListOf<GestureWaypoint>()
    val segments = mutableListOf<GestureAnimationSegment>()
    var durationMs = 0L

    for (motion in motions) {
        when (motion) {
            is GestureFingerMotion.Press -> {
                waypoints += motion.position.toWaypoint(motion.id, conditions, screenSize, singleEditableMotionId)
            }

            is GestureFingerMotion.Move -> {
                val fromIdx = waypoints.size - 1
                waypoints += motion.position.toWaypoint(motion.id, conditions, screenSize, singleEditableMotionId)
                segments += GestureAnimationSegment.Move(fromIdx, waypoints.size - 1, motion.durationMs)
                durationMs += motion.durationMs
            }

            is GestureFingerMotion.Wait -> {
                if (waypoints.isNotEmpty()) {
                    segments += GestureAnimationSegment.Wait(waypoints.size - 1, motion.durationMs)
                    durationMs += motion.durationMs
                }
            }

            is GestureFingerMotion.Release -> Unit
        }
    }

    return GestureFingerPath(waypoints, startTimeMs, durationMs, segments)
}

private fun GesturePosition.toWaypoint(
    motionId: Identifier,
    conditions: List<ScreenCondition>,
    screenSize: Point,
    singleEditableMotionId: Identifier?,
): GestureWaypoint =
    GestureWaypoint(
        position = toDisplayPoint(conditions, screenSize),
        isEditable = singleEditableMotionId == null || motionId == singleEditableMotionId,
        motionId = motionId,
    )

private fun GesturePosition.toDisplayPoint(conditions: List<ScreenCondition>, screenSize: Point): PointF =
    when (this) {
        is GesturePosition.Condition -> {
            val condition =
                if (conditionId != null) conditions.find { it.id == conditionId }
                else conditions.firstOrNull()

            when (val area = condition?.getDisplayArea()) {
                null -> PointF(screenSize.x / 2f, screenSize.y / 2f)
                else -> PointF(area.exactCenterX(), area.exactCenterY())
            }
        }

        is GesturePosition.Coordinates -> PointF(
            when (val xPos = x) {
                is GestureCoordinatesValue.Literal -> xPos.value.toFloat()
                is GestureCoordinatesValue.Counter -> screenSize.x / 2f
            },
            when (val yPos = y) {
                is GestureCoordinatesValue.Literal -> yPos.value.toFloat()
                is GestureCoordinatesValue.Counter -> screenSize.y / 2f
            },
        )
    }

private fun ScreenCondition.getDisplayArea() = when (this) {
    is ScreenCondition.Color -> detectionArea
    is ScreenCondition.Image -> detectionArea ?: area
    is ScreenCondition.Number -> detectionArea
    is ScreenCondition.Text -> detectionArea
}
