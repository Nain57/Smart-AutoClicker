/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue.Counter
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue.Literal
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toGestureFingerEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toGestureMotionEntities
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionMapperTests {

    private companion object {
        private const val ACTION_DB_ID = 42L
        private const val CLICK_CONDITION_DB_ID = 51L
        private const val CLICK_X_COUNTER_NAME = "counterX"
        private const val CLICK_Y_COUNTER_NAME = "counterY"
        private const val CLICK_OFFSET_X = 10
        private const val CLICK_OFFSET_Y = 20
        /** Press duration used by the domain mapper when the stored click has no wait motion. */
        private const val CLICK_DEFAULT_PRESS_DURATION_MS = 1L

        private const val GESTURE_SECOND_FINGER_DB_ID = 81L

        private const val CHANGE_COUNTER_OTHER_COUNTER_NAME = "otherCounter"
    }

    /* ======================== Incomplete action ======================== */

    @Test(expected = IllegalStateException::class)
    fun incompleteAction_noName_toEntity_throws() {
        ActionTestsData.getNewPause(eventId = ActionTestsData.ACTION_EVENT_ID, name = null).toEntity()
    }

    @Test(expected = IllegalStateException::class)
    fun incompleteClick_noPosition_toEntity_throws() {
        ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID, position = null).toEntity()
    }

    /* ======================== Click ======================== */

    @Test
    fun click_toEntity() {
        assertEquals(
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun click_toDomain() {
        assertEquals(
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun click_toDomain_cleanIds() {
        val expected = ActionTestsData.getNewClick(
            id = ACTION_DB_ID,
            eventId = ActionTestsData.ACTION_EVENT_ID,
            x = null,
            y = null,
        ).copy(
            id = Identifier(id = ACTION_DB_ID, asTemporary = true),
            eventId = Identifier(id = ActionTestsData.ACTION_EVENT_ID, asTemporary = true),
            position = Position.Condition(Identifier(id = CLICK_CONDITION_DB_ID, asTemporary = true)),
        )

        assertEquals(
            expected,
            ActionTestsData.getNewClickEntity(
                id = ACTION_DB_ID,
                eventId = ActionTestsData.ACTION_EVENT_ID,
                x = null,
                y = null,
                clickOnConditionId = CLICK_CONDITION_DB_ID,
            ).toDomain(cleanIds = true),
        )
    }

    @Test
    fun click_toDomain_counterCoordinates() {
        val motions = ActionTestsData.getNewClickMotionEntities().map { motion ->
            if (motion.motionType != GestureFingerMotionType.PRESS) motion
            else motion.copy(
                xCoordinateType = CoordinatesType.COUNTER,
                yCoordinateType = CoordinatesType.COUNTER,
                xLiteral = null,
                yLiteral = null,
                xCounter = CLICK_X_COUNTER_NAME,
                yCounter = CLICK_Y_COUNTER_NAME,
            )
        }

        assertEquals(
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                position = Position.Coordinates(Counter(CLICK_X_COUNTER_NAME), Counter(CLICK_Y_COUNTER_NAME)),
            ),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID, motions = motions).toDomain(),
        )
    }

    @Test
    fun click_toDomain_nullCoordinateTypes_areReadAsLiterals() {
        val motions = ActionTestsData.getNewClickMotionEntities().map { motion ->
            if (motion.motionType != GestureFingerMotionType.PRESS) motion
            else motion.copy(xCoordinateType = null, yCoordinateType = null)
        }

        assertEquals(
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID, motions = motions).toDomain(),
        )
    }

    @Test
    fun click_toDomain_withOffset() {
        assertEquals(
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                offset = Position.Coordinates(Literal(CLICK_OFFSET_X), Literal(CLICK_OFFSET_Y)),
            ),
            ActionTestsData.getNewClickEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                motions = ActionTestsData.getNewClickMotionEntities(offsetX = CLICK_OFFSET_X, offsetY = CLICK_OFFSET_Y),
            ).toDomain(),
        )
    }

    @Test
    fun click_toDomain_noWaitMotion_usesDefaultPressDuration() {
        val motions = ActionTestsData.getNewClickMotionEntities()
            .filter { it.motionType != GestureFingerMotionType.WAIT }

        assertEquals(
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                pressDuration = CLICK_DEFAULT_PRESS_DURATION_MS,
            ),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID, motions = motions).toDomain(),
        )
    }

    @Test
    fun click_toDomain_noFingers() {
        assertEquals(
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                pressDuration = CLICK_DEFAULT_PRESS_DURATION_MS,
                position = null,
            ),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID)
                .copy(gestureFingers = emptyList())
                .toDomain(),
        )
    }

    @Test
    fun click_toGestureFingerEntity() {
        assertEquals(
            GestureFingerEntity(
                id = DATABASE_ID_INSERTION,
                actionId = ACTION_DB_ID,
                index = 0,
                startTimeMs = 0,
            ),
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID)
                .toGestureFingerEntity(actionId = ACTION_DB_ID),
        )
    }

    @Test
    fun click_toGestureMotionEntities() {
        assertEquals(
            ActionTestsData.getNewClickMotionEntities(
                fingerId = ActionTestsData.CLICK_FINGER_ID,
                pressMotionId = DATABASE_ID_INSERTION,
                waitMotionId = DATABASE_ID_INSERTION,
                releaseMotionId = DATABASE_ID_INSERTION,
            ),
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID)
                .toGestureMotionEntities(
                    fingerId = ActionTestsData.CLICK_FINGER_ID,
                    clickOnConditionDbId = null,
                ),
        )
    }

    @Test
    fun click_toGestureMotionEntities_counterCoordinates() {
        val expected = ActionTestsData.getNewClickMotionEntities(
            fingerId = ActionTestsData.CLICK_FINGER_ID,
            pressMotionId = DATABASE_ID_INSERTION,
            waitMotionId = DATABASE_ID_INSERTION,
            releaseMotionId = DATABASE_ID_INSERTION,
        ).map { motion ->
            if (motion.motionType != GestureFingerMotionType.PRESS) motion
            else motion.copy(
                xCoordinateType = CoordinatesType.COUNTER,
                yCoordinateType = CoordinatesType.COUNTER,
                xLiteral = null,
                yLiteral = null,
                xCounter = CLICK_X_COUNTER_NAME,
                yCounter = CLICK_Y_COUNTER_NAME,
            )
        }

        assertEquals(
            expected,
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                position = Position.Coordinates(Counter(CLICK_X_COUNTER_NAME), Counter(CLICK_Y_COUNTER_NAME)),
            ).toGestureMotionEntities(
                fingerId = ActionTestsData.CLICK_FINGER_ID,
                clickOnConditionDbId = null,
            ),
        )
    }

    @Test
    fun click_toGestureMotionEntities_withOffset() {
        assertEquals(
            ActionTestsData.getNewClickMotionEntities(
                fingerId = ActionTestsData.CLICK_FINGER_ID,
                pressMotionId = DATABASE_ID_INSERTION,
                waitMotionId = DATABASE_ID_INSERTION,
                releaseMotionId = DATABASE_ID_INSERTION,
                offsetX = CLICK_OFFSET_X,
                offsetY = CLICK_OFFSET_Y,
            ),
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                offset = Position.Coordinates(Literal(CLICK_OFFSET_X), Literal(CLICK_OFFSET_Y)),
            ).toGestureMotionEntities(
                fingerId = ActionTestsData.CLICK_FINGER_ID,
                clickOnConditionDbId = null,
            ),
        )
    }

    @Test
    fun clickOnCondition_toGestureMotionEntities() {
        assertEquals(
            ActionTestsData.getNewClickMotionEntities(
                fingerId = ActionTestsData.CLICK_FINGER_ID,
                pressMotionId = DATABASE_ID_INSERTION,
                waitMotionId = DATABASE_ID_INSERTION,
                releaseMotionId = DATABASE_ID_INSERTION,
                x = null,
                y = null,
                clickOnConditionId = CLICK_CONDITION_DB_ID,
            ),
            ActionTestsData
                .getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID, x = null, y = null)
                .toGestureMotionEntities(
                    fingerId = ActionTestsData.CLICK_FINGER_ID,
                    clickOnConditionDbId = CLICK_CONDITION_DB_ID,
                ),
        )
    }

    @Test
    fun clickOnCondition_toDomain() {
        assertEquals(
            ActionTestsData.getNewClick(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                x = null,
                y = null,
                clickOnConditionId = CLICK_CONDITION_DB_ID,
            ),
            ActionTestsData.getNewClickEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                x = null,
                y = null,
                clickOnConditionId = CLICK_CONDITION_DB_ID,
            ).toDomain(),
        )
    }

    /* ======================== Gesture ======================== */

    @Test
    fun gesture_toEntity() {
        assertEquals(
            ActionTestsData.getNewGestureEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewGesture(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun gesture_toDomain() {
        assertEquals(
            ActionTestsData.getNewGesture(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewGestureEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun gesture_toDomain_fingersSortedByIndex() {
        val entity = ActionTestsData.getNewGestureEntity(eventId = ActionTestsData.ACTION_EVENT_ID)
        val firstFinger = entity.gestureFingers.first()
        val secondFinger = firstFinger.copy(
            finger = firstFinger.finger.copy(id = GESTURE_SECOND_FINGER_DB_ID, index = 1),
            motions = firstFinger.motions.map { it.copy(fingerId = GESTURE_SECOND_FINGER_DB_ID) },
        )

        val gesture = ActionTestsData.getNewGesture(eventId = ActionTestsData.ACTION_EVENT_ID)
        val expectedFirstFinger = gesture.fingers.first()
        val expectedSecondFinger = expectedFirstFinger.copy(id = GESTURE_SECOND_FINGER_DB_ID.asIdentifier())

        assertEquals(
            gesture.copy(fingers = listOf(expectedFirstFinger, expectedSecondFinger)),
            entity.copy(gestureFingers = listOf(secondFinger, firstFinger)).toDomain(),
        )
    }

    /* ======================== Pause ======================== */

    @Test
    fun pause_toEntity() {
        assertEquals(
            ActionTestsData.getNewPauseEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewPause(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun pause_toDomain() {
        assertEquals(
            ActionTestsData.getNewPause(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewPauseEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun pause_toDomain_cleanIds() {
        val expected = ActionTestsData.getNewPause(
            id = ACTION_DB_ID,
            eventId = ActionTestsData.ACTION_EVENT_ID,
        ).copy(
            id = Identifier(id = ACTION_DB_ID, asTemporary = true),
            eventId = Identifier(id = ActionTestsData.ACTION_EVENT_ID, asTemporary = true),
        )

        assertEquals(
            expected,
            ActionTestsData.getNewPauseEntity(id = ACTION_DB_ID, eventId = ActionTestsData.ACTION_EVENT_ID)
                .toDomain(cleanIds = true),
        )
    }

    /* ======================== Intent ======================== */

    @Test
    fun intent_toEntity() {
        assertEquals(
            ActionTestsData.getNewIntentEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun intent_toEntity_nullComponentName() {
        assertEquals(
            ActionTestsData.getNewIntentEntity(eventId = ActionTestsData.ACTION_EVENT_ID, componentName = null).action,
            ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID, componentName = null).toEntity()
        )
    }

    @Test
    fun intent_toDomain() {
        assertEquals(
            ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewIntentEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun intent_toDomain_nullOptionalFields() {
        assertEquals(
            ActionTestsData.getNewIntent(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                isBroadcast = false,
                componentName = null,
            ),
            ActionTestsData.getNewIntentEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                isBroadcast = null,
                componentName = null,
            ).toDomain(),
        )
    }

    @Test
    fun intent_toDomain_withExtras() {
        assertEquals(
            ActionTestsData.getNewIntent(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                intentExtras = mutableListOf(ActionTestsData.getNewIntentExtra(value = 42)),
            ),
            ActionTestsData.getNewIntentEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                intentExtras = listOf(ActionTestsData.getNewIntentExtraEntity(value = "42")),
            ).toDomain(),
        )
    }

    @Test
    fun intent_toDomain_withExtras_cleanIds() {
        val extra = ActionTestsData.getNewIntentExtra(value = 42)
        val expected = ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID)
            .let { intent ->
                intent.copy(
                    id = Identifier(id = intent.id.databaseId, asTemporary = true),
                    eventId = Identifier(id = ActionTestsData.ACTION_EVENT_ID, asTemporary = true),
                    extras = mutableListOf(
                        extra.copy(
                            id = Identifier(id = extra.id.databaseId, asTemporary = true),
                            actionId = Identifier(id = extra.actionId.databaseId, asTemporary = true),
                        )
                    ),
                )
            }

        assertEquals(
            expected,
            ActionTestsData.getNewIntentEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                intentExtras = listOf(ActionTestsData.getNewIntentExtraEntity(value = "42")),
            ).toDomain(cleanIds = true),
        )
    }

    /* ======================== Toggle Event ======================== */

    @Test
    fun toggleEvent_toEntity() {
        assertEquals(
            ActionTestsData.getNewToggleEventEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewToggleEvent(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun toggleEvent_toEntity_withEventToggles() {
        assertEquals(
            ActionTestsData.getNewToggleEventEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
            ).action,
            ActionTestsData.getNewToggleEvent(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
                eventToggle = mutableListOf(ActionTestsData.getNewEventToggleExtra()),
            ).toEntity()
        )
    }

    @Test
    fun toggleEvent_toDomain() {
        assertEquals(
            ActionTestsData.getNewToggleEvent(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewToggleEventEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun toggleEvent_toDomain_nullToggleAll() {
        assertEquals(
            ActionTestsData.getNewToggleEvent(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
            ),
            ActionTestsData.getNewToggleEventEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = null,
                toggleType = null,
            ).toDomain(),
        )
    }

    @Test
    fun toggleEvent_toDomain_withEventToggles() {
        assertEquals(
            ActionTestsData.getNewToggleEvent(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
                eventToggle = mutableListOf(ActionTestsData.getNewEventToggleExtra()),
            ),
            ActionTestsData.getNewToggleEventEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
                eventToggles = listOf(ActionTestsData.getNewEventToggleEntity()),
            ).toDomain(),
        )
    }

    @Test
    fun toggleEvent_toDomain_withEventToggles_cleanIds() {
        val eventToggle = ActionTestsData.getNewEventToggleExtra()
        val expected = ActionTestsData.getNewToggleEvent(
            eventId = ActionTestsData.ACTION_EVENT_ID,
            toggleAll = false,
            toggleType = null,
        ).let { toggleEvent ->
            toggleEvent.copy(
                id = Identifier(id = toggleEvent.id.databaseId, asTemporary = true),
                eventId = Identifier(id = ActionTestsData.ACTION_EVENT_ID, asTemporary = true),
                eventToggles = mutableListOf(
                    eventToggle.copy(
                        id = Identifier(id = eventToggle.id.databaseId, asTemporary = true),
                        actionId = Identifier(id = eventToggle.actionId.databaseId, asTemporary = true),
                        targetEventId = Identifier(id = eventToggle.targetEventId!!.databaseId, asTemporary = true),
                    )
                ),
            )
        }

        assertEquals(
            expected,
            ActionTestsData.getNewToggleEventEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                toggleAll = false,
                toggleType = null,
                eventToggles = listOf(ActionTestsData.getNewEventToggleEntity()),
            ).toDomain(cleanIds = true),
        )
    }

    /* ======================== Change Counter ======================== */

    @Test
    fun changeCounter_toEntity() {
        assertEquals(
            ActionTestsData.getNewChangeCounterEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewChangeCounter(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun changeCounter_toEntity_counterOperationValue() {
        val operationValue = CounterOperationValue.Counter(CHANGE_COUNTER_OTHER_COUNTER_NAME)

        assertEquals(
            ActionTestsData.getNewChangeCounterEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                operationValue = operationValue,
            ).action,
            ActionTestsData.getNewChangeCounter(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                operationValue = operationValue,
            ).toEntity(),
        )
    }

    @Test
    fun changeCounter_toDomain() {
        assertEquals(
            ActionTestsData.getNewChangeCounter(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewChangeCounterEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun changeCounter_toDomain_counterOperationValue() {
        val operationValue = CounterOperationValue.Counter(CHANGE_COUNTER_OTHER_COUNTER_NAME)

        assertEquals(
            ActionTestsData.getNewChangeCounter(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                operationValue = operationValue,
            ),
            ActionTestsData.getNewChangeCounterEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                operationValue = operationValue,
            ).toDomain(),
        )
    }

    @Test
    fun changeCounter_toDomain_nullOperationValueType_isReadAsNumber() {
        val entity = ActionTestsData.getNewChangeCounterEntity(eventId = ActionTestsData.ACTION_EVENT_ID)

        assertEquals(
            ActionTestsData.getNewChangeCounter(eventId = ActionTestsData.ACTION_EVENT_ID),
            entity.copy(action = entity.action.copy(counterOperationValueType = null)).toDomain(),
        )
    }

    /* ======================== Notification ======================== */

    @Test
    fun notification_toEntity() {
        assertEquals(
            ActionTestsData.getNewNotificationEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewNotification(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun notification_toDomain() {
        assertEquals(
            ActionTestsData.getNewNotification(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewNotificationEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    /* ======================== System Action ======================== */

    @Test
    fun systemAction_toEntity() {
        assertEquals(
            ActionTestsData.getNewSystemActionEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewSystemAction(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun systemAction_toDomain() {
        assertEquals(
            ActionTestsData.getNewSystemAction(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewSystemActionEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    /* ======================== Set Text ======================== */

    @Test
    fun setText_toEntity() {
        assertEquals(
            ActionTestsData.getNewSetTextEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewSetText(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun setText_toDomain() {
        assertEquals(
            ActionTestsData.getNewSetText(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewSetTextEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun setText_toDomain_nullValues() {
        assertEquals(
            ActionTestsData.getNewSetText(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                text = "",
                validateInput = false,
            ),
            ActionTestsData.getNewSetTextEntity(
                eventId = ActionTestsData.ACTION_EVENT_ID,
                text = null,
                validateInput = null,
            ).toDomain(),
        )
    }
}
