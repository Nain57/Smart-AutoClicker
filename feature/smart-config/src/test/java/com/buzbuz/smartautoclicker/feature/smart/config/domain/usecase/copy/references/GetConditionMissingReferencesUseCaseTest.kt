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
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import io.mockk.every
import io.mockk.mockk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetConditionMissingReferencesUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: GetConditionMissingReferencesUseCase

    @Before
    fun setUp() {
        useCase = GetConditionMissingReferencesUseCase(mockEditionRepository)
    }

    // region Simple condition types — always no missing references

    @Test
    fun `screen color condition has no missing references`() {
        val condition = mockk<ScreenCondition.Color>(relaxed = true)
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `screen image condition has no missing references`() {
        val condition = mockk<ScreenCondition.Image>(relaxed = true)
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `screen text condition has no missing references`() {
        val condition = mockk<ScreenCondition.Text>(relaxed = true)
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `trigger broadcast received condition has no missing references`() {
        val condition = TriggerCondition.OnBroadcastReceived(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "broadcast",
            intentAction = "com.test.ACTION",
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `trigger timer reached condition has no missing references`() {
        val condition = TriggerCondition.OnTimerReached(
            id = CONDITION_ID,
            eventId = EVENT_ID,
            name = "timer",
            durationMs = 1000L,
            restartWhenReached = false,
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    // endregion

    // region ScreenCondition.Number

    @Test
    fun `screen number condition with number value has no missing references`() {
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Number(5.0)
        }
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `screen number condition with reachable counter value has no missing references`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Counter(COUNTER_NAME)
        }
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `screen number condition with missing counter value has one counter reference`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val condition = mockk<ScreenCondition.Number>(relaxed = true) {
            every { counterValue } returns CounterOperationValue.Counter(COUNTER_NAME)
        }
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    // endregion

    // region TriggerCondition.OnCounterCountReached

    @Test
    fun `counter reached condition with reachable counter name and number value has no missing references`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val condition = counterReachedCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Number(10.0),
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    @Test
    fun `counter reached condition with missing counter name has one counter reference`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val condition = counterReachedCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Number(10.0),
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `counter reached condition with reachable counter name but missing counter value has one counter reference`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val condition = counterReachedCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME),
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME), result.missingReferences[0])
    }

    @Test
    fun `counter reached condition with both counter name and counter value missing has two counter references`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val condition = counterReachedCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME),
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME)))
    }

    @Test
    fun `counter reached condition with both counter name and counter value reachable has no missing references`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns mockk<Counter>()
        val condition = counterReachedCondition(
            counterName = COUNTER_NAME,
            counterValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME),
        )
        val result = useCase(condition)
        assertConditionItem(result, condition, expectedMissingCount = 0)
    }

    // endregion

    private fun assertConditionItem(
        result: ItemWithMissingReferences.ConditionItem,
        expectedCondition: com.buzbuz.smartautoclicker.core.domain.model.condition.Condition,
        expectedMissingCount: Int,
    ) {
        assertEquals(expectedCondition, result.item)
        assertEquals(expectedMissingCount, result.missingReferences.size)
    }

    private fun counterReachedCondition(
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
