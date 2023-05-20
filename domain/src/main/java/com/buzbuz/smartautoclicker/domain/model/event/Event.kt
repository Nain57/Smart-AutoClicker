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
package com.buzbuz.smartautoclicker.domain.model.event

import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.domain.model.Identifier

/**
 * Event of a scenario.
 *
 * @param id the unique identifier for the event.
 * @param scenarioId the identifier of the scenario for this event.
 * @param name the name of the event.
 * @param conditionOperator the operator to apply between the conditions in the [conditions] list.
 * @param priority the execution priority of the event in the scenario.
 * @param actions the list of action to execute when the conditions have been fulfilled
 * @param conditions the list of conditions to fulfill to execute the [actions].
 * @param enabledOnStart tells if the event should be evaluated with the scenario, or if it should be enabled by an action.
 */
data class Event(
    var id: Identifier,
    var scenarioId: Identifier,
    var name: String,
    @ConditionOperator var conditionOperator: Int,
    var priority: Int,
    val actions: MutableList<Action> = mutableListOf(),
    val conditions: MutableList<Condition> =  mutableListOf(),
    var enabledOnStart: Boolean = true,
) {

    /** Tells if this event is complete and valid for save. */
    fun isComplete(): Boolean {
        if (conditions.isEmpty()) return false
        conditions.forEach { condition ->
            if (!condition.isComplete()) return false
        }

        if (actions.isEmpty()) return false
        actions.forEach { action ->
            if (!action.isComplete()) return false
        }

        return true
    }
}
