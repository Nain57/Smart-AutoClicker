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
package com.buzbuz.smartautoclicker.domain.model.endcondition

import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionWithEvent
import com.buzbuz.smartautoclicker.domain.model.Identifier

/** @return the entity equivalent of this end condition. */
internal fun EndCondition.toEntity(): EndConditionEntity {
    val evtId = eventId
    if (!scenarioId.isInDatabase() || evtId == null || !evtId.isInDatabase())
        throw IllegalStateException("Can't create entity, scenario or event is invalid")

    return EndConditionEntity(
        id = id.databaseId,
        scenarioId = scenarioId.databaseId,
        eventId = evtId.databaseId,
        executions = executions,
    )
}

/** @return the end condition for this entity. */
internal fun EndConditionWithEvent.toEndCondition() = EndCondition(
    id = Identifier(databaseId = endCondition.id),
    scenarioId = Identifier(databaseId = endCondition.scenarioId),
    eventId = Identifier(databaseId = event.id),
    eventName = event.name,
    executions = endCondition.executions,
)