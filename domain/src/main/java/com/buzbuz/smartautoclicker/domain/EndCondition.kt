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

import androidx.annotation.IntRange

import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionWithEvent

/**
 * End condition for a scenario.
 *
 * @param id the unique identifier for the end condition. Use 0 for creating a new end condition. Default value is 0.
 * @param scenarioId the unique identifier of the scenario for this end condition.
 * @param eventId the unique identifier of the event associated with this end condition.
 * @param eventName the name of the associated event.
 * @param executions the number of execution of the associated event before fulfilling this end condition.
 */
data class EndCondition(
    val id: Long = 0,
    val scenarioId: Long,
    val eventId: Long = 0,
    val eventName: String? = null,
    @IntRange(from = 1) var executions: Int = 1,
) {

    /** @return the entity equivalent of this end condition. */
    internal fun toEntity(): EndConditionEntity {
        if (scenarioId == 0L || eventId == 0L)
            throw IllegalStateException("Can't create entity, scenario or event is invalid")

        return EndConditionEntity(id, scenarioId, eventId, executions)
    }
}


/** @return the end condition for this entity. */
internal fun EndConditionWithEvent.toEndCondition() =
    EndCondition(endCondition.id, endCondition.scenarioId, event.id, event.name, endCondition.executions)