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

import io.mockk.every
import io.mockk.mockk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsActionRelatedToUnreachableItemUseCaseTest {

    private val mockEditionState: IEditionState = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editionState } returns mockEditionState
    }

    private lateinit var useCase: IsActionRelatedToUnreachableItemUseCase

    @Before
    fun setUp() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        useCase = IsActionRelatedToUnreachableItemUseCase(mockEditionRepository)
    }

    // region Actions with no referenced items — always false

    @Test
    fun `pause action is not related to unreachable item`() {
        assertFalse(useCase(mockk<Pause>(relaxed = true)))
    }

    @Test
    fun `swipe action is not related to unreachable item`() {
        assertFalse(useCase(mockk<Swipe>(relaxed = true)))
    }

    @Test
    fun `intent action is not related to unreachable item`() {
        assertFalse(useCase(mockk<Intent>(relaxed = true)))
    }

    @Test
    fun `system action is not related to unreachable item`() {
        assertFalse(useCase(mockk<SystemAction>(relaxed = true)))
    }

    // endregion

    // region ChangeCounter

    @Test
    fun `change counter with reachable counter name and number value is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        assertFalse(useCase(changeCounter(counterName = COUNTER_NAME, operationValue = CounterOperationValue.Number(5.0))))
    }

    @Test
    fun `change counter with unreachable counter name is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        assertTrue(useCase(changeCounter(counterName = COUNTER_NAME, operationValue = CounterOperationValue.Number(5.0))))
    }

    @Test
    fun `change counter with reachable counter name but unreachable counter value is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        assertTrue(useCase(changeCounter(counterName = COUNTER_NAME, operationValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME))))
    }

    @Test
    fun `change counter with reachable counter name and reachable counter value is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns mockk<Counter>()
        assertFalse(useCase(changeCounter(counterName = COUNTER_NAME, operationValue = CounterOperationValue.Counter(COUNTER_VALUE_NAME))))
    }

    // endregion

    // region Click

    @Test
    fun `click user selected position is not related to unreachable item`() {
        val click = mockk<Click>(relaxed = true) {
            every { positionType } returns Click.PositionType.USER_SELECTED
        }
        assertFalse(useCase(click))
    }

    @Test
    fun `click on detected condition with null conditionId is not related to unreachable item`() {
        val click = mockk<Click>(relaxed = true) {
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns null
        }
        assertFalse(useCase(click))
    }

    @Test
    fun `click on detected condition with conditionId found in its own edited event is not related to unreachable item`() {
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
        assertFalse(useCase(click))
    }

    @Test
    fun `click on detected condition with conditionId found in a different edited event is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val otherEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(otherEvent)

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertTrue(useCase(click))
    }

    @Test
    fun `click on detected condition with conditionId not found is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertTrue(useCase(click))
    }

    @Test
    fun `click on detected condition with conditionId in its own eventsToCopy event is not related to unreachable item`() {
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
        assertFalse(useCase(click, eventsToCopy = listOf(copyEvent)))
    }

    @Test
    fun `click on detected condition with conditionId in a different eventsToCopy event is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { positionType } returns Click.PositionType.ON_DETECTED_CONDITION
            every { clickOnConditionId } returns conditionId
        }
        assertTrue(useCase(click, eventsToCopy = listOf(copyEvent)))
    }

    // endregion

    // region Notification

    @Test
    fun `notification with no counter references is not related to unreachable item`() {
        assertFalse(useCase(notification(messageText = "Hello world")))
    }

    @Test
    fun `notification with reachable counter reference is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        assertFalse(useCase(notification(messageText = "Value: {$COUNTER_NAME}")))
    }

    @Test
    fun `notification with unreachable counter reference is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        assertTrue(useCase(notification(messageText = "Value: {$COUNTER_NAME}")))
    }

    // endregion

    // region SetText

    @Test
    fun `set text with no counter references is not related to unreachable item`() {
        assertFalse(useCase(setText(text = "Hello world")))
    }

    @Test
    fun `set text with reachable counter reference is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        assertFalse(useCase(setText(text = "Value: {$COUNTER_NAME}")))
    }

    @Test
    fun `set text with unreachable counter reference is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        assertTrue(useCase(setText(text = "Value: {$COUNTER_NAME}")))
    }

    // endregion

    // region ToggleEvent

    @Test
    fun `toggle all event is not related to unreachable item`() {
        val action = mockk<ToggleEvent>(relaxed = true) {
            every { toggleAll } returns true
        }
        assertFalse(useCase(action))
    }

    @Test
    fun `toggle event with all targets in edited events is not related to unreachable item`() {
        val event = mockk<Event>(relaxed = true) { every { id } returns EVENT_ID }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        val toggle = EventToggle(
            id = Identifier(databaseId = 1L),
            actionId = ACTION_ID,
            targetEventId = EVENT_ID,
            toggleType = ToggleEvent.ToggleType.ENABLE,
        )
        val action = ToggleEvent(
            id = ACTION_ID,
            eventId = EVENT_ID,
            priority = 0,
            toggleAll = false,
            eventToggles = listOf(toggle),
        )
        assertFalse(useCase(action))
    }

    @Test
    fun `toggle event with a target not found in any events is related to unreachable item`() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val toggle = EventToggle(
            id = Identifier(databaseId = 1L),
            actionId = ACTION_ID,
            targetEventId = OTHER_EVENT_ID,
            toggleType = ToggleEvent.ToggleType.ENABLE,
        )
        val action = ToggleEvent(
            id = ACTION_ID,
            eventId = EVENT_ID,
            priority = 0,
            toggleAll = false,
            eventToggles = listOf(toggle),
        )
        assertTrue(useCase(action))
    }

    @Test
    fun `toggle event with target in eventsToCopy is not related to unreachable item`() {
        val copyEvent = mockk<Event>(relaxed = true) { every { id } returns OTHER_EVENT_ID }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val toggle = EventToggle(
            id = Identifier(databaseId = 1L),
            actionId = ACTION_ID,
            targetEventId = OTHER_EVENT_ID,
            toggleType = ToggleEvent.ToggleType.ENABLE,
        )
        val action = ToggleEvent(
            id = ACTION_ID,
            eventId = EVENT_ID,
            priority = 0,
            toggleAll = false,
            eventToggles = listOf(toggle),
        )
        assertFalse(useCase(action, eventsToCopy = listOf(copyEvent)))
    }

    // endregion

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

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val OTHER_EVENT_ID = Identifier(databaseId = 11L)
        const val COUNTER_NAME = "myCounter"
        const val COUNTER_VALUE_NAME = "otherCounter"
    }
}
