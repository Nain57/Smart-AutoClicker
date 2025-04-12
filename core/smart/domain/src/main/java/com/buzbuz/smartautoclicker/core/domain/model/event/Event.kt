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
package com.buzbuz.smartautoclicker.core.domain.model.event

import androidx.annotation.CallSuper
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable
import com.buzbuz.smartautoclicker.core.base.interfaces.areComplete
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition

sealed class Event: Identifiable, Completable {

    /** The unique identifier of the scenario for this event. */
    abstract val scenarioId: Identifier
    /** The name of the event. */
    abstract val name: String
    /** The operator to apply between the conditions in the [conditions] list. */
    @ConditionOperator abstract val conditionOperator: Int
    /** Tells if the event should be evaluated with the scenario, or if it should be enabled by an action. */
    abstract val enabledOnStart: Boolean
    /** The list of action to execute when the [conditions] have been fulfilled. */
    abstract val actions: List<Action>
    /** The list of conditions to fulfill to execute the [actions].  */
    abstract val conditions: List<Condition>

    @Suppress("UNCHECKED_CAST")
    fun copyBase(
        id: Identifier = this.id,
        scenarioId: Identifier = this.scenarioId,
        name: String = this.name,
        conditionOperator: Int = this.conditionOperator,
        enabledOnStart: Boolean = this.enabledOnStart,
        actions: List<Action> = this.actions,
        conditions: List<Condition> = this.conditions,
    ): Event =
        when (this) {
            is ScreenEvent -> copy(id = id, scenarioId = scenarioId, name = name, conditionOperator = conditionOperator,
                enabledOnStart = enabledOnStart, actions = actions, conditions = conditions as List<ImageCondition>)
            is TriggerEvent -> copy(id = id, scenarioId = scenarioId, name = name, conditionOperator = conditionOperator,
                enabledOnStart = enabledOnStart, actions = actions, conditions = conditions as List<TriggerCondition>)
        }

    @CallSuper
    override fun isComplete(): Boolean =
        name.isNotEmpty() && actions.isNotEmpty() && actions.areComplete() &&
                conditions.isNotEmpty() && conditions.areComplete()
}


data class ScreenEvent(
    override val id: Identifier,
    override val scenarioId: Identifier,
    override val name: String,
    @ConditionOperator override val conditionOperator: Int,
    override val actions: List<Action> = emptyList(),
    override val conditions: List<ScreenCondition> =  emptyList(),
    override val enabledOnStart: Boolean = true,
    override var priority: Int,
    val keepDetecting: Boolean,
): Event(), Prioritizable {

    /** Tells if this event is complete and valid for save. */
    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false

        actions.forEach { action ->
            if (conditionOperator == AND && action is Click && !action.isClickOnConditionValid()) return false
        }

        return true
    }
}


data class TriggerEvent(
    override val id: Identifier,
    override val scenarioId: Identifier,
    override val name: String,
    @ConditionOperator override val conditionOperator: Int,
    override val actions: List<Action> = emptyList(),
    override val conditions: List<TriggerCondition> =  emptyList(),
    override val enabledOnStart: Boolean = true,
) : Event() {

    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false

        actions.forEach { action ->
            if (!action.isValidForTrigger()) return false
        }

        return true
    }

    private fun Action.isValidForTrigger(): Boolean {
        if (!isComplete()) return false

        return when (this) {
            is Click -> positionType == Click.PositionType.USER_SELECTED
                    && clickOnConditionId == null

            else -> true
        }
    }
}
