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

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType

internal fun Action.toEntity(): ActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, action is incomplete.")

    return when (this) {
        is Action.Click -> toClickEntity()
        is Action.Swipe -> toSwipeEntity()
        is Action.Pause -> toPauseEntity()
        is Action.Intent -> toIntentEntity()
        is Action.ToggleEvent -> toToggleEventEntity()
        is Action.ChangeCounter -> toChangeCounterEntity()
    }
}

private fun Action.Click.toClickEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.CLICK,
        pressDuration = pressDuration,
        clickPositionType = positionType.toEntity(),
        x = x,
        y = y,
        clickOnConditionId = clickOnConditionId?.databaseId,
    )

private fun Action.Swipe.toSwipeEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.SWIPE,
        swipeDuration = swipeDuration,
        fromX = fromX,
        fromY = fromY,
        toX = toX,
        toY = toY,
    )

private fun Action.Pause.toPauseEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.PAUSE,
        pauseDuration = pauseDuration,
    )

private fun Action.Intent.toIntentEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.INTENT,
        isAdvanced = isAdvanced,
        isBroadcast = isBroadcast,
        intentAction = intentAction,
        componentName = componentName?.flattenToString(),
        flags = flags,
    )

private fun Action.ToggleEvent.toToggleEventEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.TOGGLE_EVENT,
        toggleAllType = toggleAllType?.toEntity(),
        toggleAll = toggleAll,
    )

private fun Action.ChangeCounter.toChangeCounterEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name!!,
        type = ActionType.CHANGE_COUNTER,
        counterName = counterName,
        counterOperation = operation.toEntity(),
        counterOperationValue = operationValue,
    )


/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toDomain(cleanIds: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toDomainClick(cleanIds)
    ActionType.SWIPE -> toDomainSwipe(cleanIds)
    ActionType.PAUSE -> toDomainPause(cleanIds)
    ActionType.INTENT -> toDomainIntent(cleanIds)
    ActionType.TOGGLE_EVENT -> toDomainToggleEvent(cleanIds)
    ActionType.CHANGE_COUNTER -> toDomainChangeCounter(cleanIds)
}

private fun CompleteActionEntity.toDomainClick(cleanIds: Boolean = false) = Action.Click(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pressDuration = action.pressDuration!!,
    positionType = action.clickPositionType!!.toDomain(),
    x = action.x,
    y = action.y,
    clickOnConditionId = action.clickOnConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
)

private fun CompleteActionEntity.toDomainSwipe(cleanIds: Boolean = false) = Action.Swipe(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    swipeDuration = action.swipeDuration!!,
    fromX = action.fromX!!,
    fromY = action.fromY!!,
    toX = action.toX!!,
    toY = action.toY!!,
)

private fun CompleteActionEntity.toDomainPause(cleanIds: Boolean = false) = Action.Pause(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pauseDuration = action.pauseDuration!!,
)

private fun CompleteActionEntity.toDomainIntent(cleanIds: Boolean = false) = Action.Intent(
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

private fun CompleteActionEntity.toDomainToggleEvent(cleanIds: Boolean = false) = Action.ToggleEvent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    toggleAll = action.toggleAll == true,
    toggleAllType = action.toggleAllType?.toDomain(),
    eventToggles = eventsToggle.map { it.toDomain(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainChangeCounter(cleanIds: Boolean = false) = Action.ChangeCounter(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    counterName = action.counterName!!,
    operation = action.counterOperation!!.toDomain(),
    operationValue = action.counterOperationValue!!,
)

private fun ClickPositionType.toDomain(): Action.Click.PositionType =
    Action.Click.PositionType.valueOf(name)

private fun EventToggleType.toDomain(): Action.ToggleEvent.ToggleType =
    Action.ToggleEvent.ToggleType.valueOf(name)

private fun ChangeCounterOperationType.toDomain(): Action.ChangeCounter.OperationType =
    Action.ChangeCounter.OperationType.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}
