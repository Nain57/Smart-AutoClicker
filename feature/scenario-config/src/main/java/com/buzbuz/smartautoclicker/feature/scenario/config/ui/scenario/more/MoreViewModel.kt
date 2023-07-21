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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.scenario.more

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class MoreViewModel(application: Application) : AndroidViewModel(application) {

    /** Debug configuration shared preferences. */
    private val repository: DebuggingRepository = DebuggingRepository.getDebuggingRepository(application)

    /** Tells if the debug view is enabled or not. */
    private val _isDebugViewEnabled = MutableStateFlow(repository.isDebugViewEnabled(application))
    val isDebugViewEnabled: Flow<Boolean> = _isDebugViewEnabled

    /** Tells if the debug report is enabled or not. */
    private val _isDebugReportEnabled = MutableStateFlow(repository.isDebugReportEnabled(application))
    val isDebugReportEnabled: Flow<Boolean> = _isDebugReportEnabled

    fun toggleIsDebugViewEnabled() {
        _isDebugViewEnabled.value = !_isDebugViewEnabled.value
    }

    fun toggleIsDebugReportEnabled() {
        _isDebugReportEnabled.value = !_isDebugReportEnabled.value
    }

    fun saveConfig() {
        repository.setDebuggingConfig(_isDebugViewEnabled.value, _isDebugReportEnabled.value)
    }
}