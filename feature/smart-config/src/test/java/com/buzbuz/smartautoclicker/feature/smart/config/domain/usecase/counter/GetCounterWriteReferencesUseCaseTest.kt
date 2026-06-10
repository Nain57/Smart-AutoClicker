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
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
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

class GetCounterWriteReferencesUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: GetCounterWriteReferencesUseCase

    @Before
    fun setUp() {
        useCase = GetCounterWriteReferencesUseCase(mockEditionRepository)
    }

    @Test
    fun `no events emits empty map`() = runTest {
        mockEvents(emptyList())

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `event with no ChangeCounter actions emits empty map`() = runTest {
        val notificationAction = notificationAction(messageText = "hello")
        mockEvents(listOf(event(actions = listOf(notificationAction))))

        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun `ChangeCounter action creates write reference for its counter`() = runTest {
        val action = changeCounterAction(counterName = COUNTER_A)
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        assertEquals(1, result.size)
        val refs = result[COUNTER_A]
        assertEquals(1, refs?.size)
        val ref = refs?.first() as? CounterReference.ActionElement
        assertEquals(action, ref?.action)
    }

    @Test
    fun `ChangeCounter actions for different counters produce separate entries`() = runTest {
        val actionA = changeCounterAction(counterName = COUNTER_A)
        val actionB = changeCounterAction(counterName = COUNTER_B)
        mockEvents(listOf(event(actions = listOf(actionA, actionB))))

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals(1, result[COUNTER_A]?.size)
        assertEquals(1, result[COUNTER_B]?.size)
    }

    @Test
    fun `ChangeCounter actions across multiple events are merged per counter`() = runTest {
        val event1 = event(actions = listOf(changeCounterAction(counterName = COUNTER_A)))
        val event2 = event(actions = listOf(changeCounterAction(counterName = COUNTER_A)))
        mockEvents(listOf(event1, event2))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(2, result[COUNTER_A]?.size)
    }

    @Test
    fun `ChangeCounter operationValue Counter does not create additional write reference`() = runTest {
        val action = changeCounterAction(
            counterName = COUNTER_A,
            operationValue = CounterOperationValue.Counter(COUNTER_B),
        )
        mockEvents(listOf(event(actions = listOf(action))))

        val result = useCase().first()

        // Only the written-to counter (counterName) creates a write ref, not the read operationValue
        assertEquals(setOf(COUNTER_A), result.keys)
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

    // endregion

    private companion object {
        const val COUNTER_A = "counterA"
        const val COUNTER_B = "counterB"
    }
}
