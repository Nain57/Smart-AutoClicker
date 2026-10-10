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
package com.buzbuz.smartautoclicker.core.database.entity

import android.annotation.SuppressLint
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.GESTURE_FINGER_MOTION_TABLE

import kotlinx.serialization.Serializable

/**
 * Entity defining a motion for a finger within a gesture action.
 *
 * A GestureFingerMotionEntity in contained within one GestureFinger and is part of a list of motions, with the
 * execution order represented by [index].
 *
 * @param id unique identifier of this motion.
 * @param fingerId identifier of the finger that should perform this motion
 * @param index index of the motion within the list of motions for a finger.
 * @param motionType the type of motion this entity represents.
 *
 * @param durationMs the duration of the motion in milliseconds. For [GestureFingerMotionType.WAIT], it is the time
 * spent still with the finger pressed. For [GestureFingerMotionType.MOVE], it is the time to go from start to end.
 * For all others [GestureFingerMotionType], it will be null.
 *
 * @param xCoordinateType the type of coordinates used for the x position of this motion. Null for
 * [GestureFingerMotionType.WAIT] & [GestureFingerMotionType.RELEASE], as they do not have coordinates.
 * @param yCoordinateType the type of coordinates used for the y position of this motion. Null for
 * [GestureFingerMotionType.WAIT] & [GestureFingerMotionType.RELEASE], as they do not have coordinates.
 *
 * @param conditionId the identifier of the condition to execute the motion on. Only defined
 * if [xCoordinateType] & [yCoordinateType] are [CoordinatesType.CONDITION] and if the event condition operator is AND.
 *
 * @param xLiteral the x position of the motion in pixels. Only defined when [xCoordinateType] is
 * [CoordinatesType.LITERAL].
 * @param xCounter the name of the counter containing the value of x in pixels. Only defined when [xCoordinateType] is
 * [CoordinatesType.COUNTER].
 *
 * @param yLiteral the y position of the motion in pixels. Only defined when [yCoordinateType] is
 * [CoordinatesType.LITERAL].
 * @param yCounter the name of the counter containing the value of y in pixels. Only defined when [yCoordinateType] is
 * [CoordinatesType.COUNTER].
 *
 * @param xOffsetCoordinateType the type of coordinates used for the x position of this motion offset. Null for
 * [GestureFingerMotionType.WAIT] & [GestureFingerMotionType.RELEASE], as they do not have coordinates.
 * @param yOffsetCoordinateType the type of coordinates used for the y position of this motion offset. Null for
 * [GestureFingerMotionType.WAIT] & [GestureFingerMotionType.RELEASE], as they do not have coordinates.
 *
 * @param xOffsetLiteral the x position of the motion offset in pixels. Only defined when [xOffsetCoordinateType] is
 * [CoordinatesType.LITERAL].
 * @param xOffsetCounter the name of the counter containing the value of x offset in pixels. Only defined when
 * [xOffsetCoordinateType] is [CoordinatesType.COUNTER].
 *
 * @param yOffsetLiteral the y position of the motion offset in pixels. Only defined when [yOffsetCoordinateType] is
 * [CoordinatesType.LITERAL].
 * @param yOffsetCounter the name of the counter containing the value of y offset in pixels. Only defined when
 * [yOffsetCoordinateType] is [CoordinatesType.COUNTER].
 *
 * @param releaseHoldType how long the finger stays pressed on the screen once the gesture is over. Only defined for
 * [GestureFingerMotionType.RELEASE]; null when the finger is released right away.
 * @param holdDurationMs the time in milliseconds the finger stays pressed once the gesture is over. Only defined when
 * [releaseHoldType] is [GestureReleaseHoldType.FOR_DURATION]. It is not part of the gesture duration.
 */
@SuppressLint("UnsafeOptInUsageError")
@Entity(
    tableName = GESTURE_FINGER_MOTION_TABLE,
    indices = [Index("finger_id")],
    foreignKeys = [
        ForeignKey(
            entity = GestureFingerEntity::class,
            parentColumns = ["id"],
            childColumns = ["finger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
@Serializable
data class GestureFingerMotionEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "finger_id") val fingerId: Long,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "motion_type") val motionType: GestureFingerMotionType,

    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,

    @ColumnInfo(name = "x_coordinate_type") val xCoordinateType: CoordinatesType? = null,
    @ColumnInfo(name = "y_coordinate_type") val yCoordinateType: CoordinatesType? = null,
    @ColumnInfo(name = "condition_id") var conditionId: Long? = null,
    @ColumnInfo(name = "x_literal") val xLiteral: Int? = null,
    @ColumnInfo(name = "x_counter") val xCounter: String? = null,
    @ColumnInfo(name = "y_literal") val yLiteral: Int? = null,
    @ColumnInfo(name = "y_counter") val yCounter: String? = null,

    @ColumnInfo(name = "x_offset_coordinate_type") val xOffsetCoordinateType: CoordinatesType? = null,
    @ColumnInfo(name = "y_offset_coordinate_type") val yOffsetCoordinateType: CoordinatesType? = null,
    @ColumnInfo(name = "x_offset_literal") val xOffsetLiteral: Int? = null,
    @ColumnInfo(name = "x_offset_counter") val xOffsetCounter: String? = null,
    @ColumnInfo(name = "y_offset_literal") val yOffsetLiteral: Int? = null,
    @ColumnInfo(name = "y_offset_counter") val yOffsetCounter: String? = null,

    @ColumnInfo(name = "release_hold_type") val releaseHoldType: GestureReleaseHoldType? = null,
    @ColumnInfo(name = "hold_duration_ms") val holdDurationMs: Long? = null,
) : EntityWithId