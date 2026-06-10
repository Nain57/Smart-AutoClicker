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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
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
