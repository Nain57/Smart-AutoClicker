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
package com.buzbuz.smartautoclicker.feature.scenario.config.data

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.EventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.ImageEventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.TriggerEventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedListState
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScenarioEditor {

    private val referenceScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)
    private val _editedScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)
    private val _currentEventEditor: MutableStateFlow<EventsEditor<Event, Condition>?> = MutableStateFlow(null)

    val editedScenario: StateFlow<Scenario?> = _editedScenario
    val editedScenarioState: Flow<EditedElementState<Scenario>> = combine(referenceScenario, _editedScenario) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = edit != null && edit.name.isNotEmpty()

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    private val imageEventsEditor = ImageEventsEditor(::deleteAllReferencesToEvent, editedScenario)
    private val triggerEventsEditor = TriggerEventsEditor(::deleteAllReferencesToEvent, editedScenario)

    val currentEventEditor: StateFlow<EventsEditor<Event, Condition>?> = _currentEventEditor

    val allEditedEvents: Flow<List<Event>> =
        combine(imageEventsEditor.allEditedItems, triggerEventsEditor.allEditedItems) { imageEvent, triggerEvents ->
            buildList {
                addAll(imageEvent)
                addAll(triggerEvents)
            }
        }

    val editedEvent: Flow<Event?> = currentEventEditor.flatMapLatest { eventsEditor ->
        eventsEditor?.editedItem ?: emptyFlow()
    }

    val editedImageEventListState: Flow<EditedListState<ImageEvent>> = imageEventsEditor.listState
    val editedImageEventState: Flow<EditedElementState<ImageEvent>> = imageEventsEditor.editedItemState

    val editedTriggerEventListState: Flow<EditedListState<TriggerEvent>> = triggerEventsEditor.listState
    val editedTriggerEventState: Flow<EditedElementState<TriggerEvent>> = triggerEventsEditor.editedItemState

    fun startEdition(scenario: Scenario, imageEvents: List<ImageEvent>, triggerEvents: List<TriggerEvent>) {
        referenceScenario.value = scenario
        _editedScenario.value = scenario

        imageEventsEditor.startEdition(imageEvents)
        triggerEventsEditor.startEdition(triggerEvents)
    }

    @Suppress("UNCHECKED_CAST")
    fun startEventEdition(event: Event) {
        _currentEventEditor.value = when (event) {
            is ImageEvent -> imageEventsEditor
            is TriggerEvent -> triggerEventsEditor
        } as EventsEditor<Event, Condition>

        currentEventEditor.value?.startItemEdition(event)
    }

    fun updateEditedEvent(event: Event) =
        currentEventEditor.value?.updateEditedItem(event)

    fun updateActionsOrder(actions: List<Action>) =
        currentEventEditor.value?.actionsEditor?.updateList(actions)

    fun upsertEditedEvent() =
        currentEventEditor.value?.upsertEditedItem()

    fun deleteEditedEvent() =
        currentEventEditor.value?.deleteEditedItem()

    fun stopEventEdition() {
        currentEventEditor.value?.stopItemEdition()
        _currentEventEditor.value = null
    }

    fun stopEdition() {
        imageEventsEditor.stopEdition()
        triggerEventsEditor.stopEdition()

        referenceScenario.value = null
        _editedScenario.value = null
    }

    fun updateEditedScenario(item: Scenario) {
        _editedScenario.value ?: return
        _editedScenario.value = item
    }

    fun updateImageEventsOrder(newEvents: List<ImageEvent>) {
        imageEventsEditor.updateList(newEvents)
    }

    fun getAllEditedEvents(): List<Event> = buildList {
        imageEventsEditor.editedList.value?.let { addAll(it) }
        triggerEventsEditor.editedList.value?.let { addAll(it) }
    }

    fun getEditedEvent(): Event? =
        currentEventEditor.value?.editedItem?.value

    fun getEditedImageEventsCount(): Int =
        imageEventsEditor.editedList.value?.size ?: 0

    private fun deleteAllReferencesToEvent(event: Event) {
        imageEventsEditor.deleteAllEventToggleReferencing(event)
        triggerEventsEditor.deleteAllEventToggleReferencing(event)
    }
}