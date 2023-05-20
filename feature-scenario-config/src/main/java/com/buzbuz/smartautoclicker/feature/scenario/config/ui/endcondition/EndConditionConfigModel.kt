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

import com.buzbuz.smartautoclicker.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take

/**
 * View model for the [EndConditionConfigDialog].
 *
 * @param application the Android application.
 */
class EndConditionConfigModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Currently configured events. */
    private val editedEvents = editionRepository.editedEvents
        .filterNotNull()
    /** Currently configured end conditions. */
    private val editedEndConditions = editionRepository.editedEndConditions
        .filterNotNull()

    /** The configured end condition. */
    private val editedEndCondition = MutableStateFlow<EndCondition?>(null)

    /** The number of executions before triggering the end condition. */
    val executions = editedEndCondition
        .filterNotNull()
        .map { it.executions }
        .take(1)
    /** Tells if the execution count is valid or not. */
    val executionCountError: Flow<Boolean> = editedEndCondition
        .map { (it?.executions ?: -1) <= 0 }

    /** True if this end condition is valid and can be saved, false if not. */
    val isValidEndCondition: Flow<Boolean> = editedEndCondition.map { endCondition ->
        endCondition?.eventId != null && endCondition.executions > 0
    }

    /** Events available as end condition event for this end condition */
    private val eventsAvailable: Flow<List<Event>> = editedEvents
        .combine(editedEndConditions) { events, endConditions ->
            events.filter { event ->
                endConditions.find { it.eventId == event.id } == null
            }
        }

    /** The event selected for the end condition. Null if none is. */
    val eventViewState: Flow<EventPickerViewState> = editedEvents
        .combine(editedEndCondition) { events, endCondition ->
            events.find { event ->
                endCondition?.eventId == event.id
            }
        }
        .combine(eventsAvailable) { selectedEvent, scenarioEvents ->
            when {
                selectedEvent != null -> EventPickerViewState.Selected(selectedEvent, scenarioEvents)
                scenarioEvents.isEmpty() -> EventPickerViewState.NoEvents
                else -> EventPickerViewState.NoSelection(scenarioEvents)
            }
        }

    /**
     * Set the end condition to be configured.
     * @param endCondition the end condition.
     */
    fun setConfiguredEndCondition(endCondition: EndCondition) {
        editedEndCondition.value = endCondition
    }

    /**
     * Set the event for the configured end condition
     * @param event the new event.
     */
    fun setEvent(event: Event) {
        editedEndCondition.value = editedEndCondition.value?.copy(
            eventId = event.id,
            eventName = event.name
        )
    }

    /**
     * Set the number of execution for the linked event.
     * @param executions the number of event executions.
     */
    fun setExecutions(executions: Int) {
        editedEndCondition.value = editedEndCondition.value?.copy(
            executions = executions
        )
    }

    /** @return the end condition currently configured. */
    fun getConfiguredEndCondition(): EndCondition = editedEndCondition.value!!
}