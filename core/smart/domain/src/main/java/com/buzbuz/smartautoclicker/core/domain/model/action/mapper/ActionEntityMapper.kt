/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.action.mapper

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition


internal fun Action.toEntity(): ActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, action is incomplete: $this")

    return when (this) {
        is Click -> toClickEntity()
        is Gesture -> toGestureEntity()
        is Pause -> toPauseEntity()
        is Intent -> toIntentEntity()
        is ToggleEvent -> toToggleEventEntity()
        is ChangeCounter -> toChangeCounterEntity()
        is Notification -> toNotificationEntity()
        is SystemAction -> toSystemActionEntity()
        is SetText -> toSetTextEntity()
        is Gesture -> TODO()
    }
}

private fun Click.toClickEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CLICK,
    )

/**
 * A click is stored the same way a gesture is: one finger pressing on the click position, waiting for the press
 * duration, and releasing.
 */
internal fun Click.toGestureFingerEntity(actionId: Long): GestureFingerEntity =
    GestureFingerEntity(
        id = DATABASE_ID_INSERTION,
        actionId = actionId,
        index = 0,
        startTimeMs = 0,
    )

/**
 * @param fingerId the database identifier of the finger created with [toGestureFingerEntity].
 * @param clickOnConditionDbId the database identifier of the condition to click on, if any.
 */
internal fun Click.toGestureMotionEntities(fingerId: Long, clickOnConditionDbId: Long?): List<GestureFingerMotionEntity> {
    val isOnCondition = position is GesturePosition.Condition
    val coordinates = position as? GesturePosition.Coordinates
    val offsetCoordinates = offset as? GesturePosition.Coordinates

    return listOf(
        GestureFingerMotionEntity(
            id = DATABASE_ID_INSERTION,
            fingerId = fingerId,
            index = 0,
            motionType = GestureFingerMotionType.PRESS,
            xCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else coordinates?.x?.toCoordinatesType(),
            yCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else coordinates?.y?.toCoordinatesType(),
            conditionId = clickOnConditionDbId,
            xLiteral = (coordinates?.x as? GestureCoordinatesValue.Literal)?.value,
            yLiteral = (coordinates?.y as? GestureCoordinatesValue.Literal)?.value,
            xCounter = (coordinates?.x as? GestureCoordinatesValue.Counter)?.counterName,
            yCounter = (coordinates?.y as? GestureCoordinatesValue.Counter)?.counterName,
            xOffsetCoordinateType = offsetCoordinates?.x?.toCoordinatesType(),
            yOffsetCoordinateType = offsetCoordinates?.y?.toCoordinatesType(),
            xOffsetLiteral = (offsetCoordinates?.x as? GestureCoordinatesValue.Literal)?.value,
            yOffsetLiteral = (offsetCoordinates?.y as? GestureCoordinatesValue.Literal)?.value,
            xOffsetCounter = (offsetCoordinates?.x as? GestureCoordinatesValue.Counter)?.counterName,
            yOffsetCounter = (offsetCoordinates?.y as? GestureCoordinatesValue.Counter)?.counterName,
        ),
        GestureFingerMotionEntity(
            id = DATABASE_ID_INSERTION,
            fingerId = fingerId,
            index = 1,
            motionType = GestureFingerMotionType.WAIT,
            durationMs = pressDuration,
        ),
        GestureFingerMotionEntity(
            id = DATABASE_ID_INSERTION,
            fingerId = fingerId,
            index = 2,
            motionType = GestureFingerMotionType.RELEASE,
        ),
    )
}

private fun Gesture.toGestureEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.GESTURE,
    )

private fun Pause.toPauseEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.PAUSE,
        pauseDuration = pauseDuration,
    )

private fun Intent.toIntentEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.INTENT,
        isAdvanced = isAdvanced,
        isBroadcast = isBroadcast,
        intentAction = intentAction,
        componentName = componentName?.flattenToString(),
        flags = flags,
    )

private fun ToggleEvent.toToggleEventEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.TOGGLE_EVENT,
        toggleAllType = toggleAllType?.toEntity(),
        toggleAll = toggleAll,
    )

private fun ChangeCounter.toChangeCounterEntity(): ActionEntity {
    val isNumberValue = operationValue is CounterOperationValue.Number

    return ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CHANGE_COUNTER,
        counterName = counterName,
        counterOperation = operation.toEntity(),
        counterOperationValueType = if (isNumberValue) CounterOperationValueType.NUMBER else CounterOperationValueType.COUNTER,
        counterOperationValue = if (isNumberValue) operationValue.value else null,
        counterOperationCounterName = if (isNumberValue) null else operationValue.value as String,
    )
}

private fun Notification.toNotificationEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.NOTIFICATION,
        notificationImportance = channelImportance,
        notificationMessageText = messageText,
    )

private fun SystemAction.toSystemActionEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.SYSTEM,
        systemActionType = type.toEntity(),
    )

private fun SetText.toSetTextEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.TEXT,
        textValue = text,
        textValidateInput = validateInput,
    )

private fun GestureCoordinatesValue.toCoordinatesType(): CoordinatesType =
    when (this) {
        is GestureCoordinatesValue.Literal -> CoordinatesType.LITERAL
        is GestureCoordinatesValue.Counter -> CoordinatesType.COUNTER
    }
