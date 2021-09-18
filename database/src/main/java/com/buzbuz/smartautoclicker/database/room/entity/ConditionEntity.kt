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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity defining a condition from an event.
 *
 * A condition is composed of a path and size of a bitmap to be matched on the screen, and the position of this matching
 * on the screen.
 *
 * It has a one to many relation with [EventEntity], meaning that one event can have several conditions. If the event is
 * deleted, this condition will be deleted as well.
 *
 * @param id unique identifier for a condition. Also the primary key in the table.
 * @param eventId identifier of this condition's event. Reference the key [EventEntity.id] in event_table.
 * @param path the path on the application appData directory for the bitmap representing the condition. Also the
 *             primary key for this entity.
 * @param areaLeft the left coordinate of the rectangle defining the matching area.
 * @param areaTop the top coordinate of the rectangle defining the matching area.
 * @param areaRight the right coordinate of the rectangle defining the matching area.
 * @param areaBottom the bottom coordinate of the rectangle defining the matching area.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 */
@Entity(
    tableName = "condition_table",
    indices = [Index("eventId")],
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name= "id") val id: Long,
    @ColumnInfo(name= "eventId") var eventId: Long,
    @ColumnInfo(name= "path") val path: String,
    @ColumnInfo(name = "area_left") val areaLeft: Int,
    @ColumnInfo(name = "area_top") val areaTop: Int,
    @ColumnInfo(name = "area_right") val areaRight: Int,
    @ColumnInfo(name = "area_bottom") val areaBottom: Int,
    @ColumnInfo(name = "threshold", defaultValue = "1") val threshold: Int,
)

