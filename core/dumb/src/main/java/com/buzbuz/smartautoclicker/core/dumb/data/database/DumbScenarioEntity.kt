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
package com.buzbuz.smartautoclicker.core.dumb.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

/**
 * Entity defining a dumb scenario.
 *
 * A scenario has a relation "one to many" with [DumbActionEntity], which is represented
 * by [DumbScenarioWithActions].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 * @param repeatCount
 * @param isRepeatInfinite
 * @param maxDurationMin
 * @param isDurationInfinite
 */
@Entity(tableName = "dumb_scenario_table")
@Serializable
data class DumbScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int,
    @ColumnInfo(name = "is_repeat_infinite") val isRepeatInfinite: Boolean,
    @ColumnInfo(name = "max_duration_minutes") val maxDurationMin: Int,
    @ColumnInfo(name = "is_duration_infinite") val isDurationInfinite: Boolean,
    @ColumnInfo(name = "randomize") val randomize: Boolean,
)

/**
 * Entity embedding a dumb scenario and its dumb actions.
 *
 * Automatically do the junction between dumb_scenario_table and dumb_action_table, and provide
 * this representation of the one to many relation between dumb scenario and dumb action entity.
 *
 * @param scenario the dumb scenario entity.
 * @param dumbActions the list of dumb actions entity for this scenario.
 */
@Serializable
data class DumbScenarioWithActions(
    @Embedded val scenario: DumbScenarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dumb_scenario_id"
    )
    val dumbActions: List<DumbActionEntity>
)