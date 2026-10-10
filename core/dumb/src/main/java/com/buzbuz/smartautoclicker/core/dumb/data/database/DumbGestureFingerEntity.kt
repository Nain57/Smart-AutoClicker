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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId

import kotlinx.serialization.Serializable

/**
 * Entity defining the behaviour of a finger during a dumb gesture action.
 *
 * Each gesture action can have up to 5 gesture fingers.
 * Each gesture finger is associated with a list of [DumbGestureFingerMotionEntity].
 *
 * @param id unique identifier of the entity.
 * @param dumbActionId the gesture action using this finger.
 * @param index the index of the finger within the finger list of the gesture action. Max is 4.
 * @param startTimeMs the time at which the finger should start its motions, relative to the gesture begin. Should
 * always be 0 when the [index] is 0, as the first finger always starts the gesture.
 */
@Entity(
    tableName = DUMB_GESTURE_FINGER_TABLE,
    indices = [Index("dumb_action_id")],
    foreignKeys = [
        ForeignKey(
            entity = DumbActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["dumb_action_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
@Serializable
data class DumbGestureFingerEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "dumb_action_id") val dumbActionId: Long,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long,
) : EntityWithId

/** A [DumbGestureFingerEntity] with its [DumbGestureFingerMotionEntity]. */
@Serializable
data class DumbGestureFingerWithMotions(
    @Embedded val finger: DumbGestureFingerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "finger_id"
    )
    val motions: List<DumbGestureFingerMotionEntity>,
)
