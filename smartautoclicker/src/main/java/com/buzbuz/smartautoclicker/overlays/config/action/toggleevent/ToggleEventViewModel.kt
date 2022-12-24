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
package com.buzbuz.smartautoclicker.overlays.config.action.toggleevent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.buzbuz.smartautoclicker.domain.Action
import kotlinx.coroutines.flow.*

class ToggleEventViewModel(application: Application) : AndroidViewModel(application) {

    /** The action being configured by the user. Defined using [setConfiguredToggleEvent]. */
    private val configuredToggleEvent = MutableStateFlow<Action.ToggleEvent?>(null)

    /** The name of the toggle event. */
    val name: Flow<String?> = configuredToggleEvent
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredToggleEvent.map { it?.name?.isEmpty() ?: true }

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
     * @param toggleEvent the toggle event to configure.
     */
    fun setConfiguredToggleEvent(toggleEvent: Action.ToggleEvent) {
        configuredToggleEvent.value = toggleEvent.deepCopy()
    }

    /** @return the toggle event containing all user changes. */
    fun getConfiguredClick(): Action.ToggleEvent =
        configuredToggleEvent.value ?: throw IllegalStateException("Can't get the configured toggle event, none were defined.")

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredToggleEvent.value?.let { click ->
            configuredToggleEvent.value = click.copy(name = "" + name)
        }
    }
}