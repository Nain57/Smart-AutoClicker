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
package com.buzbuz.smartautoclicker.core.domain.model.triggerevent

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.triggercondition.TriggerCondition

data class TriggerEvent(
    override val id: Identifier,
    val scenarioId: Identifier,
    val name: String,
    val enabledOnStart: Boolean,
    @ConditionOperator val conditionOperator: Int,
    val conditions: List<TriggerCondition>,
    val actions: List<Action>,
) : Identifiable, Completable {

    override fun isComplete(): Boolean {
        if (conditions.isEmpty()) return false
        conditions.forEach { condition ->
            if (!condition.isComplete()) return false
        }

        if (actions.isEmpty()) return false
        actions.forEach { action ->
            if (!action.isValidForTrigger()) return false
        }

        return name.isNotEmpty() && conditions.isNotEmpty()
    }

    private fun Action.isValidForTrigger(): Boolean {
        if (!isComplete()) return false

        return when (this) {
            is Action.Click -> positionType == Action.Click.PositionType.USER_SELECTED
                    && clickOnConditionId == null

            else -> true
        }
    }
}