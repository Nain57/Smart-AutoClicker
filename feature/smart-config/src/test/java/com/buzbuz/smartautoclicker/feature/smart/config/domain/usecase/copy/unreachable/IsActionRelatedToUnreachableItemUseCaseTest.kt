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

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
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
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
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
    fun `gesture action is not related to unreachable item`() {
        assertFalse(useCase(mockk<Gesture>(relaxed = true)))
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

    // region Gesture

    @Test
    fun `gesture with literal coordinates is not related to unreachable item`() {
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Literal(100), CoordinatesValue.Literal(200),
        ))))
        assertFalse(useCase(gesture))
    }

    @Test
    fun `gesture with reachable counter coordinate is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
        ))))
        assertFalse(useCase(gesture))
    }

    @Test
    fun `gesture with unreachable counter coordinate is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
        ))))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with null condition position is not related to unreachable item`() {
        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId = null))))
        assertFalse(useCase(gesture))
    }

    @Test
    fun `gesture with conditionId found in event is not related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        assertFalse(useCase(gesture))
    }

    @Test
    fun `gesture with conditionId not found is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with conditionId in eventsToCopy event is not related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        assertFalse(useCase(gesture, eventsToCopy = listOf(copyEvent)))
    }

    @Test
    fun `gesture with unreachable counter in press offset is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_NAME)),
        )))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with unreachable condition in press offset is related to unreachable item`() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        val gesture = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = Position.Condition(Identifier(databaseId = 100L)),
        )))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with unreachable counter in move position is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0))),
            releaseMotion(),
        ))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with unreachable condition in move offset is related to unreachable item`() {
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(
                position = literalPosition(),
                offset = Position.Condition(Identifier(databaseId = 100L)),
            ),
            releaseMotion(),
        ))
        assertTrue(useCase(gesture))
    }

    @Test
    fun `gesture with wait and release motions only checks positioned motions`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val gesture = gesture(finger(
            pressMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0))),
            waitMotion(),
            releaseMotion(),
        ))
        assertFalse(useCase(gesture))
    }

    @Test
    fun `gesture with unreachable counter in second finger is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(
            finger(pressMotion(literalPosition())),
            finger(pressMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)))),
        )
        assertTrue(useCase(gesture))
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
        assertFalse(useCase(click(position = literalPosition())))
    }

    @Test
    fun `click with null position is not related to unreachable item`() {
        assertFalse(useCase(click(position = null)))
    }

    @Test
    fun `click with reachable counter coordinate is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(200),
        ))
        assertFalse(useCase(click))
    }

    @Test
    fun `click with unreachable counter x coordinate is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(200),
        ))
        assertTrue(useCase(click))
    }

    @Test
    fun `click with unreachable counter y coordinate is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Literal(100), CoordinatesValue.Counter(COUNTER_NAME),
        ))
        assertTrue(useCase(click))
    }

    @Test
    fun `click user selected position with reachable counter offset is not related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val click = click(
            position = literalPosition(),
            offset = Position.Coordinates(
                CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
            ),
        )
        assertFalse(useCase(click))
    }

    @Test
    fun `click user selected position with unreachable counter offset is related to unreachable item`() {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val click = click(
            position = literalPosition(),
            offset = Position.Coordinates(
                CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
            ),
        )
        assertTrue(useCase(click))
    }

    @Test
    fun `click on detected condition with null conditionId is not related to unreachable item`() {
        assertFalse(useCase(click(position = Position.Condition(null))))
    }

    @Test
    fun `click on detected condition with reachable counter offset is not related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()

        val click = click(
            position = Position.Condition(conditionId),
            offset = Position.Coordinates(
                CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
            ),
        )
        assertFalse(useCase(click))
    }

    @Test
    fun `click on detected condition with unreachable counter offset is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null

        val click = click(
            position = Position.Condition(conditionId),
            offset = Position.Coordinates(
                CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_NAME),
            ),
        )
        assertTrue(useCase(click))
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

        assertFalse(useCase(click(position = Position.Condition(conditionId))))
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

        assertTrue(useCase(click(position = Position.Condition(conditionId))))
    }

    @Test
    fun `click on detected condition with conditionId not found is related to unreachable item`() {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        assertTrue(useCase(click(position = Position.Condition(conditionId))))
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

        val click = click(position = Position.Condition(conditionId))
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

        val click = click(position = Position.Condition(conditionId))
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

    private fun gesture(vararg fingers: GestureFinger) = Gesture(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        fingers = fingers.toList(),
    )

    private fun finger(vararg motions: GestureFingerMotion) = GestureFinger(
        id = Identifier(databaseId = 50L),
        startTimeMs = 0L,
        motions = motions.toList(),
    )

    private fun literalPosition() = Position.Coordinates(
        CoordinatesValue.Literal(100), CoordinatesValue.Literal(200),
    )

    private fun pressMotion(position: Position, offset: Position = NO_OFFSET) = GestureFingerMotion.Press(
        id = Identifier(databaseId = 51L),
        position = position,
        offset = offset,
    )

    private fun moveMotion(position: Position, offset: Position = NO_OFFSET) = GestureFingerMotion.Move(
        id = Identifier(databaseId = 52L),
        position = position,
        offset = offset,
        durationMs = 100L,
    )

    private fun waitMotion() = GestureFingerMotion.Wait(
        id = Identifier(databaseId = 53L),
        durationMs = 100L,
    )

    private fun releaseMotion() = GestureFingerMotion.Release(
        id = Identifier(databaseId = 54L),
    )

    private fun click(position: Position?, offset: Position? = null) = Click(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        pressDuration = 100L,
        position = position,
        offset = offset,
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

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val OTHER_EVENT_ID = Identifier(databaseId = 11L)
        const val COUNTER_NAME = "myCounter"
        const val COUNTER_VALUE_NAME = "otherCounter"
        val NO_OFFSET = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0))
    }
}
