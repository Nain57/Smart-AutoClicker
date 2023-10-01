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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.toggleevent

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

/** ViewModel for the [ToggleEventDialog].  */
@OptIn(FlowPreview::class)
class ToggleEventViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The action being configured by the user. */
    private val configuredToggleEvent = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Action.ToggleEvent>()

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the toggle event. */
    val name: Flow<String?> = configuredToggleEvent
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredToggleEvent.map { it.name?.isEmpty() ?: true }

    private val enableEventItem = DropdownItem(
        title = R.string.dropdown_item_title_toggle_event_state_enable,
        helperText = R.string.dropdown_helper_text_toggle_event_state_enable,
    )
    private val disableEventItem = DropdownItem(
        title = R.string.dropdown_item_title_toggle_event_state_disable,
        helperText = R.string.dropdown_helper_text_toggle_event_state_disable,
    )
    private val toggleEventItem = DropdownItem(
        title = R.string.dropdown_item_title_toggle_event_state_toggle,
        helperText = R.string.dropdown_helper_text_toggle_event_state_toggle,
    )
    val toggleStateItems = listOf(enableEventItem, disableEventItem, toggleEventItem)

    /** The selected toggle state for the action. */
    val toggleStateItem: Flow<DropdownItem> = configuredToggleEvent
        .map { toggleEventAction ->
            when (toggleEventAction.toggleEventType) {
                Action.ToggleEvent.ToggleType.ENABLE -> enableEventItem
                Action.ToggleEvent.ToggleType.DISABLE -> disableEventItem
                Action.ToggleEvent.ToggleType.TOGGLE -> toggleEventItem
                else -> throw IllegalStateException("Unknown toggle event type")
            }
        }
        .filterNotNull()

    /** The event selected for the toggle action. Null if none is. */
    val eventViewState: Flow<EventPickerViewState> = configuredToggleEvent
        .combine(editionRepository.editionState.eventsAvailableForToggleEventAction) { editedAction, editedEvents ->
            val selectedEvent = editedEvents.find { editedEvent ->
                editedAction.toggleEventId == editedEvent.id
            }

            if (selectedEvent != null) EventPickerViewState.Selected(selectedEvent, editedEvents)
            else EventPickerViewState.NoSelection(editedEvents)
        }

    /** Tells if the configured toggle event action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    /**
     * Set the name of the toggle event action.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Action.ToggleEvent>()?.let { toggleEvent ->
            editionRepository.updateEditedAction(toggleEvent.copy (name = "" + name))
        }
    }

    /**
     * Set the toggle type for the configured toggle event action.
     * @param item the new selected type.
     */
    fun setToggleType(item: DropdownItem) {
        editionRepository.editionState.getEditedAction<Action.ToggleEvent>()?.let { toggleEvent ->
            val type = when (item) {
                enableEventItem -> Action.ToggleEvent.ToggleType.ENABLE
                disableEventItem -> Action.ToggleEvent.ToggleType.DISABLE
                toggleEventItem -> Action.ToggleEvent.ToggleType.TOGGLE
                else -> return
            }

            editionRepository.updateEditedAction(toggleEvent.copy(toggleEventType = type))
        }
    }

    /**
     * Set the event for the configured toggle event action.
     * @param confEvent the new event.
     */
    fun setEvent(confEvent: Event) {
        editionRepository.editionState.getEditedAction<Action.ToggleEvent>()?.let { toggleEvent ->
            editionRepository.updateEditedAction(toggleEvent.copy(toggleEventId = confEvent.id))
        }
    }
}