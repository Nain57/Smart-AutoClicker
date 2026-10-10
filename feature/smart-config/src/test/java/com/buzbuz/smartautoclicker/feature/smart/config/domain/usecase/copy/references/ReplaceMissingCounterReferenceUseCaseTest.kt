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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import android.util.Log

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ReplaceMissingCounterReferenceUseCaseTest {

    private lateinit var useCase: ReplaceMissingCounterReferenceUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        useCase = ReplaceMissingCounterReferenceUseCase()
    }

    // region ChangeCounter action replacement

    @Test
    fun `replace counter name in change counter action`() {
        val action = changeCounter(counterName = OLD_COUNTER, operationValue = CounterOperationValue.Number(1.0))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedAction = result.actions[0] as ChangeCounter
        assertEquals(NEW_COUNTER, updatedAction.counterName)
    }

    @Test
    fun `replace counter operation value in change counter action`() {
        val action = changeCounter(counterName = "other", operationValue = CounterOperationValue.Counter(OLD_COUNTER))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedAction = result.actions[0] as ChangeCounter
        assertEquals(CounterOperationValue.Counter(NEW_COUNTER), updatedAction.operationValue)
        assertEquals("other", updatedAction.counterName)
    }

    @Test
    fun `replace both counter name and operation value in change counter action`() {
        val action = changeCounter(counterName = OLD_COUNTER, operationValue = CounterOperationValue.Counter(OLD_COUNTER))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedAction = result.actions[0] as ChangeCounter
        assertEquals(NEW_COUNTER, updatedAction.counterName)
        assertEquals(CounterOperationValue.Counter(NEW_COUNTER), updatedAction.operationValue)
    }

    // endregion

    // region Notification action replacement

    @Test
    fun `replace counter reference in notification message text`() {
        val action = notification(messageText = "Value: {$OLD_COUNTER}")
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedAction = result.actions[0] as Notification
        assertEquals("Value: {$NEW_COUNTER}", updatedAction.messageText)
    }

    // endregion

    // region SetText action replacement

    @Test
    fun `replace counter reference in set text`() {
        val action = setText(text = "Value: {$OLD_COUNTER}")
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedAction = result.actions[0] as SetText
        assertEquals("Value: {$NEW_COUNTER}", updatedAction.text)
    }

    // endregion

    // region Gesture action replacement

    @Test
    fun `replace counter x coordinate in gesture press position`() {
        val action = gesture(finger(pressMotion(GesturePosition.Coordinates(
            GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0),
        ))))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedCoords = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedCoords.x)
    }

    @Test
    fun `replace counter y coordinate in gesture press position`() {
        val action = gesture(finger(pressMotion(GesturePosition.Coordinates(
            GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Counter(OLD_COUNTER),
        ))))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedCoords = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedCoords.y)
    }

    @Test
    fun `replace counter coordinate in gesture move position`() {
        val press = GestureFingerMotion.Press(
            id = Identifier(databaseId = 51L),
            position = GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Literal(0)),
            offset = GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Literal(0)),
        )
        val move = GestureFingerMotion.Move(
            id = Identifier(databaseId = 52L),
            position = GesturePosition.Coordinates(GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0)),
            offset = GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Literal(0)),
            durationMs = 100L,
        )
        val action = gesture(finger(press, move))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedCoords = (updatedGesture.fingers[0].motions[1] as GestureFingerMotion.Move).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedCoords.x)
    }

    @Test
    fun `replace counter coordinate in gesture press offset`() {
        val action = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Counter(OLD_COUNTER)),
        )))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedOffset = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).offset as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedOffset.y)
    }

    @Test
    fun `replace counter coordinate in gesture move offset`() {
        val action = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(
                position = literalPosition(),
                offset = GesturePosition.Coordinates(GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0)),
            ),
            releaseMotion(),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedOffset = (updatedGesture.fingers[0].motions[1] as GestureFingerMotion.Move).offset as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedOffset.x)
    }

    @Test
    fun `replace counter reference in every finger of the gesture`() {
        val action = gesture(
            finger(pressMotion(GesturePosition.Coordinates(GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0)))),
            finger(pressMotion(GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Counter(OLD_COUNTER)))),
        )
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val firstCoords = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).position as GesturePosition.Coordinates
        val secondCoords = (updatedGesture.fingers[1].motions[0] as GestureFingerMotion.Press).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), firstCoords.x)
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), secondCoords.y)
    }

    @Test
    fun `gesture with non-matching counter, condition position and literal coordinates is unchanged`() {
        val action = gesture(finger(
            pressMotion(GesturePosition.Condition(Identifier(databaseId = 100L))),
            moveMotion(GesturePosition.Coordinates(GestureCoordinatesValue.Counter("other"), GestureCoordinatesValue.Literal(0))),
            waitMotion(),
            releaseMotion(),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region Click action replacement

    @Test
    fun `replace counter x coordinate in click position`() {
        val action = click(position = GesturePosition.Coordinates(
            GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedCoords = (result.actions[0] as Click).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedCoords.x)
        assertEquals(GestureCoordinatesValue.Literal(0), updatedCoords.y)
    }

    @Test
    fun `replace counter y coordinate in click position`() {
        val action = click(position = GesturePosition.Coordinates(
            GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Counter(OLD_COUNTER),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedCoords = (result.actions[0] as Click).position as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedCoords.y)
    }

    @Test
    fun `replace counter coordinate in click on condition offset`() {
        val conditionId = Identifier(databaseId = 100L)
        val action = click(
            position = GesturePosition.Condition(conditionId),
            offset = GesturePosition.Coordinates(GestureCoordinatesValue.Counter(OLD_COUNTER), GestureCoordinatesValue.Literal(0)),
        )
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedClick = result.actions[0] as Click
        assertEquals(GesturePosition.Condition(conditionId), updatedClick.position)
        val updatedOffset = updatedClick.offset as GesturePosition.Coordinates
        assertEquals(GestureCoordinatesValue.Counter(NEW_COUNTER), updatedOffset.x)
    }

    @Test
    fun `click with non-matching counter is unchanged`() {
        val action = click(position = GesturePosition.Coordinates(
            GestureCoordinatesValue.Counter("other"), GestureCoordinatesValue.Literal(0),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region Unsupported action type

    @Test
    fun `unsupported action type returns unchanged event`() {
        val action = mockk<Pause>(relaxed = true)
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region Action not found in event

    @Test
    fun `action not found in event actions returns unchanged event`() {
        val action = changeCounter(counterName = OLD_COUNTER, operationValue = CounterOperationValue.Number(1.0))
        val otherAction = changeCounter(counterName = "other", operationValue = CounterOperationValue.Number(2.0))
        val event = triggerEventWithActions(listOf(otherAction))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region TriggerCondition.OnCounterCountReached condition replacement

    @Test
    fun `replace counter name in counter reached condition`() {
        val condition = counterReachedCondition(counterName = OLD_COUNTER, counterValue = CounterOperationValue.Number(1.0))
        val event = triggerEventWithConditions(listOf(condition))
        val item = ItemWithMissingReferences.ConditionItem(item = condition, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedCondition = result.conditions[0] as TriggerCondition.OnCounterCountReached
        assertEquals(NEW_COUNTER, updatedCondition.counterName)
    }

    @Test
    fun `replace counter value in counter reached condition`() {
        val condition = counterReachedCondition(counterName = "other", counterValue = CounterOperationValue.Counter(OLD_COUNTER))
        val event = triggerEventWithConditions(listOf(condition))
        val item = ItemWithMissingReferences.ConditionItem(item = condition, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER) as TriggerEvent
        val updatedCondition = result.conditions[0] as TriggerCondition.OnCounterCountReached
        assertEquals(CounterOperationValue.Counter(NEW_COUNTER), updatedCondition.counterValue)
        assertEquals("other", updatedCondition.counterName)
    }

    // endregion

    // region Unsupported condition type

    @Test
    fun `unsupported condition type returns unchanged event`() {
        val condition = TriggerCondition.OnTimerReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "timer",
            durationMs = 1000L,
            restartWhenReached = false,
        )
        val event = triggerEventWithConditions(listOf(condition))
        val item = ItemWithMissingReferences.ConditionItem(item = condition, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region Condition not found in event

    @Test
    fun `condition not found in event conditions returns unchanged event`() {
        val condition = counterReachedCondition(counterName = OLD_COUNTER, counterValue = CounterOperationValue.Number(1.0))
        val otherCondition = counterReachedCondition(counterName = "other", counterValue = CounterOperationValue.Number(2.0))
        val event = triggerEventWithConditions(listOf(otherCondition))
        val item = ItemWithMissingReferences.ConditionItem(item = condition, missingReferences = emptyList())
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(event, item, missingRef, NEW_COUNTER)
        assertEquals(event, result)
    }

    // endregion

    // region Item level replacement

    @Test
    fun `replace counter reference in standalone action`() {
        val action = changeCounter(counterName = OLD_COUNTER, operationValue = CounterOperationValue.Number(1.0))
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(action, missingRef, NEW_COUNTER) as ChangeCounter
        assertEquals(NEW_COUNTER, result.counterName)
    }

    @Test
    fun `unsupported standalone action type returns null`() {
        val action = mockk<Pause>(relaxed = true)
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        assertNull(useCase(action, missingRef, NEW_COUNTER))
    }

    @Test
    fun `replace counter reference in standalone condition`() {
        val condition = counterReachedCondition(counterName = OLD_COUNTER, counterValue = CounterOperationValue.Number(1.0))
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        val result = useCase(condition, missingRef, NEW_COUNTER) as TriggerCondition.OnCounterCountReached
        assertEquals(NEW_COUNTER, result.counterName)
    }

    @Test
    fun `unsupported standalone condition type returns null`() {
        val condition = TriggerCondition.OnTimerReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "timer",
            durationMs = 1000L,
            restartWhenReached = false,
        )
        val missingRef = MissingCopyReference.CounterReference(OLD_COUNTER)

        assertNull(useCase(condition, missingRef, NEW_COUNTER))
    }

    // endregion

    private fun triggerEventWithActions(actions: List<com.buzbuz.smartautoclicker.core.domain.model.action.Action>) = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "test event",
        conditionOperator = AND,
        actions = actions,
        conditions = emptyList(),
    )

    private fun triggerEventWithConditions(conditions: List<TriggerCondition>) = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "test event",
        conditionOperator = AND,
        actions = emptyList(),
        conditions = conditions,
    )

    private fun gesture(vararg fingers: GestureFinger) = Gesture(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        fingers = fingers.toList(),
    )

    private fun finger(vararg motions: GestureFingerMotion) = GestureFinger(
        id = Identifier(databaseId = 50L),
        startTimeMs = 0L,
        motions = motions.toList(),
    )

    private fun literalPosition() = GesturePosition.Coordinates(
        GestureCoordinatesValue.Literal(100), GestureCoordinatesValue.Literal(200),
    )

    private fun pressMotion(position: GesturePosition, offset: GesturePosition = NO_OFFSET) = GestureFingerMotion.Press(
        id = Identifier(databaseId = 51L),
        position = position,
        offset = offset,
    )

    private fun moveMotion(position: GesturePosition, offset: GesturePosition = NO_OFFSET) = GestureFingerMotion.Move(
        id = Identifier(databaseId = 52L),
        position = position,
        offset = offset,
        durationMs = 100L,
    )

    private fun waitMotion() = GestureFingerMotion.Wait(
        id = Identifier(databaseId = 53L),
        durationMs = 100L,
    )

    private fun releaseMotion() = GestureFingerMotion.Release(
        id = Identifier(databaseId = 54L),
    )

    private fun click(position: GesturePosition?, offset: GesturePosition? = null) = Click(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        pressDuration = 100L,
        position = position,
        offset = offset,
    )

    private fun changeCounter(counterName: String, operationValue: CounterOperationValue) = ChangeCounter(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        counterName = counterName,
        operation = ChangeCounter.OperationType.SET,
        operationValue = operationValue,
    )

    private fun notification(messageText: String) = Notification(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        messageText = messageText,
        channelImportance = 3,
    )

    private fun setText(text: String) = SetText(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        text = text,
        validateInput = false,
    )

    private fun counterReachedCondition(counterName: String, counterValue: CounterOperationValue) =
        TriggerCondition.OnCounterCountReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "counter condition",
            counterName = counterName,
            comparisonOperation = ComparisonOperation.GREATER,
            counterValue = counterValue,
        )

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val CONDITION_ID = Identifier(databaseId = 2L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val SCENARIO_ID = Identifier(databaseId = 20L)
        const val OLD_COUNTER = "oldCounter"
        const val NEW_COUNTER = "newCounter"
        val NO_OFFSET = GesturePosition.Coordinates(GestureCoordinatesValue.Literal(0), GestureCoordinatesValue.Literal(0))
    }
}
