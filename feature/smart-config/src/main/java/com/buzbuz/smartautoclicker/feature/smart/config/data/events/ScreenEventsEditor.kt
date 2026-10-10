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

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.StateFlow

internal class ScreenEventsEditor(
    onDeleteEvent: (ScreenEvent) -> Unit,
    parentItem: StateFlow<Scenario?>,
) : EventsEditor<ScreenEvent, ScreenCondition>(onDeleteEvent, canBeEmpty = true, parentItem) {

    override fun onEditedEventConditionsUpdated(conditions: List<ScreenCondition>) {
        val editedEvent = editedItem.value ?: return

        actionsEditor.editedList.value?.let { actions ->
            val newActions = actions.toMutableList()
            actions.forEach { action ->
                when {
                    // Condition used as counter operation value was deleted, delete the action
                    action is ChangeCounter -> {
                        val value = action.operationValue
                        if (value is CounterOperationValue.Condition && conditions.none { it.id == value.conditionId }) {
                            newActions.remove(action)
                        }
                    }

                    // Skip all other actions but clicks
                    action !is Click -> return@forEach

                    // Nothing to do on user selected position
                    action.position !is Position.Condition -> return@forEach

                    // Condition was referenced and used by an action, delete it
                    editedEvent.conditionOperator == AND && conditions.find { action.getClickOnConditionId() == it.id } == null ->
                        newActions.remove(action)

                    // Condition was referenced but not used by an action, delete the reference
                    editedEvent.conditionOperator == OR && action.getClickOnConditionId() != null ->
                        newActions[newActions.indexOf(action)] =
                            action.copy(position = Position.Condition(conditionId = null))
                }
            }

            actionsEditor.updateList(newActions)
        }

        editedItem.value?.let { event ->
            updateEditedItem(copyEventWithNewChildren(event, conditions = conditions))
        }
    }

    override fun copyEventWithNewChildren(
        event: ScreenEvent,
        conditions: List<ScreenCondition>,
        actions: List<Action>,
    ): ScreenEvent = event.copy(conditions = conditions, actions = actions)

}