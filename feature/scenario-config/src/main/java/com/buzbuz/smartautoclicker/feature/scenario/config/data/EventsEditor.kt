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
package com.buzbuz.smartautoclicker.feature.scenario.config.data

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

import kotlinx.coroutines.flow.StateFlow

internal class EventsEditor(
    private val onDeleteEvent: (ImageEvent) -> Unit,
    parentItem: StateFlow<Scenario?>,
): ListEditor<ImageEvent, Scenario>(parentItem = parentItem) {

    val conditionsEditor: ListEditor<ImageCondition, ImageEvent> = ListEditor(
        onListUpdated = ::onEditedEventConditionsUpdated,
        canBeEmpty = false,
        parentItem = editedItem,
    )

    val actionsEditor = ActionsEditor(::onEditedEventActionsUpdated, parentItem = editedItem)

    override fun startItemEdition(item: ImageEvent) {
        super.startItemEdition(item)
        conditionsEditor.startEdition(item.conditions)
        actionsEditor.startEdition(item.actions)
    }

    override fun deleteEditedItem() {
        val editedItem = editedItem.value ?: return
        onDeleteEvent(editedItem)
        super.deleteEditedItem()
    }

    override fun stopItemEdition() {
        actionsEditor.stopEdition()
        conditionsEditor.stopEdition()
        super.stopItemEdition()
    }

    fun deleteAllActionsReferencing(event: ImageEvent) {
        val events = editedList.value ?: return

        val newEvents = events.mapNotNull { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@mapNotNull null // Skip same item

            val newActions = scenarioEvent.actions.toMutableList()
            scenarioEvent.actions.forEach { action ->
                if (action is Action.ToggleEvent && action.toggleEventId == event.id) newActions.remove(action)
            }

            scenarioEvent.copy(actions = newActions)
        }

        updateList(newEvents)
    }

    private fun onEditedEventConditionsUpdated(conditions: List<ImageCondition>) {
        val editedEvent = editedItem.value ?: return

        actionsEditor.editedList.value?.let { actions ->
            val newActions = actions.toMutableList()
            actions.forEach { action ->
                when {
                    // Skip all actions but clicks
                    action !is Action.Click -> return@forEach
                    // Nothing to do on user selected position
                    action.positionType == Action.Click.PositionType.USER_SELECTED -> return@forEach
                    // Condition was referenced and used by an action, delete it
                    editedEvent.conditionOperator == AND && conditions.find { action.clickOnConditionId == it.id } == null ->
                        newActions.remove(action)
                    // Condition was referenced but not used by an action, delete the reference
                    editedEvent.conditionOperator == OR && action.clickOnConditionId != null ->
                        newActions[newActions.indexOf(action)] = action.copy(clickOnConditionId = null)
                }
            }

            actionsEditor.updateList(newActions)
        }

        editedItem.value?.let { event ->
            updateEditedItem(event.copy(conditions = conditions))
        }
    }

    private fun onEditedEventActionsUpdated(actions: List<Action>) {
        editedItem.value?.let { event ->
            updateEditedItem(event.copy(actions = actions))
        }
    }
}