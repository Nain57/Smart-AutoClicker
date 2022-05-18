/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioWithEvents

/**
 * Scenario of events.
 *
 * @param id the unique identifier for the scenario. Use 0 for creating a new scenario. Default value is 0.
 * @param name the name of the scenario.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param endConditionOperator the operator to apply to all [EndConditionEntity] related to this scenario. Can be any
 *                             value of [com.buzbuz.smartautoclicker.domain.ConditionOperator].
 * @param eventCount the number of events in this scenario. Default value is 0.
 */
data class Scenario(
    val id: Long = 0,
    var name: String,
    var detectionQuality: Int,
    @ConditionOperator var endConditionOperator: Int,
    val eventCount: Int = 0,
) {
    /** @return the entity equivalent of this scenario. */
    internal fun toEntity() = ScenarioEntity(id, name, detectionQuality, endConditionOperator)
}

/** @return the scenario for this entity. */
internal fun ScenarioEntity.toScenario() = Scenario(
    id = id,
    name = name,
    detectionQuality = detectionQuality,
    endConditionOperator = endConditionOperator,
)

/** @return the scenario for this entity. */
internal fun ScenarioWithEvents.toScenario() = Scenario(
    id = scenario.id,
    name = scenario.name,
    detectionQuality = scenario.detectionQuality,
    endConditionOperator = scenario.endConditionOperator,
    eventCount = events.size
)