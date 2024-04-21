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
package com.buzbuz.smartautoclicker.feature.smart.config.data.events

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.StateFlow

internal class TriggerEventsEditor(
    onDeleteEvent: (TriggerEvent) -> Unit,
    parentItem: StateFlow<Scenario?>,
) : EventsEditor<TriggerEvent, TriggerCondition>(onDeleteEvent, canBeEmpty = true, parentItem) {

    override fun onEditedEventConditionsUpdated(conditions: List<TriggerCondition>) {
        editedItem.value?.let { event ->
            updateEditedItem(copyEventWithNewChildren(event, conditions = conditions))
        }
    }

    override fun copyEventWithNewChildren(
        event: TriggerEvent,
        conditions: List<TriggerCondition>,
        actions: List<Action>
    ): TriggerEvent = event.copy(conditions = conditions, actions = actions)
}