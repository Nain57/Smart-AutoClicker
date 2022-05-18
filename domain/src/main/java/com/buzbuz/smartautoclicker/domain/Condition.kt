/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.domain

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity

/**
 * Condition for a Event.
 *
 * @param id the unique identifier for the condition. Use 0 for creating a new condition. Default value is 0.
 * @param eventId the identifier of the event for this condition.
 * @param name the name of the condition.
 * @param path the path to the bitmap that should be matched for detection.
 * @param area the area of the screen to detect.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 * @param detectionType the type of detection for this condition. Must be one of [DetectionType].
 * @param bitmap the bitmap for the condition. Not set when fetched from the repository.
 */
data class Condition(
    var id: Long = 0,
    var eventId: Long,
    var name: String,
    var path: String? = null,
    var area: Rect,
    var threshold: Int,
    @DetectionType val detectionType: Int,
    val shouldBeDetected: Boolean,
    val bitmap: Bitmap? = null,
) {

    /** @return the entity equivalent of this condition. */
    internal fun toEntity() = ConditionEntity(
        id,
        eventId,
        name,
        path!!,
        area.left,
        area.top,
        area.right,
        area.bottom,
        threshold,
        detectionType,
        shouldBeDetected,
    )

    /** Cleanup all ids contained in this condition. Ideal for copying. */
    internal fun cleanUpIds() {
        id = 0
        eventId = 0
    }

    /** @return creates a deep copy of this condition. */
    fun deepCopy(): Condition = copy(
        path = path,
        area = Rect(area),
    )
}

/** @return the condition for this entity. */
internal fun ConditionEntity.toCondition(): Condition =
    Condition(id, eventId, name, path, Rect(areaLeft, areaTop, areaRight, areaBottom), threshold, detectionType, shouldBeDetected)

/** Defines the detection type to apply to a condition. */
@IntDef(EXACT, WHOLE_SCREEN)
@Retention(AnnotationRetention.SOURCE)
annotation class DetectionType
/** The condition must be detected at the exact same position. */
const val EXACT = 1
/** The condition can be detected anywhere on the screen. */
const val WHOLE_SCREEN = 2
