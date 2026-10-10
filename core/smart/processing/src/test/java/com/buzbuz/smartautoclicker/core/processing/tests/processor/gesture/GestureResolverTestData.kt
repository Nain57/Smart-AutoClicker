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
package com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture

import android.graphics.Point
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult

/** Builds the domain objects consumed by the gesture resolver. */
internal object GestureResolverTestData {

    const val COUNTER_NAME = "counter"
    const val COUNTER_VALUE = 300

    private val SCENARIO_ID = Identifier(databaseId = 1L)
    private val EVENT_ID = Identifier(databaseId = 2L)
    private val ITEM_ID = Identifier(databaseId = 3L)

    fun newProcessingState(): ProcessingState = ProcessingState(
        screenEvents = emptyList(),
        triggerEvents = emptyList(),
        counters = listOf(Counter(COUNTER_NAME, COUNTER_VALUE.toDouble(), SCENARIO_ID)),
        progressListener = null,
    )

    fun newScreenEvent(
        @ConditionOperator operator: Int = OR,
        conditions: List<ScreenCondition> = emptyList(),
    ): ScreenEvent = ScreenEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "Event",
        conditionOperator = operator,
        conditions = conditions,
        priority = 0,
        keepDetecting = false,
        cooldownMs = 0L,
    )

    fun newTriggerEvent(): TriggerEvent =
        TriggerEvent(id = EVENT_ID, scenarioId = SCENARIO_ID, name = "Event", conditionOperator = OR)

    fun newCondition(id: Long): ScreenCondition.Image = ScreenCondition.Image(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "Condition",
        threshold = 10,
        shouldBeDetected = true,
        priority = 0,
        path = "path",
        area = Rect(),
        detectionType = EXACT,
    )

    fun newConditionsResults(vararg results: Pair<ScreenCondition, Point?>): ConditionsResults =
        ConditionsResults().apply {
            results.forEach { (condition, position) ->
                addResult(
                    conditionId = condition.id.databaseId,
                    result = ProcessedConditionResult.Screen(
                        isFulfilled = true,
                        haveBeenDetected = true,
                        condition = condition,
                        confidenceRate = 100.0,
                        position = position,
                        size = position?.let { Point(10, 10) },
                    ),
                )
            }
        }

    fun newGesture(vararg fingers: GestureFinger): Gesture =
        Gesture(id = ITEM_ID, eventId = EVENT_ID, name = "Gesture", priority = 0, fingers = fingers.toList())

    fun newFinger(startTimeMs: Long = 0L, vararg motions: GestureFingerMotion): GestureFinger =
        GestureFinger(id = ITEM_ID, startTimeMs = startTimeMs, motions = motions.toList())

    fun newTapFinger(x: Int, y: Int, startTimeMs: Long = 0L): GestureFinger =
        newFinger(startTimeMs, press(x, y), release())

    fun coordinates(x: Int, y: Int): GesturePosition.Coordinates =
        GesturePosition.Coordinates(GestureCoordinatesValue.Literal(x), GestureCoordinatesValue.Literal(y))

    fun press(x: Int, y: Int): GestureFingerMotion.Press =
        press(coordinates(x, y))

    fun press(position: GesturePosition, offset: GesturePosition = coordinates(0, 0)): GestureFingerMotion.Press =
        GestureFingerMotion.Press(ITEM_ID, position, offset)

    fun move(x: Int, y: Int, durationMs: Long): GestureFingerMotion.Move =
        move(coordinates(x, y), durationMs = durationMs)

    fun move(
        position: GesturePosition,
        offset: GesturePosition = coordinates(0, 0),
        durationMs: Long,
    ): GestureFingerMotion.Move =
        GestureFingerMotion.Move(ITEM_ID, position, offset, durationMs)

    fun wait(durationMs: Long): GestureFingerMotion.Wait =
        GestureFingerMotion.Wait(ITEM_ID, durationMs)

    fun release(): GestureFingerMotion.Release =
        GestureFingerMotion.Release(ITEM_ID)
}

