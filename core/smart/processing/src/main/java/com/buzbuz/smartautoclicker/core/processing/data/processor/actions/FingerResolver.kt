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
 * Actions can be executed on every processed frame, so the positions are resolved into points kept between two
 * resolutions instead of being allocated each time. This makes an instance not thread safe.
 *
 * @param processingState the state of the current processing, holding the counters.
 * @param gestureBuilder the builder the resolved fingers are meant for.
 */
internal class FingerResolver(
    private val processingState: ProcessingState,
    private val gestureBuilder: AndroidGestureBuilder,
) {

    /** The last resolved position, offset included. A finger builder never keeps the position it is given. */
    private val resolvedPosition: Point = Point()
    /** The last resolved offset, to be applied to [resolvedPosition]. */
    private val resolvedOffset: Point = Point()

    /**
     * Resolve the finger position for the click.
     *
     * @param click the click to resolve.
     * @param event the event the gesture belongs to.
     * @param results the results of the conditions detection for this event, if any.
     */
    fun resolve(click: Click, event: Event, results: ConditionsResults?): ResolvedFinger? {
        if (!resolvePosition(click.position, click.offset, event, results)) {
            Log.d(TAG, "Cannot resolve position for $click")
            return null
        }

        return gestureBuilder
            .newFinger(index = FIRST_FINGER_INDEX, startTimeMs = 0L)
            .press(
                position = resolvedPosition,
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
                is GestureFingerMotion.Press -> {
                    if (!resolveMotionPosition(motion, event, results)) return null
                    fingerBuilder.press(resolvedPosition)
                }
                is GestureFingerMotion.Move -> {
                    if (!resolveMotionPosition(motion, event, results)) return null
                    fingerBuilder.move(resolvedPosition, motion.durationMs)
                }
                is GestureFingerMotion.Wait ->
                    fingerBuilder.wait(motion.durationMs)
                is GestureFingerMotion.Release ->
                    fingerBuilder.release(hold = motion.holdUntilUnfulfilled)
            }
        }

        return fingerBuilder.build()
    }

    /** Resolve the position of [motion] into [resolvedPosition]. @return false if it can't be resolved. */
    private fun resolveMotionPosition(
        motion: GestureFingerMotion.Positionable,
        event: Event,
        results: ConditionsResults?,
    ): Boolean {
        if (!resolvePosition(motion.position, motion.offset, event, results)) {
            Log.d(TAG, "Cannot resolve position for $motion")
            return false
        }
        return true
    }

    /** Resolve [position] with its [offset] applied into [resolvedPosition]. @return false if it can't be resolved. */
    private fun resolvePosition(
        position: GesturePosition?,
        offset: GesturePosition?,
        event: Event,
        results: ConditionsResults?,
    ): Boolean {
        if (position == null || !resolveGesturePosition(position, event, results, resolvedPosition)) return false

        resolveGestureOffset(offset, event, results, resolvedOffset)
        resolvedPosition.offset(resolvedOffset.x, resolvedOffset.y)
        return true
    }

    /**
     * Unlike a position, an offset never prevents the execution: each coordinate that can't be resolved falls back to
     * 0, independently of the other one.
     */
    private fun resolveGestureOffset(offset: GesturePosition?, event: Event, results: ConditionsResults?, into: Point) {
        when (offset) {
            null -> into.set(0, 0)
            is GesturePosition.Coordinates -> into.set(
                resolveGesturePositionCoordinateValue(offset.x).orZeroIfUnresolved(),
                resolveGesturePositionCoordinateValue(offset.y).orZeroIfUnresolved(),
            )
            is GesturePosition.Condition ->
                if (!resolveGesturePositionConditionValue(offset, event, results, into)) into.set(0, 0)
        }
    }

    /** Resolve [position] into [into]. @return false if it can't be resolved. */
    private fun resolveGesturePosition(
        position: GesturePosition,
        event: Event,
        results: ConditionsResults?,
        into: Point,
    ): Boolean =
        when (position) {
            is GesturePosition.Coordinates -> resolveGesturePositionCoordinates(position, into)
            is GesturePosition.Condition -> resolveGesturePositionConditionValue(position, event, results, into)
        }

    private fun resolveGesturePositionCoordinates(coordinates: GesturePosition.Coordinates, into: Point): Boolean {
        val x = resolveGesturePositionCoordinateValue(coordinates.x)
        val y = resolveGesturePositionCoordinateValue(coordinates.y)

        if (x == UNRESOLVED_COORDINATE || y == UNRESOLVED_COORDINATE) {
            Log.d(TAG, "Cannot resolve position for $coordinates, coordinates are null")
            return false
        }

        into.set(x, y)
        return true
    }

    /** @return the value of the coordinate, or [UNRESOLVED_COORDINATE]. Not nullable, to avoid boxing the result. */
    private fun resolveGesturePositionCoordinateValue(coordinatesValue: GestureCoordinatesValue): Int =
        when (coordinatesValue) {
            is GestureCoordinatesValue.Literal ->
                coordinatesValue.value
            is GestureCoordinatesValue.Counter ->
                processingState.getCounterValue(coordinatesValue.counterName)?.toInt() ?: UNRESOLVED_COORDINATE
        }

    private fun resolveGesturePositionConditionValue(
        condition: GesturePosition.Condition,
        event: Event,
        results: ConditionsResults?,
        into: Point,
    ): Boolean {

        if (event !is ScreenEvent) {
            Log.d(TAG, "Cannot resolve position for condition $condition, condition must be a screen event")
            return false
        }

        val condId = condition.conditionId
        val result = when {
            event.conditionOperator == AND && condId != null -> {
                val condResult = results?.getScreenConditionResult(condId.databaseId)
                if (condResult == null) {
                    Log.d(TAG, "Cannot resolve position for $condId, condition has not been processed")
                    return false
                } else condResult
            }

            event.conditionOperator == OR ->
                results?.getFirstScreenConditionDetectedResult()

            else -> {
                Log.d(TAG, "Cannot find a condition to resolve, config is invalid")
                return false
            }
        }

        val position = result?.position ?: return false
        into.set(position.x, position.y)
        return true
    }
}

private fun Int.orZeroIfUnresolved(): Int =
    if (this == UNRESOLVED_COORDINATE) 0 else this

/**
 * Marks a coordinate that can't be resolved. A counter value this far off the screen is meaningless as a coordinate, so
 * it can't be mistaken for a real one.
 */
private const val UNRESOLVED_COORDINATE = Int.MIN_VALUE

private const val TAG = "GestureResolver"
