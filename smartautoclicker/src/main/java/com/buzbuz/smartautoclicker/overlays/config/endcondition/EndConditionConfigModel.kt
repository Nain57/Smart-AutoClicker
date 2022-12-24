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
import com.buzbuz.smartautoclicker.overlays.config.ConfiguredEndCondition
import com.buzbuz.smartautoclicker.overlays.config.ConfiguredEvent
import com.buzbuz.smartautoclicker.overlays.config.INVALID_CONFIGURED_ITEM_ID
import com.buzbuz.smartautoclicker.overlays.config.EditionRepository

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
    private val configuredEndCondition = MutableStateFlow<ConfiguredEndCondition?>(null)

    /** The number of executions before triggering the end condition. */
    val executions = configuredEndCondition
        .filterNotNull()
        .map { it.endCondition.executions }
        .take(1)
    /** Tells if the execution count is valid or not. */
    val executionCountError: Flow<Boolean> = configuredEndCondition
        .map { (it?.endCondition?.executions ?: -1) <= 0 }
    /** Tells if the configured end condition can be deleted. */
    val canBeDeleted = configuredEndCondition
        .filterNotNull()
        .map { it.endCondition.id != 0L }
    /** True if this end condition is valid and can be saved, false if not. */
    val isValidEndCondition = configuredEndCondition.map { conf ->
        conf != null && conf.eventItemId != INVALID_CONFIGURED_ITEM_ID && conf.endCondition.executions > 0
    }

    /** Events available as end condition event for this end condition */
    val eventsAvailable: StateFlow<List<ConfiguredEvent>> = configuredScenario
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
    val eventViewState: Flow<EndConditionEventViewState> = configuredEndCondition
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
        configuredEndCondition.value?.endCondition?.id?.equals(0L) == false

    /**
     * Set the end condition to be configured.
     * @param endCondition the end condition.
     */
    fun setConfiguredEndCondition(endCondition: ConfiguredEndCondition) {
        configuredEndCondition.value = endCondition
    }

    /**
     * Set the event for the configured end condition
     * @param confEvent the new event.
     */
    fun setEvent(confEvent: ConfiguredEvent) {
        configuredEndCondition.value = configuredEndCondition.value?.let { conf ->
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
        configuredEndCondition.value = configuredEndCondition.value?.let { conf ->
            conf.copy(endCondition = conf.endCondition.copy(executions = executions))
        }
    }

    /** @return the end condition currently configured. */
    fun getConfiguredEndCondition(): ConfiguredEndCondition = configuredEndCondition.value!!
}

sealed class EndConditionEventViewState {
    object NoEvents : EndConditionEventViewState()
    object NoSelection: EndConditionEventViewState()
    data class Selected(val event: Event): EndConditionEventViewState()
}