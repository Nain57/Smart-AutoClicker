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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import com.buzbuz.smartautoclicker.core.base.interfaces.containsIdentifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent


internal fun List<ImageEvent>.filterForImageCopy(): List<ImageEvent> = filter { event ->
    event.isComplete()
}

internal fun List<TriggerEvent>.filterForTriggerCopy(): List<TriggerEvent> = filter { event ->
    event.isComplete()
}

internal fun List<Event>.getEditedConditionsForCopy(editedEvent: Event): List<Condition> = buildList {
    this@getEditedConditionsForCopy.forEach { event ->
        if (event::class != editedEvent::class || !editedEvent.isComplete()) return@forEach
        event.conditions.forEach { editedCondition ->
            if (editedCondition.isComplete() &&
                editedCondition !is TriggerCondition.OnScenarioStart &&
                editedCondition !is TriggerCondition.OnScenarioEnd) {
                add(editedCondition)
            }
        }
    }
}

internal fun List<Condition>.filterForCopy(editedEvent: Event, editedConditions: List<Condition>): List<Condition> = filter { condition ->
    when {
        // Remove invalid actions
        !condition.isComplete() -> false
        // Remove currently edited events, called should use the up to date values from edition
        editedConditions.containsIdentifiable(condition.id) -> false
        // Remove click on conditions.
        condition is TriggerCondition.OnScenarioStart -> false
        // Remove toggle event that specifies the events toggles.
        condition is TriggerCondition.OnScenarioEnd -> false
        // Remove conditions not suitable for current event
        !editedEvent.isConditionCompatibleForCopy(condition) -> false
        // Ok for copy
        else -> true
    }
}

internal fun List<Event>.getEditedActionsForCopy(): List<Action> = buildList {
    this@getEditedActionsForCopy.forEach { editedEvent ->
        if (!editedEvent.isComplete()) return@forEach
        editedEvent.actions.forEach { editedAction ->
            if (editedAction.isComplete()) add(editedAction)
        }
    }
}

internal fun List<Action>.filterForCopy(editedActions: List<Action>): List<Action> = filter { action ->
    when {
        // Remove invalid
        !action.isComplete() -> false
        // Remove currently edited events, called should use the up to date values from edition
        editedActions.containsIdentifiable(action.id) -> false
        // Remove click on conditions.
        action is Action.Click && action.positionType == Action.Click.PositionType.ON_DETECTED_CONDITION -> false
        // Remove toggle event that specifies the events toggles.
        action is Action.ToggleEvent && !action.toggleAll -> false
        // Ok for copy
        else -> true
    }
}

private fun Event.isConditionCompatibleForCopy(condition: Condition): Boolean =
    (this is ImageEvent && condition is ImageCondition) || (this is TriggerEvent && condition is TriggerCondition)