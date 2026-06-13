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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
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
import org.junit.Before
import org.junit.Test

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
    }
}
