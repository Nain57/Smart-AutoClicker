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

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
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
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
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
    fun `gesture action has no missing references`() = runTest {
        val result = useCase(mockk<Gesture>(relaxed = true))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `gesture with literal coordinates has no missing references`() = runTest {
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Literal(100), CoordinatesValue.Literal(200),
        ))))
        assertActionItem(useCase(gesture), expectedMissingCount = 0)
    }

    @Test
    fun `gesture with reachable counter coordinate has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
        ))))
        assertActionItem(useCase(gesture), expectedMissingCount = 0)
    }

    @Test
    fun `gesture with missing counter coordinate has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0),
        ))))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `gesture with null condition position has no missing references`() = runTest {
        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId = null))))
        assertActionItem(useCase(gesture), expectedMissingCount = 0)
    }

    @Test
    fun `gesture with conditionId found in event has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        assertActionItem(useCase(gesture), expectedMissingCount = 0)
    }

    @Test
    fun `gesture with conditionId not found but name in repository has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `gesture with conditionId not found anywhere has one screen condition reference with unknown name`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals("Unknown", ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `gesture with conditionId in another edited event has one screen condition reference named from edition`() = runTest {
        val conditionId = Identifier(tempId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) {
            every { id } returns conditionId
            every { name } returns CONDITION_NAME
        }
        val otherEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        val targetEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns emptyList()
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(targetEvent, otherEvent)
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `gesture with conditionId in its own eventsToCopy event has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()

        val gesture = gesture(finger(pressMotion(Position.Condition(conditionId))))
        assertActionItem(useCase(gesture, eventsToCopy = listOf(copyEvent)), expectedMissingCount = 0)
    }

    @Test
    fun `gesture with missing counter in press offset has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_NAME)),
        )))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `gesture with missing condition in press offset has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val gesture = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = Position.Condition(conditionId),
        )))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `gesture with missing counter in move position has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0))),
            releaseMotion(),
        ))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `gesture with missing condition in move offset has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(position = literalPosition(), offset = Position.Condition(conditionId)),
            releaseMotion(),
        ))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `gesture with both missing counter coordinates has two counter references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val gesture = gesture(finger(pressMotion(Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Counter(COUNTER_VALUE_NAME),
        ))))
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME)))
    }

    @Test
    fun `gesture with missing references across fingers reports all of them`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val gesture = gesture(
            finger(pressMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)))),
            finger(pressMotion(Position.Condition(conditionId))),
        )
        val result = useCase(gesture)
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.ScreenConditionReference(CONDITION_NAME, conditionId)))
    }

    @Test
    fun `gesture with wait and release motions only checks positioned motions`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val gesture = gesture(finger(
            pressMotion(Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0))),
            waitMotion(),
            releaseMotion(),
        ))
        assertActionItem(useCase(gesture), expectedMissingCount = 0)
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

    @Test
    fun `change counter with condition value found in event has no missing references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) { every { id } returns conditionId }
        val event = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(event)
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()

        val result = useCase(changeCounter(COUNTER_NAME, conditionValue(conditionId)))
        assertActionItem(result, expectedMissingCount = 0)
    }

    @Test
    fun `change counter with condition value not found has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val result = useCase(changeCounter(COUNTER_NAME, conditionValue(conditionId)))
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(
            MissingCopyReference.ScreenConditionReference(name = CONDITION_NAME, conditionId = conditionId),
            result.missingReferences[0],
        )
    }

    @Test
    fun `change counter with missing counter name and condition value has two references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val result = useCase(changeCounter(COUNTER_NAME, conditionValue(conditionId)))
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(
            MissingCopyReference.ScreenConditionReference(name = "Unknown", conditionId = conditionId)
        ))
    }

    // endregion

    // region Click

    @Test
    fun `click user selected position has no missing references`() = runTest {
        assertActionItem(useCase(click(position = literalPosition())), expectedMissingCount = 0)
    }

    @Test
    fun `click with reachable counter coordinate has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(200),
        ))
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click with missing counter coordinate has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(200),
        ))

        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `click with null position has no missing references`() = runTest {
        assertActionItem(useCase(click(position = null)), expectedMissingCount = 0)
    }

    @Test
    fun `click with both missing counter coordinates has two counter references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val click = click(position = Position.Coordinates(
            CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Counter(COUNTER_VALUE_NAME),
        ))

        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME)))
    }

    @Test
    fun `click user selected position with reachable counter offset has no missing references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns mockk<Counter>()
        val click = click(
            position = literalPosition(),
            offset = Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)),
        )
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click user selected position with missing counter offset has one counter reference`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        val click = click(
            position = literalPosition(),
            offset = Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)),
        )
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `click with missing counter in both position and offset has two counter references`() = runTest {
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        every { mockEditionState.getCounter(COUNTER_VALUE_NAME) } returns null
        val click = click(
            position = Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)),
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_VALUE_NAME)),
        )
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_VALUE_NAME)))
    }

    @Test
    fun `click on detected condition with reachable counter offset has no missing references`() = runTest {
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
            offset = Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)),
        )
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with missing counter offset has one counter reference`() = runTest {
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
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Counter(COUNTER_NAME)),
        )
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        assertEquals(MissingCopyReference.CounterReference(COUNTER_NAME), result.missingReferences[0])
    }

    @Test
    fun `click on missing condition with missing counter offset has both references`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        every { mockEditionState.getCounter(COUNTER_NAME) } returns null
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = click(
            position = Position.Condition(conditionId),
            offset = Position.Coordinates(CoordinatesValue.Counter(COUNTER_NAME), CoordinatesValue.Literal(0)),
        )
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 2)
        assertTrue(result.missingReferences.contains(MissingCopyReference.ScreenConditionReference(CONDITION_NAME, conditionId)))
        assertTrue(result.missingReferences.contains(MissingCopyReference.CounterReference(COUNTER_NAME)))
    }

    @Test
    fun `click on detected condition with null conditionId has no missing references`() = runTest {
        val click = mockk<Click>(relaxed = true) {
            every { position } returns Position.Condition(null)
            every { offset } returns null
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
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
        }
        assertActionItem(useCase(click), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId found in a different edited event has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) {
            every { id } returns conditionId
            every { name } returns CONDITION_NAME
        }
        val otherEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(otherEvent)
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
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
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
        }
        assertActionItem(useCase(click, eventsToCopy = listOf(copyEvent)), expectedMissingCount = 0)
    }

    @Test
    fun `click on detected condition with conditionId in a different eventsToCopy event has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) {
            every { id } returns conditionId
            every { name } returns CONDITION_NAME
        }
        val copyEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
        }
        val result = useCase(click, eventsToCopy = listOf(copyEvent))
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `click on detected condition with conditionId not found anywhere has one screen condition reference with unknown name`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
        }
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals("Unknown", ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `click on detected condition from another edited event not in database has one screen condition reference`() = runTest {
        val conditionId = Identifier(tempId = 100L)
        val condition = mockk<ScreenCondition>(relaxed = true) {
            every { id } returns conditionId
            every { name } returns CONDITION_NAME
        }
        val otherEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns OTHER_EVENT_ID
            every { conditions } returns listOf(condition)
        }
        val targetEvent = mockk<ScreenEvent>(relaxed = true) {
            every { id } returns EVENT_ID
            every { conditions } returns emptyList()
        }
        every { mockEditionState.getAllEditedEvents() } returns listOf(targetEvent, otherEvent)
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns null

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
        }
        val result = useCase(click)
        assertActionItem(result, expectedMissingCount = 1)
        val ref = result.missingReferences[0] as MissingCopyReference.ScreenConditionReference
        assertEquals(CONDITION_NAME, ref.name)
        assertEquals(conditionId, ref.conditionId)
    }

    @Test
    fun `click on detected condition with conditionId not found but name in repository has one screen condition reference`() = runTest {
        val conditionId = Identifier(databaseId = 100L)
        every { mockEditionState.getAllEditedEvents() } returns emptyList()
        coEvery { mockSmartRepository.getConditionName(conditionId) } returns CONDITION_NAME

        val click = mockk<Click>(relaxed = true) {
            every { eventId } returns EVENT_ID
            every { position } returns Position.Condition(conditionId)
            every { offset } returns null
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

    private fun conditionValue(conditionId: Identifier) = CounterOperationValue.Condition(
        conditionId = conditionId,
        positionType = CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X,
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
        val NO_OFFSET = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0))
    }
}
