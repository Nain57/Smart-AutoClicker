/*
 * Copyright (C) 2025 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE

import kotlinx.serialization.Serializable

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
 * @param name the name of the condition.
 * @param path the path on the application appData directory for the bitmap representing the condition. Also the
 *             primary key for this entity.
 * @param areaLeft the left coordinate of the rectangle defining the matching area.
 * @param areaTop the top coordinate of the rectangle defining the matching area.
 * @param areaRight the right coordinate of the rectangle defining the matching area.
 * @param areaBottom the bottom coordinate of the rectangle defining the matching area.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 * @param detectionType the type of detection. Can be any of the values defined in
 *                      [com.buzbuz.smartautoclicker.domain.DetectionType].
 * @param shouldBeDetected true if this condition should be detected to be true, false if it should not be found.
 */
@Entity(
    tableName = CONDITION_TABLE,
    indices = [Index("eventId")],
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name= "id") override var id: Long,
    @ColumnInfo(name = "eventId") var eventId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: ConditionType,
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int,

    // ConditionType.ON_IMAGE_DETECTED && ConditionType.ON_TEXT_DETECTED
    @ColumnInfo(name = "detection_type") val detectionType: Int? = null,
    @ColumnInfo(name = "shouldBeDetected") val shouldBeDetected: Boolean? = null,
    @ColumnInfo(name = "detection_area_left") val detectionAreaLeft: Int? = null,
    @ColumnInfo(name = "detection_area_top") val detectionAreaTop: Int? = null,
    @ColumnInfo(name = "detection_area_right") val detectionAreaRight: Int? = null,
    @ColumnInfo(name = "detection_area_bottom") val detectionAreaBottom: Int? = null,
    @ColumnInfo(name = "threshold") val threshold: Int? = null,

    // ConditionType.ON_IMAGE_DETECTED
    @ColumnInfo(name = "path") var path: String? = null,
    @ColumnInfo(name = "area_left") val areaLeft: Int? = null,
    @ColumnInfo(name = "area_top") val areaTop: Int? = null,
    @ColumnInfo(name = "area_right") val areaRight: Int? = null,
    @ColumnInfo(name = "area_bottom") val areaBottom: Int? = null,

    // ConditionType.ON_TEXT_DETECTED
    @ColumnInfo(name = "text_to_detect") val textToDetect: String? = null,
    @ColumnInfo(name = "text_language") val textLanguage: String? = null,

    // ConditionType.ON_BROADCAST_RECEIVED
    @ColumnInfo(name = "broadcast_action") val broadcastAction: String? = null,

    // ConditionType.ON_COUNTER_REACHED
    @ColumnInfo(name = "counter_name") val counterName: String? = null,
    @ColumnInfo(name = "counter_comparison_operation") val counterComparisonOperation: CounterComparisonOperation? = null,
    @ColumnInfo(name = "counter_operation_value_type") val counterOperationValueType: CounterOperationValueType? = null,
    @ColumnInfo(name = "counter_value") val counterValue: Int? = null,
    @ColumnInfo(name = "counter_value_counter_name") val counterOperationCounterName: String? = null,

    // ConditionType.ON_TIMER_REACHED
    @ColumnInfo(name = "timer_value_ms") val timerValueMs: Long? = null,
    @ColumnInfo(name = "timer_restart_when_reached") val restartWhenReached: Boolean? = null,
) : EntityWithId
