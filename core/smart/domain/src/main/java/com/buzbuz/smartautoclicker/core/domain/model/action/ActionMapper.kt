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

import android.content.ComponentName
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.toDomainIntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.toDomain

internal fun Action.toEntity(): ActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, action is incomplete.")

    return when (this) {
        is Click -> toClickEntity()
        is Swipe -> toSwipeEntity()
        is Pause -> toPauseEntity()
        is Intent -> toIntentEntity()
        is ToggleEvent -> toToggleEventEntity()
        is ChangeCounter -> toChangeCounterEntity()
        is Notification -> toNotificationEntity()
    }
}

private fun Click.toClickEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CLICK,
        pressDuration = pressDuration,
        clickPositionType = positionType.toEntity(),
        x = position?.x,
        y = position?.y,
        clickOnConditionId = clickOnConditionId?.databaseId,
        clickOffsetX = clickOffset?.x,
        clickOffsetY = clickOffset?.y,
    )

private fun Swipe.toSwipeEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.SWIPE,
        swipeDuration = swipeDuration,
        fromX = from?.x,
        fromY = from?.y,
        toX = to?.x,
        toY = to?.y,
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

private fun ChangeCounter.toChangeCounterEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CHANGE_COUNTER,
        counterName = counterName,
        counterOperation = operation.toEntity(),
        counterOperationValue = operationValue,
    )

private fun Notification.toNotificationEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.NOTIFICATION,
        channelImportance = channelImportance,
        notificationTitle = title,
        notificationMessage = message,
    )


/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toDomain(cleanIds: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toDomainClick(cleanIds)
    ActionType.SWIPE -> toDomainSwipe(cleanIds)
    ActionType.PAUSE -> toDomainPause(cleanIds)
    ActionType.INTENT -> toDomainIntent(cleanIds)
    ActionType.TOGGLE_EVENT -> toDomainToggleEvent(cleanIds)
    ActionType.CHANGE_COUNTER -> toDomainChangeCounter(cleanIds)
    ActionType.NOTIFICATION -> toDomainNotification(cleanIds)
}

private fun CompleteActionEntity.toDomainClick(cleanIds: Boolean = false) = Click(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pressDuration = action.pressDuration!!,
    positionType = action.clickPositionType!!.toDomain(),
    position = getPositionIfValid(action.x, action.y),
    clickOnConditionId = action.clickOnConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
    clickOffset =
        if (action.clickOffsetX != null && action.clickOffsetY != null) Point(action.clickOffsetX!!, action.clickOffsetY!!)
        else null
)

private fun CompleteActionEntity.toDomainSwipe(cleanIds: Boolean = false) = Swipe(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    swipeDuration = action.swipeDuration!!,
    from = getPositionIfValid(action.fromX, action.fromY),
    to = getPositionIfValid(action.toX, action.toY),
)

private fun CompleteActionEntity.toDomainPause(cleanIds: Boolean = false) = Pause(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pauseDuration = action.pauseDuration!!,
)

private fun CompleteActionEntity.toDomainIntent(cleanIds: Boolean = false) = Intent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    isAdvanced = action.isAdvanced,
    isBroadcast = action.isBroadcast ?: false,
    intentAction = action.intentAction,
    componentName = action.componentName.toComponentName(),
    flags = action.flags,
    extras = intentExtras.map { it.toDomainIntentExtra(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainToggleEvent(cleanIds: Boolean = false) = ToggleEvent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    toggleAll = action.toggleAll == true,
    toggleAllType = action.toggleAllType?.toDomain(),
    eventToggles = eventsToggle.map { it.toDomain(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainChangeCounter(cleanIds: Boolean = false) = ChangeCounter(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    counterName = action.counterName!!,
    operation = action.counterOperation!!.toDomain(),
    operationValue = action.counterOperationValue!!,
)

private fun CompleteActionEntity.toDomainNotification(cleanIds: Boolean = false) = Notification(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    channelImportance = action.channelImportance!!,
    title = action.notificationTitle!!,
    message = action.notificationMessage,
)

private fun ClickPositionType.toDomain(): Click.PositionType =
    Click.PositionType.valueOf(name)

private fun EventToggleType.toDomain(): ToggleEvent.ToggleType =
    ToggleEvent.ToggleType.valueOf(name)

private fun ChangeCounterOperationType.toDomain(): ChangeCounter.OperationType =
    ChangeCounter.OperationType.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}

private fun getPositionIfValid(x: Int?, y: Int?): Point? =
    if (x != null && y != null) Point(x, y) else null
