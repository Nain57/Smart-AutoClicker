/*
 * Copyright (C) 2021 Nain57
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

/**
 * Entity defining a scenario of events.
 *
 * A scenario has a relation "one to many" with [EventEntity], which is represented by [ScenarioWithEvents].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 */
@Entity(tableName = "scenario_table")
internal data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "name") val name: String,
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
internal data class ScenarioWithEvents(
    @Embedded val scenario: ScenarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val events: List<EventEntity>
)