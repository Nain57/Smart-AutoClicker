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
package com.buzbuz.smartautoclicker.overlays.config.action.pause

import android.app.Application
import android.content.SharedPreferences

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.base.utils.isValidDuration
import com.buzbuz.smartautoclicker.overlays.base.utils.putPauseDurationConfig

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class PauseViewModel(application: Application) : AndroidViewModel(application) {

    /** The action being configured by the user. Defined using [setConfiguredSwipe]. */
    private val configuredPause = MutableStateFlow<Action.Pause?>(null)
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()

    /** The name of the pause. */
    val name: Flow<String?> = configuredPause
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredPause.map { it?.name?.isEmpty() ?: true }

    /** The duration of the pause in milliseconds. */
    val pauseDuration: Flow<String?> = configuredPause
        .filterNotNull()
        .map { it.pauseDuration?.toString() }
        .take(1)
    /** Tells if the pause duration value is valid or not. */
    val pauseDurationError: Flow<Boolean> = configuredPause.map { (it?.pauseDuration ?: -1) <= 0 }

    /** Tells if the configured pause is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredPause
        .map { pause ->
            pause != null && !pause.name.isNullOrEmpty() && pause.pauseDuration.isValidDuration()
        }

    /**
     * Set the configured pause.
     * This will update all values represented by this view model.
     *
     * @param pause the pause to configure.
     */
    fun setConfiguredSwipe(pause: Action.Pause) {
        configuredPause.value = pause.deepCopy()
    }

    /** @return the pause containing all user changes. */
    fun getConfiguredPause(): Action.Pause =
        configuredPause.value ?: throw IllegalStateException("Can't get the configured pause, none were defined.")

    /**
     * Set the name of the pause.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredPause.value?.let { pause ->
            configuredPause.value = pause.copy(name = "" + name)
        }
    }

    /**
     * Set the duration of the pause.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPauseDuration(durationMs: Long?) {
        configuredPause.value?.let { pause ->
            configuredPause.value = pause.copy(pauseDuration = durationMs)
        }
    }

    fun saveLastConfig() {
        configuredPause.value?.let { swipe ->
            sharedPreferences.edit().putPauseDurationConfig(swipe.pauseDuration ?: 0).apply()
        }
    }
}