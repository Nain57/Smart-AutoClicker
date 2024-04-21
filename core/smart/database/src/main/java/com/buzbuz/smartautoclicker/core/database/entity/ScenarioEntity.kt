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
package com.buzbuz.smartautoclicker.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE

import kotlinx.serialization.Serializable

/**
 * Entity defining a scenario of events.
 *
 * A scenario has a relation "one to many" with [EventEntity], which is represented by [ScenarioWithEvents].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param randomize if true, the action values such as timers, positions will be shifted by a small random value in
 *                  order to avoid behaving like a bot.
 */
@Entity(tableName = SCENARIO_TABLE)
@Serializable
data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "detection_quality") val detectionQuality: Int,
    @ColumnInfo(name = "randomize", defaultValue="0") val randomize: Boolean = false,
) : EntityWithId

/**
 * Entity embedding a scenario and its events.
 *
 * Automatically do the junction between event_table and scenario_table, and provide this representation of the one to
 * many relation between scenario and events entity.
 *
 * @param scenario the scenario entity.
 * @param events the list of event entity for this scenario.
 */
data class ScenarioWithEvents(
    @Embedded val scenario: ScenarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val events: List<EventEntity>
)

/**
 * Entity embedding all entities for a scenario.
 * This object can be huge and must be used only for backup purpose.
 *
 * @param scenario the scenario entity.
 * @param events the complete event list for this scenario.
 */
@Serializable
data class CompleteScenario(
    @Embedded val scenario: ScenarioEntity,
    @Relation(
        entity = EventEntity::class,
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val events: List<CompleteEventEntity>,
)