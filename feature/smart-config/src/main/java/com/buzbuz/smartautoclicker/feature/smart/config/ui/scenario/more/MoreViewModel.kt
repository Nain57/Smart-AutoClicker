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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


class MoreViewModel @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val appComponentsProvider: AppComponentsProvider,
    private val debuggingRepository: DebuggingRepository,
) : ViewModel() {

    /** Tells if the debug view is enabled or not. */
    private val _isDebugViewEnabled = MutableStateFlow(debuggingRepository.isDebugViewEnabled())
    val isDebugViewEnabled: Flow<Boolean> = _isDebugViewEnabled

    /** Tells if the debug report is enabled or not. */
    private val _isDebugReportEnabled = MutableStateFlow(debuggingRepository.isDebugReportEnabled())
    val isDebugReportEnabled: Flow<Boolean> = _isDebugReportEnabled

    /** Tells if a debug report is available. */
    private val _isDebugReportAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDebugReportAvailable: Flow<Boolean> = _isDebugReportAvailable

    init {
        viewModelScope.launch(ioDispatcher) {
            _isDebugReportAvailable.update { debuggingRepository.isDebugReportAvailable() }
        }
    }

    fun toggleIsDebugViewEnabled() {
        _isDebugViewEnabled.value = !_isDebugViewEnabled.value
    }

    fun toggleIsDebugReportEnabled() {
        _isDebugReportEnabled.value = !_isDebugReportEnabled.value
    }

    fun saveConfig() {
        debuggingRepository.setDebuggingConfig(_isDebugViewEnabled.value, _isDebugReportEnabled.value)
    }

    fun getTutorialActivityComponent(): ComponentName =
        appComponentsProvider.tutorialActivityComponentName
}
