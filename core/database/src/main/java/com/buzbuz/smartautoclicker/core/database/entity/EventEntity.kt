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
 * Entity defining an event.
 *
 * An event is a combination of a list of actions (click, swipe, pause) and a list of conditions to fulfill in order to
 * execute those actions. It has a relation "one to many" with [ActionEntity], which is represented in
 * [CompleteEventEntity]. It also as another "one to many" relation with [ConditionEntity], also represented in
 * [CompleteEventEntity].
 *
 * It is always associated to a [ScenarioEntity] via its foreign key [EventEntity.scenarioId]. Deletion of a
 * scenario will lead to the automatic deletion of all events with the same scenarioId.
 *
 * To get the complete event with its actions and conditions, you can use [CompleteEventEntity].
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param scenarioId identifier of this event's scenario. Reference the key [ScenarioEntity.id] in scenario_table.
 * @param name name of this event.
 * @param conditionOperator the operator to apply to all [ConditionEntity] related to this click. Can be any value
 *                          of [com.buzbuz.smartautoclicker.domain.ConditionOperator].
 * @param priority the order in the scenario. Lowest priority will always be checked first when detecting.
 * @param enabledOnStart if true, the event will be evaluated while the scenario is playing. If false, it must be
 *                       enabled via an action TOGGLE_EVENT to be evaluated.
 */
@Entity(
    tableName = "event_table",
    indices = [Index("scenario_id")],
    foreignKeys = [ForeignKey(
        entity = ScenarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["scenario_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class EventEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "scenario_id") val scenarioId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "operator") val conditionOperator: Int,
    @ColumnInfo(name = "priority") var priority: Int,
    @ColumnInfo(name = "enabled_on_start", defaultValue="1") var enabledOnStart: Boolean = true,
)

/**
 * Entity embedding an event, its actions and its conditions.
 *
 * Automatically do the junction between event_table, action_table and condition_table, and provide this
 * representation of the one to many relations between scenario to actions and conditions entities.
 *
 * @param event the event entity.
 * @param actions the list of actions entity for this event.
 * @param conditions the list of conditions entity for this event.
 */
@Serializable
data class CompleteEventEntity(
    @Embedded val event: EventEntity,
    @Relation(
        entity = ActionEntity::class,
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val actions: List<CompleteActionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val conditions: List<ConditionEntity>,
)