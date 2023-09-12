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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.endcondition

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

/**
 * View model for the [EndConditionConfigDialog].
 *
 * @param application the Android application.
 */
@OptIn(FlowPreview::class)
class EndConditionConfigModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Currently configured end conditions. */
    private val editedEndCondition: Flow<EndCondition> = editionRepository.editionState.editedEndConditionState
        .mapNotNull { it.value }

    /** Tells if the user is currently editing a end condition. If that's not the case, dialog should be closed. */
    val isEditingEndCondition: Flow<Boolean> = editionRepository.isEditingEndCondition
        .distinctUntilChanged()
        .debounce(1000)

    /** The number of executions before triggering the end condition. */
    val executions = editedEndCondition
        .filterNotNull()
        .map { it.executions }
        .take(1)
    /** Tells if the execution count is valid or not. */
    val executionCountError: Flow<Boolean> = editedEndCondition
        .map { it.executions <= 0 }

    /** True if this end condition is valid and can be saved, false if not. */
    val endConditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedEndConditionState
        .map { it.canBeSaved }

    /** The event selected for the end condition. Null if none is. */
    val eventViewState: Flow<EventPickerViewState> = editionRepository.editionState.eventsState
        .combine(editedEndCondition) { events, endCondition ->
            events.value?.find { event -> endCondition.eventId == event.id }
        }
        .combine(editionRepository.editionState.eventsAvailableForNewEndCondition) { selectedEvent, events ->
            when {
                selectedEvent != null -> EventPickerViewState.Selected(selectedEvent, events)
                events.isEmpty() -> EventPickerViewState.NoEvents
                else -> EventPickerViewState.NoSelection(events)
            }
        }

    /**
     * Set the event for the configured end condition
     * @param event the new event.
     */
    fun setEvent(event: Event) {
        editionRepository.editionState.getEditedEndCondition()?.let { endCondition ->
            editionRepository.updateEditedEndCondition(
                endCondition.copy(
                    eventId = event.id,
                    eventName = event.name,
                )
            )
        }
    }

    /**
     * Set the number of execution for the linked event.
     * @param executions the number of event executions.
     */
    fun setExecutions(executions: Int) {
        editionRepository.editionState.getEditedEndCondition()?.let { endCondition ->
            editionRepository.updateEditedEndCondition(
                endCondition.copy(
                    executions = executions
                )
            )
        }
    }
}