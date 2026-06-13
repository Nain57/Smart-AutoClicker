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
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import io.mockk.every
import io.mockk.mockk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsConditionRelatedToUnreachableItemUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: IsConditionRelatedToUnreachableItemUseCase

    @Before
    fun setUp() {
        useCase = IsConditionRelatedToUnreachableItemUseCase(mockEditionRepository)
    }

    // region ScreenCondition.Color/Image/Text — always false

    @Test
    fun `screen color condition is not related to unreachable item`() {
        val condition = mockk<ScreenCondition.Color>(relaxed = true)
        assertFalse(useCase(condition))
    }

    @Test
    fun `screen image condition is not related to unreachable item`() {
        val condition = mockk<ScreenCondition.Image>(relaxed = true)
        assertFalse(useCase(condition))
    }

    @Test
    fun `screen text condition is not related to unreachable item`() {
        val condition = mockk<ScreenCondition.Text>(relaxed = true)
        assertFalse(useCase(condition))
    }

    // endregion

    // region TriggerCondition.OnBroadcastReceived/OnTimerReached — always false

    @Test
    fun `trigger broadcast received condition is not related to unreachable item`() {
        val condition = TriggerCondition.OnBroadcastReceived(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "broadcast",
            intentAction = "com.test.ACTION",
        )
        assertFalse(useCase(condition))
    }

    @Test
    fun `trigger timer reached condition is not related to unreachable item`() {
        val condition = TriggerCondition.OnTimerReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "timer",
            durationMs = 1000L,
            restartWhenReached = false,
        )
        assertFalse(useCase(condition))
    }

    // endregion

    // region ScreenCondition.Number

    @Test
    fun `screen number condition with number value is not related to unreachable item`() {
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Number(5.0)
        }
        assertFalse(useCase(condition))
    }

    @Test
    fun `screen number condition with reachable counter value is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Counter(COUNTER_NAME)
        }
        assertFalse(useCase(condition))
    }

    @Test
    fun `screen number condition with unreachable counter value is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Counter(COUNTER_NAME)
        }
        assertTrue(useCase(condition))
    }

    // endregion

    // region TriggerCondition.OnCounterCountReached

    @Test
    fun `counter reached condition with reachable counter name and number value is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val condition = triggerCounterCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Number(10.0),
        )
        assertFalse(useCase(condition))
    }

    @Test
    fun `counter reached condition with unreachable counter name is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val condition = triggerCounterCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Number(10.0),
        )
        assertTrue(useCase(condition))
    }

    @Test
    fun `counter reached condition with reachable counter name but unreachable counter value is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val condition = triggerCounterCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME),
        )
        assertTrue(useCase(condition))
    }

    @Test
    fun `counter reached condition with both counter name and counter value reachable is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns mockk<Counter>()
        val condition = triggerCounterCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME),
        )
        assertFalse(useCase(condition))
    }

    // endregion

    private fun triggerCounterCondition(
        counterName: String,
        counterValue: CounterOperationValue,
    ) = TriggerCondition.OnCounterCountReached(
        id = CONDITION_ID,
        eventId = EVENT_ID,
        name = "counter condition",
        counterName = counterName,
        comparisonOperation = ComparisonOperation.GREATER,
        counterValue = counterValue,
    )

    private companion object {
        val CONDITION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        const val COUNTER_NAME = "myCounter"
        const val COUNTER_VALUE_NAME = "otherCounter"
    }
}
