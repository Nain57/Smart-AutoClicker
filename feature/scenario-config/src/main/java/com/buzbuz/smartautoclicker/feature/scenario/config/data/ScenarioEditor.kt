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

import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedElementState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

internal class ScenarioEditor {

    private val referenceScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)
    private val _editedScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)

    val editedScenario: StateFlow<Scenario?> = _editedScenario
    val editedScenarioState: Flow<EditedElementState<Scenario>> = combine(referenceScenario, _editedScenario) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = edit != null && edit.name.isNotEmpty()

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    val eventsEditor = EventsEditor(::deleteAllReferencesToEvent, editedScenario)
    val endConditionsEditor = object : ListEditor<EndCondition, Scenario>(canBeEmpty = true, parentItem = editedScenario) {
        override fun areItemsTheSame(a: EndCondition, b: EndCondition): Boolean = a.id == b.id
        override fun isItemComplete(item: EndCondition, parent: Scenario?): Boolean = item.isComplete()
    }

    fun startEdition(scenario: Scenario, events: List<Event>, endConditions: List<EndCondition>) {
        referenceScenario.value = scenario
        _editedScenario.value = scenario

        eventsEditor.startEdition(events)
        endConditionsEditor.startEdition(endConditions)
    }

    fun stopEdition() {
        endConditionsEditor.stopEdition()
        eventsEditor.stopEdition()

        referenceScenario.value = null
        _editedScenario.value = null
    }

    fun updateEditedScenario(item: Scenario) {
        _editedScenario.value ?: return
        _editedScenario.value = item
    }

    private fun deleteAllReferencesToEvent(event: Event) {
        eventsEditor.deleteAllActionsReferencing(event)

        endConditionsEditor.editedList.value
            ?.filter { it.eventId != event.id }
            ?.let { endConditionsEditor.updateList(it) }

    }
}