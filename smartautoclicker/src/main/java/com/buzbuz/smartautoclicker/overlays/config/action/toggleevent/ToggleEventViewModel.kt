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
package com.buzbuz.smartautoclicker.overlays.config.action.toggleevent

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem
import com.buzbuz.smartautoclicker.overlays.base.bindings.EventPickerViewState

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

    /** The edition action being configured by the user. Defined using [setConfiguredToggleEvent]. */
    private var configuredEditedAction = MutableStateFlow<EditedAction?>(null)
    /** The action being configured by the user. */
    private val configuredToggleEvent: Flow<Action.ToggleEvent?> = configuredEditedAction
        .map { editedAction ->
            val action = editedAction?.action ?: return@map null
            if (action is Action.ToggleEvent) action
            else null
        }

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
        .combine(editionRepository.editedEvent) { scenarioEvents, editedEvent ->
            buildList {
                val found = scenarioEvents.find { scenarioEvent -> editedEvent?.itemId == scenarioEvent.itemId } != null
                if (!found && editedEvent != null) add(editedEvent)
                addAll(scenarioEvents)
            }
        }
    /** The event selected for the toggle action. Null if none is. */
    val eventViewState: Flow<EventPickerViewState> = configuredEditedAction
        .filterNotNull()
        .combine(availableEvents) { editedAction, editedEvents ->
            val selectedEvent = editedEvents.find { editedEvent ->
                editedAction.toggleEventItemId == editedEvent.itemId
            }

            if (selectedEvent != null) EventPickerViewState.Selected(selectedEvent.event, editedEvents)
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
     * @param editedAction the toggle event action to configure.
     */
    fun setConfiguredToggleEvent(editedAction: EditedAction) {
        configuredEditedAction.value = editedAction.copy(
            action = editedAction.action.deepCopy(),
        )
    }

    /** @return the toggle event containing all user changes. */
    fun getConfiguredToggleEvent(): EditedAction {
        return configuredEditedAction.value
            ?: throw IllegalStateException("Can't get the configured toggle event action, none were defined.")
    }

    /**
     * Set the name of the toggle event action.
     * @param name the new name.
     */
    fun setName(name: String) {
        val editedAction = configuredEditedAction.value ?: return
        val toggleEvent = editedAction.action as Action.ToggleEvent

        configuredEditedAction.value = editedAction.copy(
            action = toggleEvent.copy(name = "" + name)
        )
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
        val editedAction = configuredEditedAction.value ?: return
        val toggleEvent = editedAction.action as Action.ToggleEvent

        configuredEditedAction.value = editedAction.copy(
            action = toggleEvent.copy(toggleEventType = type)
        )
    }

    /**
     * Set the event for the configured toggle event action.
     * @param confEvent the new event.
     */
    fun setEvent(confEvent: EditedEvent) {
        val editedAction = configuredEditedAction.value ?: return
        val toggleEvent = editedAction.action as Action.ToggleEvent

        configuredEditedAction.value = editedAction.copy(
            toggleEventItemId = confEvent.itemId,
            action = toggleEvent.copy(toggleEventId = confEvent.event.id)
        )
    }
}