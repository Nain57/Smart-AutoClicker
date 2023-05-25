/*
 * Copyright (C) 2022 Kevin Buzeau
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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

/**
 * Entity defining a end condition for a scenario.
 *
 * A end condition has a relation "one to many" with [ScenarioEntity], which is represented by [ScenarioWithEndConditions].
 * A end condition has a relation "one to one" with [EventEntity], which is represented by [EndConditionWithEvent].
 *
 * @param id the unique identifier for a end condition.
 * @param scenarioId the unique identifier for the scenario associated with this end condition.
 * @param eventId the unique identifier for the event associated with this end condition.
 * @param executions the number of times the associated event must be executed before fulfilling this end condition.
 */
@Entity(
    tableName = "end_condition_table",
    indices = [Index("scenario_id"), Index("event_id")],
    foreignKeys = [
        ForeignKey(
            entity = ScenarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenario_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
@Serializable
data class EndConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "scenario_id") val scenarioId: Long,
    @ColumnInfo(name = "event_id") var eventId: Long,
    @ColumnInfo(name = "executions") val executions: Int,
)

/**
 * Entity embedding an end condition and its associated event.
 *
 * Automatically do the junction between event_table and end_condition_table, and provide this
 * representation of the one to one relations between end condition to event entities.
 *
 * @param endCondition the end condition entity.
 * @param event the event entity.
 */
data class EndConditionWithEvent(
    @Embedded val endCondition: EndConditionEntity,
    @Relation(
        parentColumn = "event_id",
        entityColumn = "id"
    )
    val event: EventEntity
)
