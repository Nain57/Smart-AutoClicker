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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.pause

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putSwipeDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putSwipeRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.putSwipeRepeatDelayConfig

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

class DumbPauseViewModel(application: Application) : AndroidViewModel(application) {

    private val _editedDumbPause: MutableStateFlow<DumbAction.DumbPause?> = MutableStateFlow(null)
    private val editedDumbPause: Flow<DumbAction.DumbPause> = _editedDumbPause.filterNotNull()

    /** Tells if the configured dumb pause is valid and can be saved. */
    val isValidDumbPause: Flow<Boolean> = _editedDumbPause
        .map { it != null && it.isValid() }

    /** The name of the pause. */
    val name: Flow<String> = editedDumbPause
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = editedDumbPause
        .map { it.name.isEmpty() }

    /** The duration of the pause. */
    val pauseDuration: Flow<String> = editedDumbPause
        .map { it.pauseDurationMs.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pauseDurationError: Flow<Boolean> = editedDumbPause
        .map { it.pauseDurationMs <= 0 }

    fun setEditedDumbPause(pause: DumbAction.DumbPause) {
        _editedDumbPause.value = pause.copy()
    }

    fun getEditedDumbPause(): DumbAction.DumbPause? =
        _editedDumbPause.value

    fun setName(newName: String) {
        _editedDumbPause.value = _editedDumbPause.value?.copy(name = newName)
    }

    fun setPauseDurationMs(durationMs: Long) {
        _editedDumbPause.value = _editedDumbPause.value?.copy(pauseDurationMs = durationMs)
    }

    fun saveLastConfig(context: Context) {
        _editedDumbPause.value?.let { pause ->
            context.getDumbConfigPreferences()
                .edit()
                .putPauseDurationConfig(pause.pauseDurationMs)
                .apply()
        }
    }
}