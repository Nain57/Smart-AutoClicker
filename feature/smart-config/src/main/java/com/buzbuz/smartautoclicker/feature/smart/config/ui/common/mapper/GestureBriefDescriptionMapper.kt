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
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureAnimationSegment
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypoint
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypointCondition

/**
 * Maps a [Gesture] to the [GestureDescription] displayed by the gesture renderer and editor.
 *
 * All motion positions are resolved to screen coordinates, as their actual value is only known at execution time:
 * a condition based position is placed at the center of the area where its condition can be detected, and a counter
 * based coordinate at the starting value of its counter.
 *
 * @param conditionOperator the conditions operator of the event owning the gesture. With OR, the condition detected
 * first is only known at execution time, so the first condition of the event is always used.
 * @param conditions the screen conditions of the event owning the gesture, used to resolve condition positions.
 * @param counters the counters of the scenario with a custom starting value, used to resolve counter coordinates.
 * @param screenSize the size of the screen, used for conditions detected on the whole screen or not found.
 * @param singleEditableMotionId when non null, only the waypoint of this motion is editable, all others are fixed.
 * @param conditionDisplays the display of each condition, by condition identifier, attached to the waypoints placed on
 * that condition. See [toWaypointConditions]. Waypoints with counter based coordinates get a label with those
 * coordinates instead, such as "counterX / 120".
 */
internal fun Gesture.toGestureDescription(
    @ConditionOperator conditionOperator: Int,
    conditions: List<ScreenCondition>,
    counters: List<Counter>,
    screenSize: Point,
    singleEditableMotionId: Identifier? = null,
    conditionDisplays: Map<Identifier, GestureWaypointCondition> = emptyMap(),
): GestureDescription {
    val positionResolver = PositionResolver(conditionOperator, conditions, counters, screenSize, conditionDisplays)
    val fingerPaths = fingers.map { finger ->
        finger.toFingerPath(positionResolver, singleEditableMotionId)
    }

    return GestureDescription(
        fingers = fingerPaths,
        totalDurationMs = fingerPaths.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L,
    )
}

private fun GestureFinger.toFingerPath(
    positionResolver: PositionResolver,
    singleEditableMotionId: Identifier?,
): GestureFingerPath {
    val waypoints = mutableListOf<GestureWaypoint>()
    val segments = mutableListOf<GestureAnimationSegment>()
    var durationMs = 0L
    var isHeld = false

    for (motion in motions) {
        when (motion) {
            is GestureFingerMotion.Press -> {
                waypoints += positionResolver.toWaypoint(motion.position, motion.id, singleEditableMotionId)
            }

            is GestureFingerMotion.Move -> {
                val fromIdx = waypoints.size - 1
                waypoints += positionResolver.toWaypoint(motion.position, motion.id, singleEditableMotionId)
                segments += GestureAnimationSegment.Move(fromIdx, waypoints.size - 1, motion.durationMs)
                durationMs += motion.durationMs
            }

            is GestureFingerMotion.Wait -> {
                if (waypoints.isNotEmpty()) {
                    segments += GestureAnimationSegment.Wait(waypoints.size - 1, motion.durationMs)
                    durationMs += motion.durationMs
                }
            }

            is GestureFingerMotion.Release -> {
                isHeld = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && motion.holdUntilUnfulfilled
            }
        }
    }

    return GestureFingerPath(waypoints, startTimeMs, durationMs, segments, isHeld)
}

private class PositionResolver(
    @param:ConditionOperator private val conditionOperator: Int,
    private val conditions: List<ScreenCondition>,
    counters: List<Counter>,
    private val screenSize: Point,
    private val conditionDisplays: Map<Identifier, GestureWaypointCondition>,
) {

    /** Counters without a custom starting value aren't stored and starts at 0. */
    private val counterStartValues: Map<String, Double> = counters.associate { it.counterName to it.defaultValue }

    fun toWaypoint(position: Position, motionId: Identifier, singleEditableMotionId: Identifier?): GestureWaypoint {
        val condition = (position as? Position.Condition)?.let(::findCondition)

        return GestureWaypoint(
            position = condition?.getDisplayPoint() ?: toDisplayPoint(position),
            isEditable = singleEditableMotionId == null || motionId == singleEditableMotionId,
            motionId = motionId,
            condition = condition?.let { conditionDisplays[it.id] }
                ?: (position as? Position.Coordinates)?.toCounterWaypointCondition(),
        )
    }

    /** @return a label such as "counterX / 120" if any coordinate is read from a counter, null otherwise. */
    private fun Position.Coordinates.toCounterWaypointCondition(): GestureWaypointCondition? {
        if (x !is CoordinatesValue.Counter && y !is CoordinatesValue.Counter) return null
        return GestureWaypointCondition(label = "${x.toLabel()} / ${y.toLabel()}")
    }

    private fun findCondition(position: Position.Condition): ScreenCondition? =
        if (conditionOperator == AND && position.conditionId != null) conditions.find { it.id == position.conditionId }
        else conditions.firstOrNull()

    private fun toDisplayPoint(position: Position): PointF =
        when (position) {
            is Position.Condition -> screenCenter()
            is Position.Coordinates -> PointF(position.x.toDisplayValue(), position.y.toDisplayValue())
        }

    private fun ScreenCondition.getDisplayPoint(): PointF =
        when (val area = getDetectionArea()) {
            null -> screenCenter()
            else -> PointF(area.exactCenterX(), area.exactCenterY())
        }

    private fun screenCenter(): PointF = PointF(screenSize.x / 2f, screenSize.y / 2f)

    private fun CoordinatesValue.toDisplayValue(): Float =
        when (this) {
            is CoordinatesValue.Literal -> value.toFloat()
            is CoordinatesValue.Counter -> counterStartValues[counterName]?.toFloat() ?: 0f
        }

    private fun CoordinatesValue.toLabel(): String =
        when (this) {
            is CoordinatesValue.Literal -> value.toString()
            is CoordinatesValue.Counter -> counterName.ifEmpty { "?" }
        }

    /** @return the area where the condition can be detected, or null if it is the whole screen. */
    private fun ScreenCondition.getDetectionArea(): Rect? = when (this) {
        is ScreenCondition.Color -> detectionArea
        is ScreenCondition.Image -> when (detectionType) {
            EXACT -> area
            IN_AREA -> detectionArea ?: area
            else -> null
        }
        is ScreenCondition.Number -> detectionArea
        is ScreenCondition.Text -> detectionArea
    }
}
