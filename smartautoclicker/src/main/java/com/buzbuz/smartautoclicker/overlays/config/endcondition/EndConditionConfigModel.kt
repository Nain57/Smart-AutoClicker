/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.endcondition

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.edition.EditedEndCondition
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent
import com.buzbuz.smartautoclicker.domain.edition.INVALID_EDITED_ITEM_ID
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * View model for the [EndConditionConfigDialog].
 *
 * @param application the Android application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndConditionConfigModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Currently configured scenario. */
    private val configuredScenario = editionRepository.editedScenario
        .filterNotNull()

    /** The configured end condition. */
    private val editedEndCondition = MutableStateFlow<EditedEndCondition?>(null)

    /** The number of executions before triggering the end condition. */
    val executions = editedEndCondition
        .filterNotNull()
        .map { it.endCondition.executions }
        .take(1)
    /** Tells if the execution count is valid or not. */
    val executionCountError: Flow<Boolean> = editedEndCondition
        .map { (it?.endCondition?.executions ?: -1) <= 0 }
    /** Tells if the configured end condition can be deleted. */
    val canBeDeleted = editedEndCondition
        .filterNotNull()
        .map { it.endCondition.id != 0L }
    /** True if this end condition is valid and can be saved, false if not. */
    val isValidEndCondition = editedEndCondition.map { conf ->
        conf != null && conf.eventItemId != INVALID_EDITED_ITEM_ID && conf.endCondition.executions > 0
    }

    /** Events available as end condition event for this end condition */
    val eventsAvailable: StateFlow<List<EditedEvent>> = configuredScenario
        .map { scenario ->
            scenario.events.filter { configuredEvent ->
                scenario.endConditions.find { it.endCondition.eventId == configuredEvent.event.id } == null
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    /** The event selected for the end condition. Null if none is. */
    val eventViewState: Flow<EndConditionEventViewState> = editedEndCondition
        .combine(configuredScenario) { confEndCondition, scenario ->
            scenario.events.find { configuredEvent ->
                confEndCondition?.eventItemId == configuredEvent.itemId
            }
        }
        .combine(eventsAvailable) { selectedEvent, scenarioEvents ->
            when {
                selectedEvent != null -> EndConditionEventViewState.Selected(selectedEvent.event)
                scenarioEvents.isEmpty() -> EndConditionEventViewState.NoEvents
                else -> EndConditionEventViewState.NoSelection
            }
        }

    /** Tells if the delete button should be shown. */
    fun shouldShowDeleteButton(): Boolean =
        editedEndCondition.value?.endCondition?.id?.equals(0L) == false

    /**
     * Set the end condition to be configured.
     * @param endCondition the end condition.
     */
    fun setConfiguredEndCondition(endCondition: EditedEndCondition) {
        editedEndCondition.value = endCondition
    }

    /**
     * Set the event for the configured end condition
     * @param confEvent the new event.
     */
    fun setEvent(confEvent: EditedEvent) {
        editedEndCondition.value = editedEndCondition.value?.let { conf ->
            conf.copy(
                endCondition = conf.endCondition.copy(eventId = confEvent.event.id, eventName = confEvent.event.name),
                eventItemId = confEvent.itemId,
            )
        }
    }

    /**
     * Set the number of execution for the linked event.
     * @param executions the number of event executions.
     */
    fun setExecutions(executions: Int) {
        editedEndCondition.value = editedEndCondition.value?.let { conf ->
            conf.copy(endCondition = conf.endCondition.copy(executions = executions))
        }
    }

    /** @return the end condition currently configured. */
    fun getConfiguredEndCondition(): EditedEndCondition = editedEndCondition.value!!
}

sealed class EndConditionEventViewState {
    object NoEvents : EndConditionEventViewState()
    object NoSelection: EndConditionEventViewState()
    data class Selected(val event: Event): EndConditionEventViewState()
}