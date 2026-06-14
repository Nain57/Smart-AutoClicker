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
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetActionMissingReferencesUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }
    private val mockSmartRepository: IRepository = mockk()

    private lateinit var useCase: GetActionMissingReferencesUseCase

    @Before
    fun setUp() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        useCase = GetActionMissingReferencesUseCase(mockEditionRepository, mockSmartRepository)
    }

    // region Simple action types — always no missing references

    @Test
    fun `pause action has no missing references`() = runTest {
        val result = useCase(mockk<Pause>(relaxed = true))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `swipe action has no missing references`() = runTest {
        val result = useCase(mockk<Swipe>(relaxed = true))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `intent action has no missing references`() = runTest {
        val result = useCase(mockk<Intent>(relaxed = true))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `system action has no missing references`() = runTest {
        val result = useCase(mockk<SystemAction>(relaxed = true))
        assertActionItem(result, expectedMissingCount = 0)
    }

    // endregion

    // region ChangeCounter

    @Test
    fun `change counter with reachable counter name and number value has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val result = useCase(changeCounter(COUNTER_NAME, CounterOperationValue.Number(5.0)))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `change counter with missing counter name has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val result = useCase(changeCounter(COUNTER_NAME, CounterOperationValue.Number(5.0)))
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `change counter with reachable name but missing counter value has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val result = useCase(changeCounter(COUNTER_NAME, CounterOperationValue.Counter(COUNTER_VALUE_NAME)))
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME), result.missingReferences[0])
    }

    @Test
    fun `change counter with both name and value missing has two counter references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val result = useCase(changeCounter(COUNTER_NAME, CounterOperationValue.Counter(COUNTER_VALUE_NAME)))
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME)))
    }

    // endregion

    // region Click

    @Test
    fun `click user selected position has no missing references`() = runTest {
        val click = mockk<Click>(relaxed = true) {
            every { positionType } returns Click.PositionType.USER_SELECTED
        }
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with null conditionId has no missing references`() = runTest {
        val click = mockk<Click>(relaxed = true) {
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns null
        }
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId found in its own edited event has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId found in a different edited event has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val otherEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(otherEvent)
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `click on detected condition with conditionId in its own eventsToCopy event has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertActionItem(useCase(click, eventsToCopy = listOf(copyEvent)), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId in a different eventsToCopy event has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        val result = useCase(click, eventsToCopy = listOf(copyEvent))
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `click on detected condition with conditionId not found in repository has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId not found but name in repository has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    // endregion

    // region Notification

    @Test
    fun `notification with no counter references has no missing references`() = runTest {
        assertActionItem(useCase(notification("Hello world")), expectedMissingCount = 0)
    }

    @Test
    fun `notification with reachable counter reference has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        assertActionItem(useCase(notification("Value: {$COUNTER_NAME}")), expectedMissingCount = 0)
    }

    @Test
    fun `notification with missing counter reference has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val result = useCase(notification("Value: {$COUNTER_NAME}"))
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    // endregion

    // region SetText

    @Test
    fun `set text with no counter references has no missing references`() = runTest {
        assertActionItem(useCase(setText("Hello world")), expectedMissingCount = 0)
    }

    @Test
    fun `set text with reachable counter reference has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        assertActionItem(useCase(setText("Value: {$COUNTER_NAME}")), expectedMissingCount = 0)
    }

    @Test
    fun `set text with missing counter reference has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val result = useCase(setText("Value: {$COUNTER_NAME}"))
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    // endregion

    // region ToggleEvent

    @Test
    fun `toggle all event has no missing references`() = runTest {
        val action = mockk<ToggleEvent>(relaxed = true) {
            every { toggleAll } returns true
        }
        assertActionItem(useCase(action), expectedMissingCount = 0)
    }

    @Test
    fun `toggle event with all targets in edited events has no missing references`() = runTest {
        val event = mockk<Event>(relaxed = true) { every { id } returns EVENT_ID }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        val action = toggleEvent(listOf(eventToggle(targetEventId = EVENT_ID)))
        assertActionItem(useCase(action), expectedMissingCount = 0)
    }

    @Test
    fun `toggle event with target in eventsToCopy has no missing references`() = runTest {
        val copyEvent = mockk<Event>(relaxed = true) { every { id } returns OTHER_EVENT_ID }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val action = toggleEvent(listOf(eventToggle(targetEventId = OTHER_EVENT_ID)))
        assertActionItem(useCase(action, eventsToCopy = listOf(copyEvent)), expectedMissingCount = 0)
    }

    @Test
    fun `toggle event with target not found in repository has one event toggle reference with unknown name`() = runTest {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getEventName(OTHER_EVENT_ID) } returns null

        val action = toggleEvent(listOf(eventToggle(targetEventId = OTHER_EVENT_ID)))
        val result = useCase(action)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.EventToggleReference("Unknown"), result.missingReferences[0])
    }

    @Test
    fun `toggle event with target not found but name in repository has one event toggle reference`() = runTest {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getEventName(OTHER_EVENT_ID) } returns EVENT_NAME

        val action = toggleEvent(listOf(eventToggle(targetEventId = OTHER_EVENT_ID)))
        val result = useCase(action)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.EventToggleReference(EVENT_NAME), result.missingReferences[0])
    }

    @Test
    fun `toggle event with multiple missing targets only reports one event toggle reference`() = runTest {
        val anotherEventId = Identifier(databaseId = 12L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getEventName(OTHER_EVENT_ID) } returns EVENT_NAME
        coEvery { mockSmartRepository.getEventName(anotherEventId) } returns "anotherEvent"

        val action = toggleEvent(listOf(
            eventToggle(targetEventId = OTHER_EVENT_ID),
            eventToggle(targetEventId = anotherEventId),
        ))
        assertActionItem(useCase(action), expectedMissingCount = 1)
    }

    @Test
    fun `toggle event with null targetEventId has no missing references`() = runTest {
        val action = toggleEvent(listOf(eventToggle(targetEventId = null)))
        assertActionItem(useCase(action), expectedMissingCount = 0)
    }

    // endregion

    private fun assertActionItem(result: ItemWithMissingReferences.ActionItem, expectedMissingCount: Int) {
        assertEquals(expectedMissingCount, result.missingReferences.size)
    }

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

    private fun eventToggle(targetEventId: Identifier?) = EventToggle(
        id = Identifier(databaseId = 1L),
        actionId = ACTION_ID,
        targetEventId = targetEventId,
        toggleType = ToggleEvent.ToggleType.ENABLE,
    )

    private fun toggleEvent(toggles: List<EventToggle>) = ToggleEvent(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        toggleAll = false,
        eventToggles = toggles,
    )

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val OTHER_EVENT_ID = Identifier(databaseId = 11L)
        const val COUNTER_NAME = "myCounter"
        const val COUNTER_VALUE_NAME = "otherCounter"
        const val CONDITION_NAME = "myCondition"
        const val EVENT_NAME = "myEvent"
    }
}
