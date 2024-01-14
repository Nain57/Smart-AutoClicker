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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.EVENT_TOGGLE_TABLE

import kotlinx.serialization.Serializable


/**
 * Entity defining an event toggle.
 *
 * An intent extra is linked with an action of type [ActionType.TOGGLE_EVENT] and contains the toggle to apply to an event.
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param actionId unique identifier the action containing this toggle. Reference the key [ActionEntity.id] in action_table.
 * @param type the type of manipulation to apply to the event.
 * @param eventId unique identifier of the event to apply the toggle to. Reference the key [EventEntity.id] in event_table.
 */
@Entity(
    tableName = EVENT_TOGGLE_TABLE,
    indices = [Index("action_id"), Index("toggle_event_id")],
    foreignKeys = [
        ForeignKey(
            entity = ActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["action_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["toggle_event_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
@Serializable
data class EventToggleEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long,
    @ColumnInfo(name = "action_id") var actionId: Long,
    @ColumnInfo(name = "toggle_type") val type: EventToggleType,
    @ColumnInfo(name = "toggle_event_id") var toggleEventId: Long,
) : EntityWithId

/**
 * The type of manipulation to apply to an event with a [EventToggleEntity].
 * /!\ DO NOT RENAME: EventToggleType enum name is used in the database.
 */
enum class EventToggleType {
    /** Enable the event. Has no effect if the event is already enabled. */
    ENABLE,
    /** Disable the event. Has no effect if the event is already disabled. */
    DISABLE,
    /** Enable the event if it is disabled, disable it if it is enabled. */
    TOGGLE,
}