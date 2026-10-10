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
package com.buzbuz.smartautoclicker.core.processing.data.processor.gesture

import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.common.actions.gesture.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ResolvedFinger
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
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
 * Resolves the positions of a [Gesture] into screen pixels, from the detection results and the scenario counters.
 *
 * @param processingState the state of the current processing, holding the counters.
 * @param gestureBuilder the builder the resolved fingers are meant for.
 */
internal class GestureResolver(
    private val processingState: ProcessingState,
    private val gestureBuilder: AndroidGestureBuilder,
) {

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
                    fingerBuilder.release()
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
        val off = resolveGesturePosition(motion.offset, event, results) ?: Point(0, 0)

        return Point(base.x + off.x, base.y + off.y)
    }

    private fun resolveGesturePosition(position: GesturePosition, event: Event, results: ConditionsResults?): Point? =
        when (position) {
            is GesturePosition.Coordinates -> {
                val x = resolveGestureCoordinateValue(position.x)
                val y = resolveGestureCoordinateValue(position.y)

                if (x != null && y != null) Point(x, y)
                else {
                    Log.d(TAG, "Cannot resolve position for $position, coordinates are null")
                    null
                }
            }

            is GesturePosition.Condition -> {
                if (event !is ScreenEvent) {
                    Log.d(TAG, "Cannot resolve position for $position, condition must be a screen event")
                    return null
                }

                val condId = position.conditionId
                val result = when {
                    event.conditionOperator == AND && condId != null -> {
                        val condResult = results?.getScreenConditionResult(condId.databaseId)
                        if (condResult == null) {
                            Log.d(TAG, "Cannot resolve position for $position, condition has not been processed")
                            null
                        } else condResult
                    }

                    event.conditionOperator == OR ->
                        results?.getFirstScreenConditionDetectedResult()

                    else -> {
                        Log.d(TAG, "Cannot find a condition to resolve, config is invalid")
                        null
                    }
                }

                result?.position?.let { Point(it.x, it.y) }
            }
        }

    private fun resolveGestureCoordinateValue(coordinatesValue: GestureCoordinatesValue): Int? =
        when (coordinatesValue) {
            is GestureCoordinatesValue.Literal ->
                coordinatesValue.value
            is GestureCoordinatesValue.Counter ->
                processingState.getCounterValue(coordinatesValue.counterName)?.toInt()
        }
}

private const val TAG = "GestureResolver"
