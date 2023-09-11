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
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.buzbuz.smartautoclicker.feature.scenario.config.R

import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebuggingRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MoreViewModel(application: Application) : AndroidViewModel(application) {

    private val debuggingRepository: DebuggingRepository = DebuggingRepository.getDebuggingRepository(application)

    /** Tells if the debug view is enabled or not. */
    private val _isDebugViewEnabled = MutableStateFlow(debuggingRepository.isDebugViewEnabled(application))
    val isDebugViewEnabled: Flow<Boolean> = _isDebugViewEnabled

    /** Tells if the debug report is enabled or not. */
    private val _isDebugReportEnabled = MutableStateFlow(debuggingRepository.isDebugReportEnabled(application))
    val isDebugReportEnabled: Flow<Boolean> = _isDebugReportEnabled

    /** Tells if a debug report is available. */
    val debugReportAvailability: Flow<Boolean> = debuggingRepository.debugReport
        .map { it != null }

    fun toggleIsDebugViewEnabled() {
        _isDebugViewEnabled.value = !_isDebugViewEnabled.value
    }

    fun toggleIsDebugReportEnabled() {
        _isDebugReportEnabled.value = !_isDebugReportEnabled.value
    }

    fun saveConfig() {
        debuggingRepository.setDebuggingConfig(_isDebugViewEnabled.value, _isDebugReportEnabled.value)
    }
}
