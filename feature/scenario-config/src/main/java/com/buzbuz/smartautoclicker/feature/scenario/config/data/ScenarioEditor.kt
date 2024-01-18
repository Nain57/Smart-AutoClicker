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

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.EventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.ImageEventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.TriggerEventsEditor
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

    val imageEventsEditor = ImageEventsEditor(::deleteAllReferencesToEvent, editedScenario)
    val triggerEventsEditor = TriggerEventsEditor(::deleteAllReferencesToEvent, editedScenario)

    fun startEdition(scenario: Scenario, imageEvents: List<ImageEvent>, triggerEvents: List<TriggerEvent>) {
        referenceScenario.value = scenario
        _editedScenario.value = scenario

        imageEventsEditor.startEdition(imageEvents)
        triggerEventsEditor.startEdition(triggerEvents)
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

    private fun deleteAllReferencesToEvent(event: Event) {
        imageEventsEditor.deleteAllEventToggleReferencing(event)
        triggerEventsEditor.deleteAllEventToggleReferencing(event)
    }
}