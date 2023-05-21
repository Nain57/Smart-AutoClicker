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

import com.buzbuz.smartautoclicker.core.ui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take

/** ViewModel for the [ToggleEventDialog].  */
class ToggleEventViewModel(application: Application) : AndroidViewModel(application) {

    /** The currently loaded scenario info. */
    private val editionRepository: EditionRepository = EditionRepository.getInstance(application)

    /** The action being configured by the user. */
    private val configuredToggleEvent = MutableStateFlow<Action.ToggleEvent?>(null)

    /** The name of the toggle event. */
    val name: Flow<String?> = configuredToggleEvent
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredToggleEvent.map { it?.name?.isEmpty() ?: true }

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
            when (toggleEventAction?.toggleEventType) {
                Action.ToggleEvent.ToggleType.ENABLE -> enableEventItem
                Action.ToggleEvent.ToggleType.DISABLE -> disableEventItem
                Action.ToggleEvent.ToggleType.TOGGLE -> toggleEventItem
                else -> throw IllegalStateException("Unknown toggle event type")
            }
        }
        .filterNotNull()

    /** All events currently edited in the attached scenario for this action. */
    private val availableEvents = editionRepository.editedEvents
        .filterNotNull()
        .combine(editionRepository.editedEvent) { scenarioEvents, editedEvent ->
            buildList {
                val found = scenarioEvents.find { scenarioEvent -> editedEvent?.id == scenarioEvent.id } != null
                if (!found && editedEvent != null) add(editedEvent)
                addAll(scenarioEvents)
            }
        }
    /** The event selected for the toggle action. Null if none is. */
    val eventViewState: Flow<EventPickerViewState> = configuredToggleEvent
        .filterNotNull()
        .combine(availableEvents) { editedAction, editedEvents ->
            val selectedEvent = editedEvents.find { editedEvent ->
                editedAction.toggleEventId == editedEvent.id
            }

            if (selectedEvent != null) EventPickerViewState.Selected(selectedEvent, editedEvents)
            else EventPickerViewState.NoSelection(editedEvents)
        }

    /** Tells if the configured click is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredToggleEvent
        .map { toggleEvent ->
            toggleEvent != null && !toggleEvent.name.isNullOrEmpty() && toggleEvent.toggleEventId != null
                    && toggleEvent.toggleEventType != null
        }

    /**
     * Set the configured toggle event.
     * This will update all values represented by this view model.
     *
     * @param action the toggle event action to configure.
     */
    fun setConfiguredToggleEvent(action: Action.ToggleEvent) {
        configuredToggleEvent.value = action.deepCopy()
    }

    /** @return the toggle event containing all user changes. */
    fun getConfiguredToggleEvent(): Action.ToggleEvent =
        configuredToggleEvent.value
            ?: throw IllegalStateException("Can't get the configured toggle event action, none were defined.")


    /**
     * Set the name of the toggle event action.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredToggleEvent.value?.let { action ->
            configuredToggleEvent.value = action.copy (name = "" + name)
        }
    }

    /**
     * Set the toggle type for the configured toggle event action.
     * @param item the new selected type.
     */
    fun setToggleType(item: DropdownItem) {
        val type = when (item) {
            enableEventItem -> Action.ToggleEvent.ToggleType.ENABLE
            disableEventItem -> Action.ToggleEvent.ToggleType.DISABLE
            toggleEventItem -> Action.ToggleEvent.ToggleType.TOGGLE
            else -> return
        }

        configuredToggleEvent.value?.let { action ->
            configuredToggleEvent.value = action.copy(toggleEventType = type)
        }
    }

    /**
     * Set the event for the configured toggle event action.
     * @param confEvent the new event.
     */
    fun setEvent(confEvent: Event) {
        configuredToggleEvent.value?.let { action ->
            configuredToggleEvent.value = action.copy(
                toggleEventId = confEvent.id,
            )
        }
    }
}