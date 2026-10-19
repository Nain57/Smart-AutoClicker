/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more.debugview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


class DebugViewSettingsViewModel @Inject constructor(
    private val debuggingRepository: DebuggingRepository,
) : ViewModel() {

    private val settings: StateFlow<DebugViewSettingsUiState?> =
        combine(
            debuggingRepository.isDebugViewEventStatsShown,
            debuggingRepository.isDebugViewActionsShown,
            debuggingRepository.isDebugViewHeldFingerShown,
        ) { showEventStats, showActions, showHeldFinger ->
            DebugViewSettingsUiState(
                showEventStats = showEventStats,
                showActions = showActions,
                showHeldFinger = showHeldFinger,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: Flow<DebugViewSettingsUiState> = settings.filterNotNull()

    fun toggleShowEventStats() {
        val current = settings.value ?: return
        debuggingRepository.setDebugViewEventStatsShown(!current.showEventStats)
    }

    fun toggleShowActions() {
        val current = settings.value ?: return
        debuggingRepository.setDebugViewActionsShown(!current.showActions)
    }

    fun toggleShowHeldFinger() {
        val current = settings.value ?: return
        debuggingRepository.setDebugViewHeldFingerShown(!current.showHeldFinger)
    }
}

data class DebugViewSettingsUiState(
    val showEventStats: Boolean,
    val showActions: Boolean,
    val showHeldFinger: Boolean,
)
