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
package com.buzbuz.smartautoclicker.core.domain.model.event

import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.toAction
import com.buzbuz.smartautoclicker.core.domain.model.condition.toCondition

/** @return the entity equivalent of this event. */
internal fun Event.toEntity() = EventEntity(
    id = id.databaseId,
    scenarioId = scenarioId.databaseId,
    name = name,
    conditionOperator = conditionOperator,
    priority = priority,
    enabledOnStart = enabledOnStart,
)

/** @return the complete event for this entity. */
internal fun CompleteEventEntity.toEvent(asDomain: Boolean = false) = Event(
    id = Identifier(id = event.id, asDomain = asDomain),
    scenarioId = Identifier(id = event.scenarioId, asDomain = asDomain),
    name= event.name,
    conditionOperator = event.conditionOperator,
    priority = event.priority,
    enabledOnStart = event.enabledOnStart,
    actions = actions.sortedBy { it.action.priority }.map { it.toAction(asDomain) }.toMutableList(),
    conditions = conditions.map { it.toCondition(asDomain) }.toMutableList(),
)