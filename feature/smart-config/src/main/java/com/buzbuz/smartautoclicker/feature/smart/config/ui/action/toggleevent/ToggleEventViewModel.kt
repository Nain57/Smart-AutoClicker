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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent

import android.content.Context
import androidx.annotation.StringRes

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import javax.inject.Inject

/** ViewModel for the [ToggleEventDialog].  */
@OptIn(FlowPreview::class)
class ToggleEventViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

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

    /** The selected toggle all state for the action. */
    val toggleAllEnabledButton: Flow<ToggleAllButtonState> = configuredToggleEvent
        .map { toggleEventAction ->
            when {
                !toggleEventAction.toggleAll ->
                    ToggleAllButtonState(null, R.string.item_desc_toggle_event_manual)
                toggleEventAction.toggleAllType == Action.ToggleEvent.ToggleType.ENABLE ->
                    ToggleAllButtonState(BUTTON_ENABLE_EVENT,R.string.item_desc_toggle_event_enable_all)
                toggleEventAction.toggleAllType == Action.ToggleEvent.ToggleType.TOGGLE ->
                    ToggleAllButtonState(BUTTON_TOGGLE_EVENT, R.string.item_desc_toggle_event_invert_all)
                toggleEventAction.toggleAllType == Action.ToggleEvent.ToggleType.DISABLE ->
                    ToggleAllButtonState(BUTTON_DISABLE_EVENT, R.string.item_desc_toggle_event_disable_all)
                else -> null
            }
        }
        .filterNotNull()

    val eventToggleSelectorState: Flow<EventToggleSelectorState> = configuredToggleEvent
        .map { toggleEventAction ->
            var enableCount = 0
            var toggleCount = 0
            var disableCount = 0

            toggleEventAction.eventToggles.forEach {
                when (it.toggleType) {
                    Action.ToggleEvent.ToggleType.ENABLE -> enableCount++
                    Action.ToggleEvent.ToggleType.TOGGLE -> toggleCount++
                    Action.ToggleEvent.ToggleType.DISABLE -> disableCount++
                }
            }

            EventToggleSelectorState(
                isEnabled = !toggleEventAction.toggleAll,
                title = context.getEventToggleListName(toggleEventAction),
                emptyText = if (toggleEventAction.eventToggles.isEmpty()) R.string.item_desc_event_toggles_empty else null,
                enableCount = enableCount,
                toggleCount = toggleCount,
                disableCount = disableCount,
            )
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
     * Set the toggle all type for the configured toggle event action.
     * @param checkedButtonId the new selected type.
     */
    fun setToggleAllType(checkedButtonId: Int?) {
        editionRepository.editionState.getEditedAction<Action.ToggleEvent>()?.let { toggleEvent ->
            val type = when (checkedButtonId) {
                BUTTON_ENABLE_EVENT -> Action.ToggleEvent.ToggleType.ENABLE
                BUTTON_TOGGLE_EVENT -> Action.ToggleEvent.ToggleType.TOGGLE
                BUTTON_DISABLE_EVENT -> Action.ToggleEvent.ToggleType.DISABLE
                null -> null
                else -> return
            }
            if (type == toggleEvent.toggleAllType) return

            editionRepository.updateEditedAction(
                toggleEvent.copy(
                    toggleAll = type != null,
                    toggleAllType = type
                )
            )
        }
    }

    fun setNewEventToggles(toggles: List<EventToggle>) {
        editionRepository.editionState.getEditedAction<Action.ToggleEvent>()?.let { toggleEvent ->
            editionRepository.updateEditedAction(
                toggleEvent.copy(
                    toggleAll = false,
                    toggleAllType = null,
                    eventToggles = toggles,
                )
            )
        }
    }

    private fun Context.getEventToggleListName(toggleEventAction: Action.ToggleEvent): String =
        if (toggleEventAction.eventToggles.isEmpty()) getString(R.string.item_title_empty_event_toggles)
        else getString(R.string.item_title_event_toggles, toggleEventAction.eventToggles.size)

}

data class ToggleAllButtonState(
    val checkedButton: Int?,
    @StringRes val descriptionText: Int,
)

data class EventToggleSelectorState(
    val isEnabled: Boolean,
    val title: String,
    @StringRes val emptyText: Int?,
    val enableCount: Int?,
    val toggleCount: Int?,
    val disableCount: Int?,
)

internal const val BUTTON_ENABLE_EVENT = 0
internal const val BUTTON_TOGGLE_EVENT = 1
internal const val BUTTON_DISABLE_EVENT = 2