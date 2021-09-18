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
import androidx.room.TypeConverter

/**
 * Entity defining an action from an event.
 *
 * An action can have several types, defined by [ActionType] and contained in [ActionEntity.type] as a String. Depending
 * on this type, this entity will have several columns corresponding to that type with their values set, and the other
 * values for all others will have their values set to null.
 *
 * It has a one to many relation with [EventEntity], meaning that one event can have several actions. If the event is
 * deleted, this action will be deleted as well.
 *
 * @param id unique identifier for an action. Also the primary key in the table.
 * @param eventId identifier of this action's event. Reference the key [EventEntity.id] in event_table.
 * @param priority the order in the action list. Lowest priority will always be executed first.
 * @param name the name of this action
 * @param type type of this action. Must be the this representation of the [ActionType] enum.
 *
 * @param x [ActionType.CLICK] only: the x position of the click. Null for others [ActionType].
 * @param y [ActionType.CLICK] only: the y position of the click. Null for others [ActionType].
 * @param pressDuration [ActionType.CLICK] only: the duration of the click press in milliseconds.
 *                      Null for others [ActionType].
 *
 * @param fromX [ActionType.SWIPE] only: the swipe start x coordinates. Null for others [ActionType].
 * @param fromY [ActionType.SWIPE] only: the swipe start y coordinates. Null for others [ActionType].
 * @param toX [ActionType.SWIPE] only: the swipe end x coordinates. Null for others [ActionType].
 * @param toY [ActionType.SWIPE] only: the swipe end y coordinates. Null for others [ActionType].
 * @param swipeDuration [ActionType.SWIPE] only: the delay between the swipe start and end in milliseconds.
 *                      Null for others [ActionType].
 *
 * @param pauseDuration [ActionType.PAUSE] only: the duration of the pause in milliseconds.
 */
@Entity(
    tableName = "action_table",
    indices = [Index("eventId")],
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class ActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "eventId") var eventId: Long,
    @ColumnInfo(name = "priority") var priority: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: ActionType,

    // ActionType.CLICK
    @ColumnInfo(name = "x") val x: Int? = null,
    @ColumnInfo(name = "y") val y: Int? = null,
    @ColumnInfo(name = "pressDuration") val pressDuration: Long? = null,

    // ActionType.SWIPE
    @ColumnInfo(name = "fromX") val fromX: Int? = null,
    @ColumnInfo(name = "fromY") val fromY: Int? = null,
    @ColumnInfo(name = "toX") val toX: Int? = null,
    @ColumnInfo(name = "toY") val toY: Int? = null,
    @ColumnInfo(name = "swipeDuration") val swipeDuration: Long? = null,

    // ActionType.PAUSE
    @ColumnInfo(name = "pauseDuration") val pauseDuration: Long? = null,
)

/**
 * Type of [ActionEntity].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [ActionEntity] documentation for values/type association.
 *
 * /!\ DO NOT RENAME: ActionType enum name is used in the database.
 */
internal enum class ActionType {
    /** A single tap on the screen. */
    CLICK,
    /** A swipe on the screen. */
    SWIPE,
    /** A pause, waiting before the next action. */
    PAUSE,
}

/** Type converter to read/write the [ActionType] into the database. */
internal class ActionTypeStringConverter {
    @TypeConverter
    fun fromString(value: String): ActionType = ActionType.valueOf(value)
    @TypeConverter
    fun toString(date: ActionType): String = date.toString()
}

