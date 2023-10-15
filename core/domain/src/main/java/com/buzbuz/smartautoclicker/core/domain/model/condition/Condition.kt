/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Condition for a Event.
 *
 * @param id the unique identifier for the condition.
 * @param eventId the identifier of the event for this condition.
 * @param name the name of the condition.
 * @param path the path to the bitmap that should be matched for detection.
 * @param area the area of the screen to detect.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 * @param detectionType the type of detection for this condition. Must be one of [DetectionType].
 * @param bitmap the bitmap for the condition. Not set when fetched from the repository.
 * @param detectionArea the area to detect the condition in if [detectionType] is IN_AREA.
 */
data class Condition(
    val id: Identifier,
    val eventId: Identifier,
    val name: String,
    val path: String? = null,
    val area: Rect,
    val threshold: Int,
    @DetectionType val detectionType: Int,
    val shouldBeDetected: Boolean,
    val bitmap: Bitmap? = null,
    val detectionArea: Rect? = null,
) {

    /** @return creates a deep copy of this condition. */
    fun deepCopy(): Condition = copy(
        path = "" + path,
        area = Rect(area),
    )

    /** Tells if this condition is complete and valid to be saved. */
    fun isComplete(): Boolean =
        name.isNotEmpty()
                && (path != null || bitmap != null)
                && (detectionType == IN_AREA && detectionArea != null || detectionType != IN_AREA)
}
