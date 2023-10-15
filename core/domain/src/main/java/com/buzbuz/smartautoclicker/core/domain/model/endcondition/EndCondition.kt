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
package com.buzbuz.smartautoclicker.core.domain.model.endcondition

import androidx.annotation.IntRange

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * End condition for a scenario.
 *
 * @param id the unique identifier for the end condition.
 * @param scenarioId the unique identifier of the scenario for this end condition.
 * @param eventId the unique identifier of the event associated with this end condition.
 * @param eventName the name of the associated event.
 * @param executions the number of execution of the associated event before fulfilling this end condition.
 */
data class EndCondition(
    val id: Identifier,
    val scenarioId: Identifier,
    val eventId: Identifier? = null,
    val eventName: String? = null,
    @IntRange(from = 1) val executions: Int = 1,
) {

    /** @return true if this end condition is complete and can be transformed into its entity. */
    fun isComplete(): Boolean =
        eventId != null && eventName != null
}