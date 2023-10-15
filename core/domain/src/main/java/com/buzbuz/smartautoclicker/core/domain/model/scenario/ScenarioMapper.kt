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
package com.buzbuz.smartautoclicker.core.domain.model.scenario

import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/** @return the entity equivalent of this scenario. */
internal fun Scenario.toEntity() = ScenarioEntity(
    id = id.databaseId,
    name = name,
    detectionQuality = detectionQuality,
    endConditionOperator = endConditionOperator,
    randomize = randomize,
)

/** @return the scenario for this entity. */
internal fun ScenarioEntity.toScenario(asDomain: Boolean = false) = Scenario(
    id = Identifier(id = id, asDomain = asDomain),
    name = name,
    detectionQuality = detectionQuality,
    endConditionOperator = endConditionOperator,
    randomize = randomize,
)

/** @return the scenario for this entity. */
internal fun ScenarioWithEvents.toScenario() = Scenario(
    id = Identifier(databaseId = scenario.id),
    name = scenario.name,
    detectionQuality = scenario.detectionQuality,
    endConditionOperator = scenario.endConditionOperator,
    randomize = scenario.randomize,
    eventCount = events.size,
)