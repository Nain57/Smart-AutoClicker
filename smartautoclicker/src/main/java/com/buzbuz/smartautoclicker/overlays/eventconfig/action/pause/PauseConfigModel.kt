/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.pause

import android.content.SharedPreferences
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.isValidDuration
import com.buzbuz.smartautoclicker.overlays.utils.putPauseDurationConfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Allow to observe/edit the values a pause action.
 *
 * @param viewModelScope the scope for the view model holding this model.
 * @param configuredAction the flow on the edited action.
 */
class PauseConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredAction: MutableStateFlow<Action?>,
) : ActionModel() {

    override val isValidAction: Flow<Boolean> = configuredAction
        .map { action ->
            action is Action.Pause && !action.name.isNullOrEmpty() && action.pauseDuration.isValidDuration()
        }

    override fun saveLastConfig(eventConfigPrefs: SharedPreferences) {
        configuredAction.value?.let { action ->
            if (action is Action.Pause) {
                eventConfigPrefs.edit().putPauseDurationConfig(action.pauseDuration ?: 0).apply()
            }
        }
    }

    /** The duration of the pause in milliseconds. */
    val pauseDuration: Flow<Long?> = configuredAction
        .filterIsInstance<Action.Pause>()
        .map { it.pauseDuration }
        .take(1)

    /**
     * Set the duration of the pause.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPauseDuration(durationMs: Long?) {
        (configuredAction.value as Action.Pause).let { pause ->
            viewModelScope.launch {
                configuredAction.value = pause.copy(pauseDuration = durationMs)
            }
        }
    }
}