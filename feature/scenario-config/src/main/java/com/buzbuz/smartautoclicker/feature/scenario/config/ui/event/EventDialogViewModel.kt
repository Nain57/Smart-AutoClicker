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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class)
class EventDialogViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository containing the user editions. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = combine(
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedEventConditionsState,
        editionRepository.editionState.editedEventActionsState,
    ) { editedEvent, conditions, actions, ->
        buildMap {
            put(R.id.page_event, editedEvent.value?.name?.isNotEmpty() ?: false)
            put(R.id.page_conditions, conditions.canBeSaved)
            put(R.id.page_actions, actions.canBeSaved)
        }
    }

    /** Tells if the configured event is valid and can be saved. */
    val eventCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedEventState
        .map { it.canBeSaved }

    /** Tells if the user is currently editing an event. If that's not the case, dialog should be closed. */
    val isEditingEvent: Flow<Boolean> = editionRepository.isEditingEvent
        .distinctUntilChanged()
        .debounce(1000)

    /** Tells if this event have associated end conditions. */
    fun isEventHaveRelatedEndConditions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByEndCondition()

    /** Tells if this event have associated actions. */
    fun isEventHaveRelatedActions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByAction()

    fun monitorActionTabView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS, view)
    }
    fun monitorConditionTabView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS, view)
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS)
            detach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS)
        }
    }
}