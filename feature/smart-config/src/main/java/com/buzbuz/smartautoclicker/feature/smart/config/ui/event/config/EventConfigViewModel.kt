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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.config

import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

import javax.inject.Inject

class EventConfigViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** Currently configured event. */
    private val configuredEvent = editionRepository.editionState.editedEventState
        .mapNotNull { it.value }

    /** The enabled on start state of the configured event. */
    val eventEnabledOnStart: Flow<Boolean> = configuredEvent
        .map { event -> event.enabledOnStart }

    /** The event condition operator currently edited by the user. */
    val conditionOperator: Flow<Int> = configuredEvent
        .map { event -> event.conditionOperator }

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> = configuredEvent
        .filterNotNull()
        .map { it.name }
        .take(1)

    /** Tells if the event name is valid or not. */
    val eventNameError: Flow<Boolean> = configuredEvent
        .map { it.name.isEmpty() }

    val shouldShowTryCard: Flow<Boolean> = configuredEvent
        .map { it is ImageEvent }

    val canTryEvent: Flow<Boolean> = configuredEvent
        .map { it.isComplete() }

    fun getTryInfo(): Pair<Scenario, ImageEvent>? {
        val scenario = editionRepository.editionState.getScenario() ?: return null
        val event = editionRepository.editionState.getEditedEvent<ImageEvent>() ?: return null

        return scenario to event
    }


    /** Set a new name for the configured event. */
    fun setEventName(newName: String) {
        updateEditedEvent { oldValue -> oldValue.copyBase(name = newName) }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(@ConditionOperator operator: Int) {
        updateEditedEvent { oldValue ->
            oldValue.copyBase(conditionOperator = operator)
        }
    }

    /** Toggle the event state between true and false. */
    fun toggleEventState() {
        updateEditedEvent { oldValue ->
            oldValue.copyBase(enabledOnStart = !oldValue.enabledOnStart)
        }
    }

    private fun updateEditedEvent(closure: (oldValue: Event) -> Event?) {
        editionRepository.editionState.getEditedEvent<Event>()?.let { oldValue ->
            viewModelScope.launch {
                closure(oldValue)?.let { newValue ->
                    editionRepository.updateEditedEvent(newValue)
                }
            }
        }
    }

    fun monitorConditionOperatorItemAndView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND, view,)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND)
    }
}