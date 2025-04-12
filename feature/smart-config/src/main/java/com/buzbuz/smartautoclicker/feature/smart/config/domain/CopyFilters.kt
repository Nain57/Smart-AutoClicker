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
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import com.buzbuz.smartautoclicker.core.base.interfaces.containsIdentifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.utils.isClickOnCondition


internal fun List<ScreenEvent>.getEditedImageEventsForCopy(): List<ScreenEvent> = filter { event ->
    event.isComplete()
}

internal fun List<ScreenEvent>.filterForImageEventCopy(editedEvents: List<ScreenEvent>): List<ScreenEvent> = filter { event ->
    event.isComplete() && (editedEvents.find { it.id == event.id } == null)
}

internal fun List<TriggerEvent>.getEditedTriggerEventsForCopy(): List<TriggerEvent> = filter { event ->
    event.isComplete()
}

internal fun List<TriggerEvent>.filterForTriggerEventCopy(editedEvents: List<TriggerEvent>): List<TriggerEvent> = filter { event ->
    event.isComplete() && (editedEvents.find { it.id == event.id } == null)
}

internal fun List<Event>.getEditedConditionsForCopy(editedEvent: Event): List<Condition> = buildList {
    editedEvent.conditions.forEach { editedCondition ->
        if (editedCondition.isComplete()) add(editedCondition)
    }

    this@getEditedConditionsForCopy.forEach { event ->
        if (event.id == editedEvent.id || event::class != editedEvent::class || !editedEvent.isComplete()) return@forEach

        event.conditions.forEach { editedCondition ->
            if (editedCondition.isComplete()) add(editedCondition)
        }
    }
}

internal fun List<Condition>.filterConditionsForCopy(editedEvent: Event, editedConditions: List<Condition>): List<Condition> =
    filter { condition ->
        when {
            // Remove invalid actions
            !condition.isComplete() -> false
            // Remove currently edited events, called should use the up to date values from edition
            condition.eventId == editedEvent.id || editedConditions.containsIdentifiable(condition.id) -> false
            // Remove conditions not suitable for current event
            !editedEvent.isConditionCompatibleForCopy(condition) -> false
            // Ok for copy
            else -> true
        }
    }

internal fun List<Event>.getEditedActionsForCopy(editedEvent: Event): List<Action> = buildList {
    editedEvent.actions.forEach { action ->
        if (editedEvent.shouldAddAction(action)) add(action)
    }

    this@getEditedActionsForCopy.forEach { event ->
        if (event.id == editedEvent.id || !event.isComplete()) return@forEach

        event.actions.forEach actions@{ editedAction ->
            if (editedEvent.shouldAddAction(editedAction)) add(editedAction)
        }
    }
}

private fun Event.shouldAddAction(action: Action): Boolean =
    when {
        !action.isComplete() -> false
        this is TriggerEvent && action is Click && action.isClickOnCondition() -> false
        this is ScreenEvent && action is Click && !action.isClickOnConditionValid() -> false
        else -> true
    }

internal fun List<Action>.filterActionsForCopy(editedEvent: Event, editedActions: List<Action>): List<Action> =
    filter { action ->
        when {
            // Remove invalid
            !action.isComplete() -> false
            // Remove currently edited events, called should use the up to date values from edition
            action.eventId == editedEvent.id || editedActions.containsIdentifiable(action.id) -> false
            // Remove click on conditions.
            action is Click && action.positionType == Click.PositionType.ON_DETECTED_CONDITION -> false
            // Remove toggle event that specifies the events toggles.
            action is ToggleEvent && !action.toggleAll -> false
            // Ok for copy
            else -> true
        }
    }

private fun Event.isConditionCompatibleForCopy(condition: Condition): Boolean =
    (this is ScreenEvent && condition is ImageCondition) || (this is TriggerEvent && condition is TriggerCondition)