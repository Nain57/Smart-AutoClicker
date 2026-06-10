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

import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ReplaceCounterUseCaseTest {

    private val mockEditionState: IEditionState = mockk(relaxed = true)
    private val mockEditionRepository: EditionRepository = mockk(relaxed = true) {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: ReplaceCounterUseCase

    @Before
    fun setUp() {
        useCase = ReplaceCounterUseCase(mockEditionRepository)
    }

    @Test
    fun `no events does nothing`() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        useCase(FROM_COUNTER, TO_COUNTER)

        verify(exactly = 0) { mockEditionRepository.startEventEdition(any()) }
    }

    @Test
    fun `event with no matching conditions or actions wraps event edition only`() {
        val event = event(
            conditions = listOf(mockk<TriggerCondition.OnTimerReached>(relaxed = true)),
            actions = listOf(mockk<Action>(relaxed = true)),
        )
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        useCase(FROM_COUNTER, TO_COUNTER)

        verify(exactly = 1) { mockEditionRepository.startEventEdition(event) }
        verify(exactly = 1) { mockEditionRepository.upsertEditedEvent() }
        verify(exactly = 0) { mockEditionRepository.startConditionEdition(any()) }
        verify(exactly = 0) { mockEditionRepository.startActionEdition(any()) }
    }

    @Test
    fun `ChangeCounter with matching counterName is updated`() {
        val action = changeCounterAction(counterName = FROM_COUNTER_NAME)
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startActionEdition(action)
            mockEditionRepository.updateEditedAction(action.copy(counterName = TO_COUNTER_NAME))
            mockEditionRepository.upsertEditedAction()
        }
    }

    @Test
    fun `ChangeCounter with matching Counter operationValue is updated`() {
        val action = changeCounterAction(
            counterName = "other",
            operationValue = CounterOperationValue.Counter(FROM_COUNTER_NAME),
        )
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startActionEdition(action)
            mockEditionRepository.updateEditedAction(
                action.copy(operationValue = CounterOperationValue.Counter(TO_COUNTER_NAME))
            )
            mockEditionRepository.upsertEditedAction()
        }
    }

    @Test
    fun `ChangeCounter with both counterName and operationValue matching is updated in one call`() {
        val action = changeCounterAction(
            counterName = FROM_COUNTER_NAME,
            operationValue = CounterOperationValue.Counter(FROM_COUNTER_NAME),
        )
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startActionEdition(action)
            mockEditionRepository.updateEditedAction(
                action.copy(
                    counterName = TO_COUNTER_NAME,
                    operationValue = CounterOperationValue.Counter(TO_COUNTER_NAME),
                )
            )
            mockEditionRepository.upsertEditedAction()
        }
    }

    @Test
    fun `ChangeCounter with no match is not updated`() {
        val action = changeCounterAction(counterName = "otherCounter")
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verify(exactly = 0) { mockEditionRepository.startActionEdition(any()) }
    }

    @Test
    fun `TriggerCondition OnCounterCountReached with matching counterName is updated`() {
        val condition = counterReachedCondition(counterName = FROM_COUNTER_NAME)
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(conditions = listOf(condition)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startConditionEdition(condition)
            mockEditionRepository.updateEditedCondition(condition.copy(counterName = TO_COUNTER_NAME))
            mockEditionRepository.upsertEditedCondition()
        }
    }

    @Test
    fun `TriggerCondition OnCounterCountReached with matching Counter counterValue is updated`() {
        val condition = counterReachedCondition(
            counterName = "other",
            counterValue = CounterOperationValue.Counter(FROM_COUNTER_NAME),
        )
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(conditions = listOf(condition)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startConditionEdition(condition)
            mockEditionRepository.updateEditedCondition(
                condition.copy(counterValue = CounterOperationValue.Counter(TO_COUNTER_NAME))
            )
            mockEditionRepository.upsertEditedCondition()
        }
    }

    @Test
    fun `ScreenCondition Number with matching Counter counterValue is updated`() {
        val condition = numberCondition(counterValue = CounterOperationValue.Counter(FROM_COUNTER_NAME))
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(conditions = listOf(condition)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startConditionEdition(condition)
            mockEditionRepository.updateEditedCondition(
                condition.copy(counterValue = CounterOperationValue.Counter(TO_COUNTER_NAME))
            )
            mockEditionRepository.upsertEditedCondition()
        }
    }

    @Test
    fun `ScreenCondition Number with Number counterValue is not updated`() {
        val condition = numberCondition(counterValue = CounterOperationValue.Number(5.0))
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(conditions = listOf(condition)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verify(exactly = 0) { mockEditionRepository.startConditionEdition(any()) }
    }

    @Test
    fun `Notification with counter reference in messageText is updated`() {
        val action = notificationAction(messageText = "Score: {$FROM_COUNTER_NAME}")
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startActionEdition(action)
            mockEditionRepository.updateEditedAction(
                action.copy(messageText = "Score: {$TO_COUNTER_NAME}")
            )
            mockEditionRepository.upsertEditedAction()
        }
    }

    @Test
    fun `Notification without counter reference is not updated`() {
        val action = notificationAction(messageText = "No counter here")
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verify(exactly = 0) { mockEditionRepository.startActionEdition(any()) }
    }

    @Test
    fun `SetText with counter reference in text is updated`() {
        val action = setTextAction(text = "Value: {$FROM_COUNTER_NAME}")
        every { mockEditionState.getAllEditedEvents() } returns listOf(event(actions = listOf(action)))

        useCase(FROM_COUNTER, TO_COUNTER)

        verifyOrder {
            mockEditionRepository.startActionEdition(action)
            mockEditionRepository.updateEditedAction(
                action.copy(text = "Value: {$TO_COUNTER_NAME}")
            )
            mockEditionRepository.upsertEditedAction()
        }
    }

    // region helpers

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

    private fun numberCondition(
        counterValue: CounterOperationValue,
    ): ScreenCondition.Number = ScreenCondition.Number(
        id = Identifier(databaseId = 1L),
        eventId = Identifier(databaseId = 1L),
        name = "Number condition",
        threshold = 0,
        shouldBeDetected = true,
        priority = 0,
        detectionArea = Rect(0, 0, 100, 100),
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
        const val FROM_COUNTER_NAME = "fromCounter"
        const val TO_COUNTER_NAME = "toCounter"

        val FROM_COUNTER = Counter(
            counterName = FROM_COUNTER_NAME,
            defaultValue = 0.0,
            scenarioId = Identifier(databaseId = 1L),
        )
        val TO_COUNTER = Counter(
            counterName = TO_COUNTER_NAME,
            defaultValue = 0.0,
            scenarioId = Identifier(databaseId = 1L),
        )
    }
}
