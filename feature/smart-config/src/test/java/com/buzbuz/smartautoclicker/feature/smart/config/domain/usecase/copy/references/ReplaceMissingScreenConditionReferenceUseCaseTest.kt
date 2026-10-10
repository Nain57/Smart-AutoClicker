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
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import android.util.Log

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ReplaceMissingScreenConditionReferenceUseCaseTest {

    private lateinit var useCase: ReplaceMissingScreenConditionReferenceUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        useCase = ReplaceMissingScreenConditionReferenceUseCase()
    }

    @Test
    fun `replace click condition id with replacement condition id`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val click = click(clickOnConditionId = oldConditionId)
        val event = triggerEventWithActions(listOf(click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedClick = result.actions[0] as Click
        assertEquals(replacementConditionId, updatedClick.getClickOnConditionId())
    }

    @Test
    fun `non-click action returns unchanged event`() {
        val pause = mockk<Pause>(relaxed = true)
        val event = triggerEventWithActions(listOf(pause))
        val item = ItemWithMissingReferences.ActionItem(item = pause, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = Identifier(databaseId = 100L))
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `click with non-matching condition id returns unchanged event`() {
        val clickConditionId = Identifier(databaseId = 100L)
        val differentConditionId = Identifier(databaseId = 999L)
        val click = click(clickOnConditionId = clickConditionId)
        val event = triggerEventWithActions(listOf(click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Other Condition", conditionId = differentConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `action not found in event returns unchanged event`() {
        val conditionId = Identifier(databaseId = 100L)
        val click = click(clickOnConditionId = conditionId)
        val otherClick = click(clickOnConditionId = Identifier(databaseId = 999L))
        val event = triggerEventWithActions(listOf(otherClick))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = conditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns Identifier(databaseId = 200L)
        }

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    // region Gesture action replacement

    @Test
    fun `replace condition id in gesture press position`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val action = gesture(finger(pressMotion(Position.Condition(oldConditionId))))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedPosition = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).position as Position.Condition
        assertEquals(replacementConditionId, updatedPosition.conditionId)
    }

    @Test
    fun `replace condition id in gesture move position`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val press = GestureFingerMotion.Press(
            id = Identifier(databaseId = 51L),
            position = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0)),
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0)),
        )
        val move = GestureFingerMotion.Move(
            id = Identifier(databaseId = 52L),
            position = Position.Condition(oldConditionId),
            offset = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0)),
            durationMs = 100L,
        )
        val action = gesture(finger(press, move))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedPosition = (updatedGesture.fingers[0].motions[1] as GestureFingerMotion.Move).position as Position.Condition
        assertEquals(replacementConditionId, updatedPosition.conditionId)
    }

    @Test
    fun `replace condition id in gesture press offset`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val action = gesture(finger(pressMotion(
            position = literalPosition(),
            offset = Position.Condition(oldConditionId),
        )))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedOffset = (updatedGesture.fingers[0].motions[0] as GestureFingerMotion.Press).offset as Position.Condition
        assertEquals(replacementConditionId, updatedOffset.conditionId)
    }

    @Test
    fun `replace condition id in gesture move offset`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val action = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(position = literalPosition(), offset = Position.Condition(oldConditionId)),
            releaseMotion(),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        val updatedOffset = (updatedGesture.fingers[0].motions[1] as GestureFingerMotion.Move).offset as Position.Condition
        assertEquals(replacementConditionId, updatedOffset.conditionId)
    }

    @Test
    fun `replace condition id in every finger of the gesture`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val action = gesture(
            finger(pressMotion(Position.Condition(oldConditionId))),
            finger(pressMotion(Position.Condition(oldConditionId))),
        )
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedGesture = result.actions[0] as Gesture
        updatedGesture.fingers.forEach { finger ->
            val updatedPosition = (finger.motions[0] as GestureFingerMotion.Press).position as Position.Condition
            assertEquals(replacementConditionId, updatedPosition.conditionId)
        }
    }

    @Test
    fun `gesture with non-matching condition id and coordinates is unchanged`() {
        val otherConditionId = Identifier(databaseId = 999L)
        val action = gesture(finger(
            pressMotion(Position.Condition(otherConditionId)),
            moveMotion(Position.Coordinates(CoordinatesValue.Counter("counter"), CoordinatesValue.Literal(0))),
            waitMotion(),
            releaseMotion(),
        ))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = Identifier(databaseId = 100L))
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns Identifier(databaseId = 200L)
        }

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    // endregion

    @Test
    fun `click on user selected position returns unchanged event`() {
        val click = Click(
            id = ACTION_ID,
            eventId = EVENT_ID,
            priority = 0,
            position = literalPosition(),
            pressDuration = 100L,
        )
        val event = triggerEventWithActions(listOf(click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = Identifier(databaseId = 100L))
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `action position in event is preserved after replacement`() {
        val pause = mockk<Pause>(relaxed = true)
        val conditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val click = click(clickOnConditionId = conditionId)
        val event = triggerEventWithActions(listOf(pause, click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = conditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        assertEquals(pause, result.actions[0])
        val updatedClick = result.actions[1] as Click
        assertEquals(replacementConditionId, updatedClick.getClickOnConditionId())
    }

    @Test
    fun `replace condition id in standalone click action`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val click = click(clickOnConditionId = oldConditionId)
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(click, missingRef, replacement) as Click
        assertEquals(replacementConditionId, result.getClickOnConditionId())
    }

    @Test
    fun `standalone non-click action returns null`() {
        val pause = mockk<Pause>(relaxed = true)
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = Identifier(databaseId = 100L))
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        assertNull(useCase(pause, missingRef, replacement))
    }

    private fun triggerEventWithActions(actions: List<com.buzbuz.smartautoclicker.core.domain.model.action.Action>) = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "test event",
        conditionOperator = AND,
        actions = actions,
        conditions = emptyList(),
    )

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

    private fun click(clickOnConditionId: Identifier?) = Click(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        position = Position.Condition(conditionId = clickOnConditionId),
        pressDuration = 100L,
    )

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val SCENARIO_ID = Identifier(databaseId = 20L)
        val NO_OFFSET = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0))
    }
}
