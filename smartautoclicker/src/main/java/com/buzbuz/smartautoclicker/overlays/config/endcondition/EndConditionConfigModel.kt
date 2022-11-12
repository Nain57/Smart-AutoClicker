/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.endcondition

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

/**
 * View model for the [EndConditionConfigDialog].
 *
 * @param application the Android application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndConditionConfigModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)

    /** The configured end condition. */
    private val configuredEndCondition = MutableStateFlow<EndCondition?>(null)
    /** The list of current end conditions for this scenario. */
    private val currentEndConditions = MutableStateFlow<List<EndCondition>>(emptyList())
    /** The complete list of events for this scenario. */
    private val events = configuredEndCondition
        .filterNotNull()
        .flatMapLatest {
            repository.getCompleteEventListFlow(it.scenarioId)
        }

    /** The number of executions before triggering the end condition. */
    val executions = configuredEndCondition
        .filterNotNull()
        .map { it.executions }
        .take(1)
    /** Tells if the configured end condition can be deleted. */
    val canBeDeleted = configuredEndCondition
        .filterNotNull()
        .map { it.id != 0L }
    /** True if this end condition is valid and can be saved, false if not. */
    val isValidEndCondition = configuredEndCondition.map { endCondition ->
        endCondition != null && endCondition.eventId != 0L && endCondition.executions > 0
    }

    /** Events available as end condition event for this end condition */
    val eventsAvailable = combine(
        currentEndConditions,
        configuredEndCondition,
        events
    ) { endConditions, endCondition, events ->
            events.filter { event ->
                endCondition?.eventId == event.id || endConditions.find { it.eventId == event.id } == null
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )
    /** The event selected for the end condition. Null if none is. */
    val eventViewState = configuredEndCondition
        .combine(events) { endCondition, events ->
            events.find { event -> endCondition?.eventId == event.id }
        }
        .combine(eventsAvailable) { selectedEvent, scenarioEvents ->
            when {
                scenarioEvents.isEmpty() -> EndConditionEventViewState.NoEvents
                selectedEvent == null -> EndConditionEventViewState.NoSelection
                else -> EndConditionEventViewState.Selected(selectedEvent)
            }
        }

    /** Tells if the delete button should be shown. */
    fun shouldShowDeleteButton(): Boolean =
        configuredEndCondition.value?.id?.equals(0L) == false

    /**
     * Set the end condition to be configured.
     * @param endCondition the end condition.
     * @param endConditions the complete list of events for this scenario.
     */
    fun setEndCondition(endCondition: EndCondition, endConditions: List<EndCondition>) {
        configuredEndCondition.value = endCondition.copy()
        currentEndConditions.value = endConditions
    }

    /**
     * Set the event for the configured end condition
     * @param event the new event.
     */
    fun setEvent(event: Event) {
        configuredEndCondition.value = configuredEndCondition.value?.copy(eventId = event.id, eventName = event.name)
    }

    /**
     * Set the number of execution for the linked event.
     * @param executions the number of event executions.
     */
    fun setExecutions(executions: Int) {
        configuredEndCondition.value = configuredEndCondition.value?.copy(executions = executions)
    }

    /** @return the end condition currently configured. */
    fun getConfiguredEndCondition(): EndCondition = configuredEndCondition.value!!
}

sealed class EndConditionEventViewState {
    object NoEvents : EndConditionEventViewState()
    object NoSelection: EndConditionEventViewState()
    data class Selected(val event: Event): EndConditionEventViewState()
}