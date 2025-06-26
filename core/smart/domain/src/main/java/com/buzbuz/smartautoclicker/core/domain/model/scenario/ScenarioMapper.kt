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
package com.buzbuz.smartautoclicker.core.domain.model.scenario

import com.buzbuz.smartautoclicker.core.base.ScenarioStats
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioStatsEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.toDomain

/** @return the entity equivalent of this scenario. */
internal fun Scenario.toEntity() = ScenarioEntity(
    id = id.databaseId,
    name = name,
    detectionQuality = detectionQuality,
    randomize = randomize,
    keepScreenOn = keepScreenOn,
)

/** @return the scenario for this entity. */
internal fun ScenarioWithEvents.toDomain(asDomain: Boolean = false) = Scenario(
    id = Identifier(id = scenario.id, asTemporary = asDomain),
    name = scenario.name,
    detectionQuality = scenario.detectionQuality,
    randomize = scenario.randomize,
    keepScreenOn = scenario.keepScreenOn,
    eventCount = events.size,
    stats = stats.toDomain(),
)

/** @return the scenario for this entity. */
internal fun CompleteScenario.toDomain(cleanIds: Boolean = false): Pair<Scenario, List<Event>> =
    scenario.toDomain(cleanIds) to events.map { completeEventEntity ->
        completeEventEntity.toDomain(cleanIds)
    }


/** @return the scenario for this entity. */
private fun ScenarioEntity.toDomain(cleanIds: Boolean = false) = Scenario(
    id = Identifier(id = id, asTemporary = cleanIds),
    name = name,
    detectionQuality = detectionQuality,
    randomize = randomize,
    keepScreenOn = keepScreenOn,
)

private fun ScenarioStatsEntity?.toDomain() =
    if (this == null) ScenarioStats(
        lastStartTimestampMs = 0,
        startCount = 0,
    ) else ScenarioStats(
        lastStartTimestampMs = lastStartTimestampMs,
        startCount = startCount,
    )