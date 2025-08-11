
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
)

/** @return the scenario for this entity. */
internal fun ScenarioWithEvents.toDomain(asDomain: Boolean = false) = Scenario(
    id = Identifier(id = scenario.id, asTemporary = asDomain),
    name = scenario.name,
    detectionQuality = scenario.detectionQuality,
    randomize = scenario.randomize,
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
)

private fun ScenarioStatsEntity?.toDomain() =
    if (this == null) ScenarioStats(
        lastStartTimestampMs = 0,
        startCount = 0,
    ) else ScenarioStats(
        lastStartTimestampMs = lastStartTimestampMs,
        startCount = startCount,
    )