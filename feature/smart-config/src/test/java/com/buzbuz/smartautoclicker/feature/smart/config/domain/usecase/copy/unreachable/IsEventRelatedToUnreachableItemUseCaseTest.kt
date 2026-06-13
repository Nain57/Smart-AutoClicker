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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.unreachable

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import io.mockk.every
import io.mockk.mockk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsEventRelatedToUnreachableItemUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var isConditionUseCase: IsConditionRelatedToUnreachableItemUseCase
    private lateinit var isActionUseCase: IsActionRelatedToUnreachableItemUseCase
    private lateinit var useCase: IsEventRelatedToUnreachableItemUseCase

    @Before
    fun setUp() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        isConditionUseCase = IsConditionRelatedToUnreachableItemUseCase(mockEditionRepository)
        isActionUseCase = IsActionRelatedToUnreachableItemUseCase(mockEditionRepository)
        useCase = IsEventRelatedToUnreachableItemUseCase(isActionUseCase, isConditionUseCase)
    }

    @Test
    fun `event with no conditions and no actions is not related to unreachable item`() {
        val event = eventWithConditionsAndActions(conditions = emptyList(), actions = emptyList())
        assertFalse(useCase(event))
    }

    @Test
    fun `event with only reachable conditions and actions is not related to unreachable item`() {
        every { mockEditionState.getCounter(REACHABLE_COUNTER_NAME) } returns mockk<Counter>()
        val condition = reachableCounterCondition()
        val action = mockk<Pause>(relaxed = true)
        val event = eventWithConditionsAndActions(conditions = listOf(condition), actions = listOf(action))
        assertFalse(useCase(event))
    }

    @Test
    fun `event with an unreachable condition returns true without checking actions`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val unreachableCondition = unreachableCounterCondition()
        val action = mockk<Pause>(relaxed = true)
        val event = eventWithConditionsAndActions(conditions = listOf(unreachableCondition), actions = listOf(action))
        assertTrue(useCase(event))
    }

    @Test
    fun `event with reachable conditions but unreachable action is related to unreachable item`() {
        every { mockEditionState.getCounter(REACHABLE_COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val condition = reachableCounterCondition()
        val unreachableAction = unreachableChangeCounterAction()
        val event = eventWithConditionsAndActions(conditions = listOf(condition), actions = listOf(unreachableAction))
        assertTrue(useCase(event))
    }

    @Test
    fun `first unreachable condition short-circuits remaining conditions`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val unreachableCondition = unreachableCounterCondition()
        val secondCondition = mockk<Condition>(relaxed = true)
        val event = eventWithConditionsAndActions(
            conditions = listOf(unreachableCondition, secondCondition),
            actions = emptyList(),
        )
        // Should return true without evaluating secondCondition (relaxed mock — no getCounter stubbing needed)
        assertTrue(useCase(event))
    }

    private fun eventWithConditionsAndActions(conditions: List<Condition>, actions: List<Action>): Event =
        mockk(relaxed = true) {
            every { this@mockk.conditions } returns conditions
            every { this@mockk.actions } returns actions
        }

    private fun reachableCounterCondition(): TriggerCondition.OnCounterCountReached =
        TriggerCondition.OnCounterCountReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "reachable",
            counterName = REACHABLE_COUNTER_NAME,
            comparisonOperation = com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation.GREATER,
            counterValue = CounterOperationValue.Number(5.0),
        )

    private fun unreachableCounterCondition(): TriggerCondition.OnCounterCountReached =
        TriggerCondition.OnCounterCountReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "unreachable",
            counterName = COUNTER_NAME,
            comparisonOperation = com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation.GREATER,
            counterValue = CounterOperationValue.Number(5.0),
        )

    private fun unreachableChangeCounterAction() = com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        counterName = COUNTER_NAME,
        operation = com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter.OperationType.SET,
        operationValue = CounterOperationValue.Number(1.0),
    )

    private companion object {
        val CONDITION_ID = Identifier(databaseId = 1L)
        val ACTION_ID = Identifier(databaseId = 2L)
        val EVENT_ID = Identifier(databaseId = 10L)
        const val COUNTER_NAME = "unreachableCounter"
        const val REACHABLE_COUNTER_NAME = "reachableCounter"
    }
}
