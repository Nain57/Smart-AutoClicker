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

import android.content.Context

import com.buzbuz.smartautoclicker.domain.model.AND
import com.buzbuz.smartautoclicker.domain.model.Identifier
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EventsEditor : ListEditor<EventsEditor.Reference, Event>() {

    val eventEditor: EventEditor = EventEditor()

    val isEventListValid: Flow<Boolean> = editedValue.map { eventList ->
        if (eventList.isNullOrEmpty()) return@map false
        eventList.firstOrNull { !it.isComplete() } == null
    }

    override fun createReferenceFromEdition(): Reference =
        Reference(
            scenarioId = getReferenceOrThrow().scenarioId,
            events = editedValue.value ?: emptyList(),
        )

    override fun getValueFromReference(reference: Reference): List<Event> =
        reference.events

    override fun itemMatcher(first: Event, second: Event): Boolean =
        first.id == second.id

    fun startEventEdition(event: Event) {
        eventEditor.startEdition(EventEditor.Reference(event))
    }

    fun updateEditedEvent(event: Event) {
        eventEditor.updateEditedValue(event)
    }

    fun commitEventEditions() {
        upsertItem(eventEditor.finishEdition().event)
    }

    fun deleteEditedEvent() {
        getEditedValueOrThrow().forEach { event ->
            // Skip the currently edited event
            if (event.id == eventEditor.getEditedValueOrThrow().id) return@forEach

            event.actions.forEach { action ->
                if (action is Action.ToggleEvent && action.toggleEventId == event.id) eventEditor.deleteAction(action)
            }
        }
        deleteItem(eventEditor.finishEdition().event)
    }

    fun discardEventEditions() {
        eventEditor.finishEdition()
    }

    fun createNewItem(context: Context): Event =
        Event(
            id = generateNewIdentifier(),
            scenarioId = getReferenceOrThrow().scenarioId,
            name = context.getString(R.string.default_event_name),
            conditionOperator = AND,
            priority = getEditedListSize(),
            conditions = mutableListOf(),
            actions = mutableListOf(),
        )

    fun createNewItemFrom(item: Event): Event {
        val eventId = generateNewIdentifier()

        return item.copy(
            id = eventId,
            scenarioId = getReferenceOrThrow().scenarioId,
            name = "" + item.name,
            conditions = eventEditor.conditionsEditor.createNewItemsFrom(item.conditions, eventId).toMutableList(),
            actions = eventEditor.actionsEditor.createNewItemsFrom(item.actions, eventId).toMutableList(),
        )
    }

    internal data class Reference(
        val scenarioId: Identifier,
        val events: List<Event>,
    )
}