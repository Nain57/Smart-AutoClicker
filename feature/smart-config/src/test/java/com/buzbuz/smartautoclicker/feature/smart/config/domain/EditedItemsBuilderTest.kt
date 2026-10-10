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
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor

import io.mockk.mockk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EditedItemsBuilderTest {

    private lateinit var builder: EditedItemsBuilder

    @Before
    fun setUp() {
        builder = EditedItemsBuilder(
            displayConfigManager = mockk<DisplayConfigManager>(relaxed = true),
            bitmapRepository = mockk<BitmapRepository>(relaxed = true),
            editor = mockk<ScenarioEditor>(relaxed = true),
        )
    }

    // region Event copy: click on condition

    @Test
    fun `copied click on condition targets the copied condition`() {
        val condition = colorCondition(CONDITION_ID)
        val click = click(position = Position.Condition(CONDITION_ID))
        val event = screenEvent(conditions = listOf(condition), actions = listOf(click))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedConditionId = copy.conditions[0].id
        assertNotEquals(CONDITION_ID, copiedConditionId)
        assertEquals(Position.Condition(copiedConditionId), (copy.actions[0] as Click).position)
    }

    @Test
    fun `copied click on unknown condition keeps its condition id`() {
        val click = click(position = Position.Condition(OTHER_CONDITION_ID))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(click))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        assertEquals(Position.Condition(OTHER_CONDITION_ID), (copy.actions[0] as Click).position)
    }

    @Test
    fun `copied click on coordinates keeps its position`() {
        val click = click(position = literalPosition())
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(click))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        assertEquals(literalPosition(), (copy.actions[0] as Click).position)
    }

    // endregion

    // region Event copy: change counter on condition

    @Test
    fun `copied change counter on condition targets the copied condition`() {
        val changeCounter = changeCounter(conditionValue(CONDITION_ID))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(changeCounter))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedConditionId = copy.conditions[0].id
        assertNotEquals(CONDITION_ID, copiedConditionId)
        assertEquals(conditionValue(copiedConditionId), (copy.actions[0] as ChangeCounter).operationValue)
    }

    @Test
    fun `copied change counter on unknown condition keeps its condition id`() {
        val changeCounter = changeCounter(conditionValue(OTHER_CONDITION_ID))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(changeCounter))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        assertEquals(conditionValue(OTHER_CONDITION_ID), (copy.actions[0] as ChangeCounter).operationValue)
    }

    // endregion

    // region Event copy: gesture on condition

    @Test
    fun `copied gesture press on condition targets the copied condition`() {
        val gesture = gesture(finger(pressMotion(Position.Condition(CONDITION_ID)), releaseMotion()))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedConditionId = copy.conditions[0].id
        val copiedPress = (copy.actions[0] as Gesture).fingers[0].motions[0] as GestureFingerMotion.Press
        assertEquals(Position.Condition(copiedConditionId), copiedPress.position)
    }

    @Test
    fun `copied gesture move on condition targets the copied condition in position and offset`() {
        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(position = Position.Condition(CONDITION_ID), offset = Position.Condition(CONDITION_ID)),
            releaseMotion(),
        ))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedConditionId = copy.conditions[0].id
        val copiedMove = (copy.actions[0] as Gesture).fingers[0].motions[1] as GestureFingerMotion.Move
        assertEquals(Position.Condition(copiedConditionId), copiedMove.position)
        assertEquals(Position.Condition(copiedConditionId), copiedMove.offset)
    }

    @Test
    fun `copied gesture targets the copied condition in every finger`() {
        val gesture = gesture(
            finger(pressMotion(Position.Condition(CONDITION_ID)), releaseMotion()),
            finger(pressMotion(Position.Condition(CONDITION_ID)), releaseMotion()),
        )
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedConditionId = copy.conditions[0].id
        (copy.actions[0] as Gesture).fingers.forEach { finger ->
            assertEquals(Position.Condition(copiedConditionId), (finger.motions[0] as GestureFingerMotion.Press).position)
        }
    }

    @Test
    fun `copied gesture on unknown condition keeps its condition id`() {
        val gesture = gesture(finger(pressMotion(Position.Condition(OTHER_CONDITION_ID)), releaseMotion()))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedPress = (copy.actions[0] as Gesture).fingers[0].motions[0] as GestureFingerMotion.Press
        assertEquals(Position.Condition(OTHER_CONDITION_ID), copiedPress.position)
    }

    @Test
    fun `copied gesture on coordinates keeps its positions`() {
        val counterPosition = Position.Coordinates(
            CoordinatesValue.Counter("counter"), CoordinatesValue.Literal(0),
        )
        val gesture = gesture(finger(
            pressMotion(literalPosition()),
            moveMotion(position = counterPosition, offset = literalPosition()),
            releaseMotion(),
        ))
        val event = screenEvent(conditions = listOf(colorCondition(CONDITION_ID)), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedFinger = (copy.actions[0] as Gesture).fingers[0]
        assertEquals(literalPosition(), (copiedFinger.motions[0] as GestureFingerMotion.Press).position)
        val copiedMove = copiedFinger.motions[1] as GestureFingerMotion.Move
        assertEquals(counterPosition, copiedMove.position)
        assertEquals(literalPosition(), copiedMove.offset)
    }

    @Test
    fun `copied gesture gets new identifiers for fingers and motions`() {
        val gesture = gesture(finger(pressMotion(literalPosition()), releaseMotion()))
        val event = screenEvent(conditions = emptyList(), actions = listOf(gesture))

        val copy = builder.createNewImageEventFrom(event, scenarioId = SCENARIO_ID)

        val copiedGesture = copy.actions[0] as Gesture
        assertNotEquals(gesture.id, copiedGesture.id)
        assertEquals(copy.id, copiedGesture.eventId)
        assertNotEquals(FINGER_ID, copiedGesture.fingers[0].id)
        copiedGesture.fingers[0].motions.forEachIndexed { index, motion ->
            assertNotEquals(gesture.fingers[0].motions[index].id, motion.id)
        }
    }

    // endregion

    private fun screenEvent(conditions: List<ScreenCondition>, actions: List<Action>) = ScreenEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "event",
        conditionOperator = AND,
        conditions = conditions,
        actions = actions,
        priority = 0,
        keepDetecting = false,
        cooldownMs = 0L,
    )

    private fun colorCondition(id: Identifier) = ScreenCondition.Color(
        id = id,
        eventId = EVENT_ID,
        name = "condition",
        threshold = 10,
        shouldBeDetected = true,
        priority = 0,
        color = 0,
        detectionArea = Rect(0, 0, 10, 10),
    )

    private fun changeCounter(operationValue: CounterOperationValue) = ChangeCounter(
        id = ACTION_ID,
        eventId = EVENT_ID,
        name = "counter",
        priority = 0,
        counterName = "counter",
        operation = ChangeCounter.OperationType.SET,
        operationValue = operationValue,
    )

    private fun conditionValue(conditionId: Identifier) = CounterOperationValue.Condition(
        conditionId = conditionId,
        positionType = CounterOperationValue.Condition.PositionType.CONDITION_BOTTOM,
    )

    private fun click(position: Position?) = Click(
        id = ACTION_ID,
        eventId = EVENT_ID,
        name = "click",
        priority = 0,
        pressDuration = 100L,
        position = position,
    )

    private fun gesture(vararg fingers: GestureFinger) = Gesture(
        id = ACTION_ID,
        eventId = EVENT_ID,
        name = "gesture",
        priority = 0,
        fingers = fingers.toList(),
    )

    private fun finger(vararg motions: GestureFingerMotion) = GestureFinger(
        id = FINGER_ID,
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

    private fun releaseMotion() = GestureFingerMotion.Release(
        id = Identifier(databaseId = 54L),
    )

    private companion object {
        val SCENARIO_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val CONDITION_ID = Identifier(databaseId = 100L)
        val OTHER_CONDITION_ID = Identifier(databaseId = 101L)
        val ACTION_ID = Identifier(databaseId = 200L)
        val FINGER_ID = Identifier(databaseId = 50L)
        val NO_OFFSET = Position.Coordinates(CoordinatesValue.Literal(0), CoordinatesValue.Literal(0))
    }
}
