/*
 * Copyright (C) 2026 Kevin Buzeau
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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId

import kotlinx.serialization.Serializable

/**
 * Entity defining a motion for a finger within a dumb gesture action.
 *
 * A DumbGestureFingerMotionEntity is contained within one DumbGestureFingerEntity and is part of a list of motions,
 * with the execution order represented by [index].
 *
 * @param id unique identifier of this motion.
 * @param fingerId identifier of the finger that should perform this motion.
 * @param index index of the motion within the list of motions for a finger.
 * @param motionType the type of motion this entity represents.
 *
 * @param durationMs the duration of the motion in milliseconds. For [DumbGestureFingerMotionType.WAIT], it is the
 * time spent still with the finger pressed. For [DumbGestureFingerMotionType.MOVE], it is the time to go from start
 * to end. For all others [DumbGestureFingerMotionType], it will be null.
 *
 * @param x the x position of the motion in pixels. Null for [DumbGestureFingerMotionType.WAIT] &
 * [DumbGestureFingerMotionType.RELEASE], as they do not have coordinates.
 * @param y the y position of the motion in pixels. Null for [DumbGestureFingerMotionType.WAIT] &
 * [DumbGestureFingerMotionType.RELEASE], as they do not have coordinates.
 */
@Entity(
    tableName = DUMB_GESTURE_FINGER_MOTION_TABLE,
    indices = [Index("finger_id")],
    foreignKeys = [
        ForeignKey(
            entity = DumbGestureFingerEntity::class,
            parentColumns = ["id"],
            childColumns = ["finger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
@Serializable
data class DumbGestureFingerMotionEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "finger_id") val fingerId: Long,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "motion_type") val motionType: DumbGestureFingerMotionType,

    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "x") val x: Int? = null,
    @ColumnInfo(name = "y") val y: Int? = null,
) : EntityWithId

/**
 * Type of [DumbGestureFingerMotionEntity].
 *
 * /!\ DO NOT RENAME: DumbGestureFingerMotionType enum name is used in the database.
 */
enum class DumbGestureFingerMotionType {
    /** The finger slides from its current position to a new one. */
    MOVE,
    /** The finger touches the screen. */
    PRESS,
    /** The finger leaves the screen. */
    RELEASE,
    /** The finger stays still on the screen. */
    WAIT,
}

/** Type converter to read/write the [DumbGestureFingerMotionType] into the database. */
internal class DumbGestureFingerMotionTypeStringConverter {
    @TypeConverter
    fun fromString(value: String): DumbGestureFingerMotionType = DumbGestureFingerMotionType.valueOf(value)
    @TypeConverter
    fun toString(motionType: DumbGestureFingerMotionType): String = motionType.toString()
}
