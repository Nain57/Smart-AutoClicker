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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** ViewModel for the [ScenarioDialog] and its content. */
@OptIn(FlowPreview::class)
class ScenarioDialogViewModel @Inject constructor(
    editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
): ViewModel() {

    private val editedScenarioHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.scenarioCompleteState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = combine(
        editionRepository.editionState.scenarioState.filterNotNull(),
        editionRepository.editionState.editedScreenEventsState.filterNotNull(),
        editionRepository.editionState.editedTriggerEventsState.filterNotNull(),
    ) { scenarioState, imageEventsState, triggerEventsState ->
        buildMap {
            put(R.id.page_image_events, imageEventsState.canBeSaved &&
                    (!imageEventsState.value.isNullOrEmpty() || !triggerEventsState.value.isNullOrEmpty()))
            put(R.id.page_trigger_events, triggerEventsState.canBeSaved &&
                    (!imageEventsState.value.isNullOrEmpty() || !triggerEventsState.value.isNullOrEmpty()))
            put(R.id.page_config, scenarioState.canBeSaved)
            put(R.id.page_more, true)
        }
    }

    /** Tells if the user is currently editing a scenario. If that's not the case, dialog should be closed. */
    val isEditingScenario: Flow<Boolean> = editionRepository.isEditingScenario
        .distinctUntilChanged()
        .debounce(1000)

    /** Tells if the configured scenario is valid and can be saved in database. */
    val scenarioCanBeSaved: Flow<Boolean> =
        combine(editionRepository.editionState.scenarioCompleteState, editionRepository.isEditingScenario) { state, isEditing ->
            state.canBeSaved && isEditing
        }

    fun hasUnsavedModifications(): Boolean =
        editedScenarioHasChanged.value

    fun monitorViews(createEventButton: View, saveButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT, createEventButton)
            attach(MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE, saveButton)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT)
            detach(MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE)
        }
    }
}
