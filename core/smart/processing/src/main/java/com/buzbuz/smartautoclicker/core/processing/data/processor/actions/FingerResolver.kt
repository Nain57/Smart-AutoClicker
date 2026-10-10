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
package com.buzbuz.smartautoclicker.core.processing.data.processor.actions

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.FIRST_FINGER_INDEX
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState


/**
 * Resolves the finger positions of a [Gesture] or [Click] into screen pixels, from the detection results and the scenario
 * counters.
 *
 * @param processingState the state of the current processing, holding the counters.
 * @param gestureBuilder the builder the resolved fingers are meant for.
 */
internal class FingerResolver(
    private val processingState: ProcessingState,
    private val gestureBuilder: AndroidGestureBuilder,
) {

    /**
     * Resolve the finger position for the click.
     *
     * @param click the click to resolve.
     * @param event the event the gesture belongs to.
     * @param results the results of the conditions detection for this event, if any.
     */
    fun resolve(click: Click, event: Event, results: ConditionsResults?): ResolvedFinger? {
        val base = click.position?.let { resolveGesturePosition(it, event, results) }
        if (base == null) {
            Log.d(TAG, "Cannot resolve position for $click")
            return null
        }
        val offset = resolveGestureOffset(click.offset, event, results)

        return gestureBuilder
            .newFinger(index = FIRST_FINGER_INDEX, startTimeMs = 0L)
            .press(
                position = Point(base.x + offset.x, base.y + offset.y),
                holdMs = click.pressDuration!!,
            )
            .release()
            .build()
    }

    /**
     * Resolve all the fingers of a gesture. A finger that can't be resolved is skipped.
     *
     * @param gesture the gesture to resolve.
     * @param event the event the gesture belongs to.
     * @param results the results of the conditions detection for this event, if any.
     */
    fun resolve(gesture: Gesture, event: Event, results: ConditionsResults?): List<ResolvedFinger> =
        gesture.fingers.mapIndexedNotNull { index, finger -> resolveFinger(index, finger, event, results) }

    private fun resolveFinger(index: Int, finger: GestureFinger, event: Event, results: ConditionsResults?): ResolvedFinger? {
        if (!finger.isComplete()) {
            Log.i(TAG, "Can't resolve finger $index, finger is incomplete")
            return null
        }

        val fingerBuilder = gestureBuilder.newFinger(index, finger.startTimeMs)
        finger.motions.forEach { motion ->
            when (motion) {
                is GestureFingerMotion.Press ->
                    fingerBuilder.press(resolveMotionPosition(motion, event, results) ?: return null)
                is GestureFingerMotion.Move ->
                    fingerBuilder.move(resolveMotionPosition(motion, event, results) ?: return null, motion.durationMs)
                is GestureFingerMotion.Wait ->
                    fingerBuilder.wait(motion.durationMs)
                is GestureFingerMotion.Release ->
                    fingerBuilder.release(hold = motion.holdUntilUnfulfilled)
            }
        }

        return fingerBuilder.build()
    }

    private fun resolveMotionPosition(
        motion: GestureFingerMotion.Positionable,
        event: Event,
        results: ConditionsResults?,
    ): Point? {
        val base = resolveGesturePosition(motion.position, event, results)
        if (base == null) {
            Log.d(TAG, "Cannot resolve position for $motion")
            return null
        }
        val off = resolveGestureOffset(motion.offset, event, results)

        return Point(base.x + off.x, base.y + off.y)
    }

    /**
     * Unlike a position, an offset never prevents the execution: each coordinate that can't be resolved falls back to
     * 0, independently of the other one.
     */
    private fun resolveGestureOffset(offset: GesturePosition?, event: Event, results: ConditionsResults?): Point =
        when (offset) {
            null -> Point(0, 0)
            is GesturePosition.Coordinates -> Point(
                resolveGesturePositionCoordinateValue(offset.x) ?: 0,
                resolveGesturePositionCoordinateValue(offset.y) ?: 0,
            )
            is GesturePosition.Condition -> resolveGesturePositionConditionValue(offset, event, results) ?: Point(0, 0)
        }

    private fun resolveGesturePosition(position: GesturePosition, event: Event, results: ConditionsResults?): Point? =
        when (position) {
            is GesturePosition.Coordinates -> resolveGesturePositionCoordinates(position)
            is GesturePosition.Condition -> resolveGesturePositionConditionValue(position, event, results)
        }

    private fun resolveGesturePositionCoordinates(coordinates: GesturePosition.Coordinates): Point? {
        val x = resolveGesturePositionCoordinateValue(coordinates.x)
        val y = resolveGesturePositionCoordinateValue(coordinates.y)

        if (x == null || y == null) {
            Log.d(TAG, "Cannot resolve position for $coordinates, coordinates are null")
            return null
        }

        return Point(x, y)
    }

    private fun resolveGesturePositionCoordinateValue(coordinatesValue: GestureCoordinatesValue): Int? =
        when (coordinatesValue) {
            is GestureCoordinatesValue.Literal ->
                coordinatesValue.value
            is GestureCoordinatesValue.Counter ->
                processingState.getCounterValue(coordinatesValue.counterName)?.toInt()
        }

    private fun resolveGesturePositionConditionValue(
        condition: GesturePosition.Condition,
        event: Event,
        results: ConditionsResults?,
    ): Point? {

        if (event !is ScreenEvent) {
            Log.d(TAG, "Cannot resolve position for condition $condition, condition must be a screen event")
            return null
        }

        val condId = condition.conditionId
        val result = when {
            event.conditionOperator == AND && condId != null -> {
                val condResult = results?.getScreenConditionResult(condId.databaseId)
                if (condResult == null) {
                    Log.d(TAG, "Cannot resolve position for $condId, condition has not been processed")
                    return null
                } else condResult
            }

            event.conditionOperator == OR ->
                results?.getFirstScreenConditionDetectedResult()

            else -> {
                Log.d(TAG, "Cannot find a condition to resolve, config is invalid")
                return null
            }
        }

        return result?.position?.let { Point(it.x, it.y) }
    }
}

private const val TAG = "GestureResolver"
