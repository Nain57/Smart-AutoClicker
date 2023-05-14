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
package com.buzbuz.smartautoclicker.domain.model.condition

import android.graphics.Rect

import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity

/** @return the entity equivalent of this condition. */
internal fun Condition.toEntity() = ConditionEntity(
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


/** @return the condition for this entity. */
internal fun ConditionEntity.toCondition() = Condition(
    id,
    eventId,
    name,
    path,
    Rect(areaLeft, areaTop, areaRight, areaBottom),
    threshold,
    detectionType,
    shouldBeDetected,
)