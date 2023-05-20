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

import com.buzbuz.smartautoclicker.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.Editor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class ScenarioEditor : Editor<ScenarioEditor.Reference, Scenario>() {

    val eventsEditor: EventsEditor = EventsEditor()
    val endConditionsEditor: EndConditionsEditor = EndConditionsEditor()

    val containsChange: Flow<Boolean> = combine(
        reference,
        editedValue,
        eventsEditor.editedValue,
        endConditionsEditor.editedValue,
    ) { ref, editedScenario, editedEvents, editedEndConditions, ->
        if (ref == null || editedScenario == null || editedEvents == null || editedEndConditions == null) false
        else ref.scenario != editedScenario || ref.events != editedEvents || ref.endConditions != editedEndConditions
    }

    override fun getValueFromReference(reference: Reference): Scenario =
        reference.scenario

    override fun onEditionStarted(reference: Reference) {
        eventsEditor.startEdition(
            EventsEditor.Reference(
                reference.scenario.id,
                reference.events,
            )
        )

        endConditionsEditor.startEdition(
            EndConditionsEditor.Reference(
                reference.scenario.id,
                reference.endConditions,
            )
        )
    }

    fun createNewEvent(context: Context, from: Event?) =
        from?.let { eventsEditor.createNewItemFrom(it) }
            ?: eventsEditor.createNewItem(context)

    fun setEditedEvent(event: Event) {
        eventsEditor.startEventEdition(event)
    }

    fun updateEditedEvent(event: Event) {
        eventsEditor.updateEditedEvent(event)
    }

    fun commitEditedEvent() {
        eventsEditor.commitEventEditions()
    }

    fun deleteEditedEvent() {
        endConditionsEditor.deleteAllItemsReferencing(eventsEditor.eventEditor.getEditedValueOrThrow())
        eventsEditor.deleteEditedEvent()
    }

    fun discardEditedEvent() {
        eventsEditor.discardEventEditions()
    }

    fun updateEventsOrder(newEvents: List<Event>) {
        eventsEditor.updateList(newEvents)
    }

    fun createNewEndCondition(from: EndCondition?) =
        from?.let { endConditionsEditor.createNewItemFrom(it) }
            ?: endConditionsEditor.createNewItem()

    fun upsertEndCondition(endCondition: EndCondition) {
        endConditionsEditor.upsertItem(endCondition)
    }

    fun deleteEndCondition(endCondition: EndCondition) {
        endConditionsEditor.deleteItem(endCondition)
    }

    override fun onEditionFinished(): Reference =
        Reference(
            scenario = getEditedValueOrThrow(),
            events = eventsEditor.finishEdition().events,
            endConditions = endConditionsEditor.finishEdition().endConditions,
        )

    internal data class Reference(
        val scenario: Scenario,
        val events: List<Event>,
        val endConditions: List<EndCondition>,
    )
}

