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
package com.buzbuz.smartautoclicker.database.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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
 * @param endConditionOperator the operator to apply to all [EndConditionEntity] related to this scenario. Can be any
 *                             value of [com.buzbuz.smartautoclicker.domain.ConditionOperator].
 */
@Entity(tableName = "scenario_table")
@Serializable
data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "detection_quality") val detectionQuality: Int,
    @ColumnInfo(name = "end_condition_operator") val endConditionOperator: Int,
)

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
 * Entity embedding a scenario and all end condition with their events.
 *
 * Automatically do the junction between scenario_table, end_condition_table and event_table, and provide this
 * representation of the one to many relation between scenario and end conditions.
 *
 * @param scenario the scenario entity
 * @param endConditions the list of end conditions and their events.
 */
data class ScenarioWithEndConditions(
    @Embedded val scenario: ScenarioEntity,
    @Relation(
        entity = EndConditionEntity::class,
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val endConditions: List<EndConditionWithEvent>
)

/**
 * Entity embedding all entities for a scenario.
 * This object can be huge and must be used only for backup purpose.
 *
 * @param scenario the scenario entity.
 * @param events the complete event list for this scenario.
 * @param endConditions the list of end conditions for this scenario.
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
    @Relation(
        entity = EndConditionEntity::class,
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val endConditions: List<EndConditionEntity>
)