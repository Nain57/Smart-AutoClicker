/*
 * Copyright (C) 2023 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

internal fun Action.toEntity(): CompleteActionEntity = when (this) {
    is Action.Click -> toClickEntity()
    is Action.Swipe -> toSwipeEntity()
    is Action.Pause -> toPauseEntity()
    is Action.Intent -> toIntentEntity()
    is Action.ToggleEvent -> toToggleEventEntity()
}

private fun Action.Click.toClickEntity(): CompleteActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

    return CompleteActionEntity(
        action = ActionEntity(
            id = id.databaseId,
            eventId = eventId.databaseId,
            name = name!!,
            type = ActionType.CLICK,
            pressDuration = pressDuration,
            clickPositionType = positionType.toEntity(),
            x = x,
            y = y,
            clickOnConditionId = clickOnConditionId?.databaseId,
        ),
        intentExtras = emptyList(),
    )
}

private fun Action.Swipe.toSwipeEntity(): CompleteActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

    return CompleteActionEntity(
        action = ActionEntity(
            id = id.databaseId,
            eventId = eventId.databaseId,
            name = name!!,
            type = ActionType.SWIPE,
            swipeDuration = swipeDuration,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
        ),
        intentExtras = emptyList(),
    )
}

private fun Action.Pause.toPauseEntity(): CompleteActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")

    return CompleteActionEntity(
        action = ActionEntity(
            id = id.databaseId,
            eventId = eventId.databaseId,
            name = name!!,
            type = ActionType.PAUSE,
            pauseDuration = pauseDuration,
        ),
        intentExtras = emptyList(),
    )
}

private fun Action.Intent.toIntentEntity(): CompleteActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, Intent is incomplete.")

    return CompleteActionEntity(
        action = ActionEntity(
            id = id.databaseId,
            eventId = eventId.databaseId,
            name = name!!,
            type = ActionType.INTENT,
            isAdvanced = isAdvanced,
            isBroadcast = isBroadcast,
            intentAction = intentAction,
            componentName = componentName?.flattenToString(),
            flags = flags,
        ),
        intentExtras = extras?.map { it.toEntity() } ?: emptyList(),
    )
}

private fun Action.ToggleEvent.toToggleEventEntity(): CompleteActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, ToggleEvent is incomplete.")

    return CompleteActionEntity(
        action = ActionEntity(
            id = id.databaseId,
            eventId = eventId.databaseId,
            name = name!!,
            type = ActionType.TOGGLE_EVENT,
            toggleEventId = toggleEventId!!.databaseId,
            toggleEventType = toggleEventType!!.toEntity(),
        ),
        intentExtras = emptyList(),
    )
}

/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toAction(asDomain: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toClick(asDomain)
    ActionType.SWIPE -> toSwipe(asDomain)
    ActionType.PAUSE -> toPause(asDomain)
    ActionType.INTENT -> toIntent(asDomain)
    ActionType.TOGGLE_EVENT -> toToggleEvent(asDomain)
}

private fun CompleteActionEntity.toClick(asDomain: Boolean = false) = Action.Click(
    id = Identifier(id = action.id, asDomain = asDomain),
    eventId = Identifier(id = action.eventId, asDomain = asDomain),
    name = action.name,
    pressDuration = action.pressDuration!!,
    positionType = action.clickPositionType!!.toDomain(),
    x = action.x,
    y = action.y,
    clickOnConditionId = action.clickOnConditionId?.let { Identifier(id = it, asDomain = asDomain) },
)

private fun CompleteActionEntity.toSwipe(asDomain: Boolean = false) = Action.Swipe(
    id = Identifier(id = action.id, asDomain = asDomain),
    eventId = Identifier(id = action.eventId, asDomain = asDomain),
    name = action.name,
    swipeDuration = action.swipeDuration!!,
    fromX = action.fromX!!,
    fromY = action.fromY!!,
    toX = action.toX!!,
    toY = action.toY!!,
)

private fun CompleteActionEntity.toPause(asDomain: Boolean = false) = Action.Pause(
    id = Identifier(id = action.id, asDomain = asDomain),
    eventId = Identifier(id = action.eventId, asDomain = asDomain),
    name = action.name,
    pauseDuration = action.pauseDuration!!,
)

private fun CompleteActionEntity.toIntent(asDomain: Boolean = false) = Action.Intent(
    id = Identifier(id = action.id, asDomain = asDomain),
    eventId = Identifier(id = action.eventId, asDomain = asDomain),
    name = action.name,
    isAdvanced = action.isAdvanced,
    isBroadcast = action.isBroadcast,
    intentAction = action.intentAction,
    componentName = action.componentName.toComponentName(),
    flags = action.flags,
    extras = intentExtras.map { it.toIntentExtra(asDomain) }.toMutableList(),
)

private fun CompleteActionEntity.toToggleEvent(asDomain: Boolean = false) = Action.ToggleEvent(
    id = Identifier(id = action.id, asDomain = asDomain),
    eventId = Identifier(id = action.eventId, asDomain = asDomain),
    name = action.name,
    toggleEventId = Identifier(id = action.toggleEventId!!, asDomain = asDomain),
    toggleEventType = action.toggleEventType!!.toDomain(),
)

private fun ClickPositionType.toDomain(): Action.Click.PositionType =
    Action.Click.PositionType.valueOf(name)

private fun ToggleEventType.toDomain(): Action.ToggleEvent.ToggleType =
    Action.ToggleEvent.ToggleType.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}
