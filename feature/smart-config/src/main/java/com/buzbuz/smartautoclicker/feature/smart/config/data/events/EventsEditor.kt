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
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.data.ActionsEditor
import com.buzbuz.smartautoclicker.feature.smart.config.data.base.ListEditor

import kotlinx.coroutines.flow.StateFlow

internal abstract class EventsEditor<Item : Event, ChildCondition : Condition>(
    private val onDeleteEvent: (Item) -> Unit,
    canBeEmpty: Boolean,
    parentItem: StateFlow<Scenario?>,
): ListEditor<Item, Scenario>(canBeEmpty = canBeEmpty, parentItem = parentItem) {

    val conditionsEditor: ListEditor<ChildCondition, Item> = ListEditor(
        onListUpdated = ::onEditedEventConditionsUpdated,
        canBeEmpty = false,
        parentItem = editedItem,
    )

    val actionsEditor = com.buzbuz.smartautoclicker.feature.smart.config.data.ActionsEditor(
        ::onEditedEventActionsUpdated,
        parentItem = editedItem
    )

    @Suppress("UNCHECKED_CAST")
    override fun startItemEdition(item: Item) {
        super.startItemEdition(item)
        conditionsEditor.startEdition(item.conditions as? List<ChildCondition> ?: listOf())
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

    fun deleteAllEventToggleReferencing(event: Event) {
        val events = editedList.value ?: return

        val newEvents = events.mapNotNull { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@mapNotNull null // Skip same item

            val newActions = scenarioEvent.actions.map { action ->
                if (action is Action.ToggleEvent) {
                    action.copy(
                        eventToggles = action.eventToggles.mapNotNull { eventToggle ->
                            if (eventToggle.targetEventId == event.id) null else eventToggle
                        }
                    )
                } else action
            }

            copyEventWithNewChildren(scenarioEvent, actions = newActions)
        }

        updateList(newEvents)
    }

    private fun onEditedEventActionsUpdated(actions: List<Action>) {
        editedItem.value?.let { event ->
            updateEditedItem(copyEventWithNewChildren(event, actions = actions))
        }
    }

    protected abstract fun onEditedEventConditionsUpdated(conditions: List<ChildCondition>)

    @Suppress("UNCHECKED_CAST")
    protected abstract fun copyEventWithNewChildren(
        event: Item,
        conditions: List<ChildCondition> = event.conditions as? List<ChildCondition> ?: listOf(),
        actions: List<Action> = event.actions,
    ): Item
}


