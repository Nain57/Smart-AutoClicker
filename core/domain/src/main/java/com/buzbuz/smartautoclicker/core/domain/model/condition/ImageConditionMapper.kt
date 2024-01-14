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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType

/** @return the entity equivalent of this condition. */
internal fun ImageCondition.toEntity() = ConditionEntity(
    id = id.databaseId,
    eventId = eventId.databaseId,
    name = name,
    type = ConditionType.ON_IMAGE_DETECTED,
    path = path!!,
    areaLeft = area.left,
    areaTop = area.top,
    areaRight = area.right,
    areaBottom = area.bottom,
    threshold = threshold,
    detectionType = detectionType,
    shouldBeDetected = shouldBeDetected,
    detectionAreaLeft = detectionArea?.left,
    detectionAreaTop = detectionArea?.top,
    detectionAreaRight = detectionArea?.right,
    detectionAreaBottom = detectionArea?.bottom,
)

/** @return the condition for this entity. */
internal fun ConditionEntity.toDomainImageCondition(cleanIds: Boolean = false): ImageCondition {
    if (type != ConditionType.ON_IMAGE_DETECTED) throw IllegalArgumentException("Not an image condition")

    return ImageCondition(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        path = path,
        area = Rect(areaLeft!!, areaTop!!, areaRight!!, areaBottom!!),
        threshold = threshold!!,
        detectionType = detectionType!!,
        detectionArea = getDetectionArea(),
        shouldBeDetected = shouldBeDetected ?: true,
    )
}

private fun ConditionEntity.getDetectionArea(): Rect? =
    if (detectionAreaLeft != null && detectionAreaTop != null && detectionAreaRight != null && detectionAreaBottom != null)
        Rect(detectionAreaLeft!!, detectionAreaTop!!, detectionAreaRight!!, detectionAreaBottom!!)
    else
        null
