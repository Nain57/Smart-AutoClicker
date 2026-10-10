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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture

import androidx.core.graphics.toPointF

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureAnimationSegment
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypoint

/**
 * Maps a [DumbAction.DumbGesture] to the [GestureDescription] displayed by the gesture renderer and editor.
 *
 * @param singleEditableMotionId when non null, only the waypoint of this motion is editable, all others are fixed.
 */
internal fun DumbAction.DumbGesture.toGestureDescription(
    singleEditableMotionId: Identifier? = null,
): GestureDescription {
    val fingerPaths = fingers.map { finger -> finger.toFingerPath(singleEditableMotionId) }

    return GestureDescription(
        fingers = fingerPaths,
        totalDurationMs = fingerPaths.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L,
    )
}

private fun DumbGestureFinger.toFingerPath(singleEditableMotionId: Identifier?): GestureFingerPath {
    val waypoints = mutableListOf<GestureWaypoint>()
    val segments = mutableListOf<GestureAnimationSegment>()
    var durationMs = 0L

    for (motion in motions) {
        when (motion) {
            is DumbGestureFingerMotion.Press -> {
                waypoints += motion.toWaypoint(singleEditableMotionId)
            }

            is DumbGestureFingerMotion.Move -> {
                val fromIdx = waypoints.size - 1
                waypoints += motion.toWaypoint(singleEditableMotionId)
                segments += GestureAnimationSegment.Move(fromIdx, waypoints.size - 1, motion.durationMs)
                durationMs += motion.durationMs
            }

            is DumbGestureFingerMotion.Wait -> {
                if (waypoints.isNotEmpty()) {
                    segments += GestureAnimationSegment.Wait(waypoints.size - 1, motion.durationMs)
                    durationMs += motion.durationMs
                }
            }

            is DumbGestureFingerMotion.Release -> Unit
        }
    }

    return GestureFingerPath(waypoints, startTimeMs, durationMs, segments)
}

private fun <T> T.toWaypoint(singleEditableMotionId: Identifier?): GestureWaypoint
        where T : DumbGestureFingerMotion, T : DumbGestureFingerMotion.Positionable =
    GestureWaypoint(
        position = position.toPointF(),
        isEditable = singleEditableMotionId == null || id == singleEditableMotionId,
        motionId = id,
    )
