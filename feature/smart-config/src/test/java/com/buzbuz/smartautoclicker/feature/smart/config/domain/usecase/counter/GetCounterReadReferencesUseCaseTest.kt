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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.model.CounterReference

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GetCounterReadReferencesUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: GetCounterReadReferencesUseCase

    @Before
    fun setUp() {
        useCase = GetCounterReadReferencesUseCase(mockEditionRepository)
    }

    @Test
    fun `no events emits empty map`() = runTest {
        mockEvents(emptyList())

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `event with unrelated conditions and actions emits empty map`() = runTest {
        val timerCondition = mockk<TriggerCondition.OnTimerReached>(relaxed = true)
        val changeCounterNumberValue = changeCounterAction(
            counterName = COUNTER_A,
            operationValue = CounterOperationValue.Number(1.0),
        )
        mockEvents(listOf(event(
            conditions = listOf(timerCondition),
            actions = listOf(changeCounterNumberValue),
        )))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `OnCounterCountReached creates read reference for its counterName`() = runTest {
        val condition = counterReachedCondition(counterName = COUNTER_A)
        mockEvents(listOf(event(conditions = listOf(condition))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ConditionElement
        assertEquals(condition, ref?.condition)
    }

    @Test
    fun `OnCounterCountReached with Counter operationValue creates read reference for both counters`() = runTest {
        val condition = counterReachedCondition(
            counterName = COUNTER_A,
            counterValue = CounterOperationValue.Counter(COUNTER_B),
        )
        mockEvents(listOf(event(conditions = listOf(condition))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A, COUNTER_B), result.keys)
    }

    @Test
    fun `ScreenCondition Number with Counter operationValue creates read reference`() = runTest {
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Counter(COUNTER_A)
        }
        mockEvents(listOf(event(conditions = listOf(condition))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ConditionElement
        assertEquals(condition, ref?.condition)
    }

    @Test
    fun `ScreenCondition Number with Number operationValue creates no read reference`() = runTest {
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Number(5.0)
        }
        mockEvents(listOf(event(conditions = listOf(condition))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `Notification with counter reference in messageText creates read reference`() = runTest {
        val action = notificationAction(messageText = "Score: {$COUNTER_A}")
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Notification with multiple counter references creates read reference for each`() = runTest {
        val action = notificationAction(messageText = "{$COUNTER_A} vs {$COUNTER_B}")
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A, COUNTER_B), result.keys)
    }

    @Test
    fun `SetText with counter reference in text creates read reference`() = runTest {
        val action = setTextAction(text = "Value: {$COUNTER_A}")
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Gesture with Counter coordinate creates read reference`() = runTest {
        val action = gestureAction(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Literal(0),
        ))
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Gesture with Literal coordinate creates no read reference`() = runTest {
        val action = gestureAction(Position.Coordinates(
            CoordinatesValue.Literal(100), CoordinatesValue.Literal(200),
        ))
        mockEvents(listOf(event(actions = listOf(action))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `Gesture with Condition position creates no read reference`() = runTest {
        val action = gestureAction(Position.Condition(Identifier(databaseId = 100L)))
        mockEvents(listOf(event(actions = listOf(action))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `Gesture with Counter in press offset creates read reference`() = runTest {
        val action = gestureAction(
            pressPosition = literalPosition(),
            pressOffset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_A)),
        )
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Gesture with Counter in move position and offset creates read reference for each`() = runTest {
        val action = gestureAction(
            pressPosition = literalPosition(),
            move = GestureFingerMotion.Move(
                id = Identifier(databaseId = 52L),
                position = Position.Coordinates(CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Literal(0)),
                offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_B)),
                durationMs = 100L,
            ),
        )
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A, COUNTER_B), result.keys)
    }

    @Test
    fun `Gesture with both Counter coordinates creates read reference for each`() = runTest {
        val action = gestureAction(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Counter(COUNTER_B),
        ))
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A, COUNTER_B), result.keys)
    }

    @Test
    fun `Gesture referencing same counter in several fingers creates one reference`() = runTest {
        val counterPosition = Position.Coordinates(CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Literal(0))
        val action = gestureAction(counterPosition).let { gesture ->
            gesture.copy(fingers = gesture.fingers + gesture.fingers[0].copy(id = Identifier(databaseId = 60L)))
        }
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        assertEquals(1, result[COUNTER_A]?.size)
    }

    @Test
    fun `Click with Counter coordinate creates read reference`() = runTest {
        val action = clickAction(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Literal(0),
        ))
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Click with both Counter coordinates creates read reference for each`() = runTest {
        val action = clickAction(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Counter(COUNTER_B),
        ))
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A, COUNTER_B), result.keys)
    }

    @Test
    fun `Click with Literal coordinate creates no read reference`() = runTest {
        val action = clickAction(position = literalPosition())
        mockEvents(listOf(event(actions = listOf(action))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `Click on Condition with Counter offset creates read reference`() = runTest {
        val action = clickAction(
            position = Position.Condition(Identifier(databaseId = 100L)),
            offset = Position.Coordinates(CoordinatesValue.Counter(COUNTER_A), CoordinatesValue.Literal(0)),
        )
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(setOf(COUNTER_A), result.keys)
        val ref = result[COUNTER_A]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `Click on Condition without offset creates no read reference`() = runTest {
        val action = clickAction(position = Position.Condition(Identifier(databaseId = 100L)))
        mockEvents(listOf(event(actions = listOf(action))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `ChangeCounter with Counter operationValue creates read reference`() = runTest {
        val action = changeCounterAction(
            counterName = COUNTER_A,
            operationValue = CounterOperationValue.Counter(COUNTER_B),
        )
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        // COUNTER_B is the read reference (operationValue), not COUNTER_A (write target)
        assertEquals(setOf(COUNTER_B), result.keys)
        val ref = result[COUNTER_B]?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `references for same counter across multiple events are merged`() = runTest {
        val condition1 = counterReachedCondition(counterName = COUNTER_A)
        val condition2 = counterReachedCondition(counterName = COUNTER_A)
        mockEvents(listOf(
            event(conditions = listOf(condition1)),
            event(conditions = listOf(condition2)),
        ))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(2, result[COUNTER_A]?.size)
    }

    // region helpers

    private fun mockEvents(events: List<Event>) {
        every { hint(Flow::class).run { mockEditionState.allEditedEventsFlow } } returns flowOf(events)
    }

    private fun event(
        actions: List<Action> = emptyList(),
        conditions: List<Condition> = emptyList(),
    ): Event = mockk(relaxed = true) {
        every { this@mockk.actions } returns actions
        every { this@mockk.conditions } returns conditions
    }

    private fun literalPosition() = Position.Coordinates(
        CoordinatesValue.Literal(100), CoordinatesValue.Literal(200),
    )

    private fun gestureAction(
        pressPosition: Position,
        pressOffset: Position = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0)),
        move: GestureFingerMotion.Move? = null,
    ): Gesture = Gesture(
        id = Identifier(databaseId = 4L),
        eventId = Identifier(databaseId = 1L),
        name = "Gesture",
        priority = 0,
        fingers = listOf(GestureFinger(
            id = Identifier(databaseId = 50L),
            startTimeMs = 0L,
            motions = listOfNotNull(
                GestureFingerMotion.Press(
                    id = Identifier(databaseId = 51L),
                    position = pressPosition,
                    offset = pressOffset,
                ),
                move,
                GestureFingerMotion.Release(id = Identifier(databaseId = 54L)),
            ),
        )),
    )

    private fun clickAction(position: Position, offset: Position? = null): Click = Click(
        id = Identifier(databaseId = 5L),
        eventId = Identifier(databaseId = 1L),
        name = "Click",
        priority = 0,
        pressDuration = 100L,
        position = position,
        offset = offset,
    )

    private fun counterReachedCondition(
        counterName: String,
        counterValue: CounterOperationValue = CounterOperationValue.Number(5.0),
    ): TriggerCondition.OnCounterCountReached = TriggerCondition.OnCounterCountReached(
        id = Identifier(databaseId = 1L),
        eventId = Identifier(databaseId = 1L),
        name = "Counter reached",
        counterName = counterName,
        comparisonOperation = ComparisonOperation.EQUALS,
        counterValue = counterValue,
    )

    private fun changeCounterAction(
        counterName: String,
        operationValue: CounterOperationValue = CounterOperationValue.Number(1.0),
    ): ChangeCounter = ChangeCounter(
        id = Identifier(databaseId = 1L),
        eventId = Identifier(databaseId = 1L),
        name = "Change $counterName",
        priority = 0,
        counterName = counterName,
        operation = ChangeCounter.OperationType.ADD,
        operationValue = operationValue,
    )

    private fun notificationAction(messageText: String): Notification = Notification(
        id = Identifier(databaseId = 2L),
        eventId = Identifier(databaseId = 1L),
        name = "Notification",
        priority = 0,
        messageText = messageText,
        channelImportance = 3,
    )

    private fun setTextAction(text: String): SetText = SetText(
        id = Identifier(databaseId = 3L),
        eventId = Identifier(databaseId = 1L),
        name = "SetText",
        priority = 0,
        text = text,
        validateInput = false,
    )

    // endregion

    private companion object {
        const val COUNTER_A = "counterA"
        const val COUNTER_B = "counterB"
    }
}
