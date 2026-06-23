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
package com.buzbuz.smartautoclicker.core.settings.engine

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortSettings
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortType
import com.buzbuz.smartautoclicker.core.settings.engine.data.ScenarioSortSettingsDataSource
import com.buzbuz.smartautoclicker.core.settings.engine.data.SettingsDataSource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

// TODO: Cleanup those state flows, this should be done by the caller
@Singleton
internal class SettingsRepositoryImpl @Inject constructor(
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val dataSource: SettingsDataSource,
    private val scenarioSortSettingsDatasource: ScenarioSortSettingsDataSource,
) : SettingsRepository {

    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _isLegacyActionUiEnabledFlow: StateFlow<Boolean> = dataSource.isLegacyActionUiEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isLegacyActionUiEnabledFlow: Flow<Boolean> = _isLegacyActionUiEnabledFlow

    private val _isLegacyNotificationUiEnabledFlow: StateFlow<Boolean> = dataSource.isLegacyNotificationUiEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isLegacyNotificationUiEnabledFlow: Flow<Boolean> = _isLegacyNotificationUiEnabledFlow

    private val _isEntireScreenCaptureForcedFlow: StateFlow<Boolean> = dataSource.isEntireScreenCaptureForced()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isEntireScreenCaptureForcedFlow: Flow<Boolean> = _isEntireScreenCaptureForcedFlow

    private val _isFilterScenarioUiEnabled: StateFlow<Boolean> = dataSource.isFilterScenarioUiEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isFilterScenarioUiEnabledFlow: Flow<Boolean> = _isFilterScenarioUiEnabled

    private val _isInputBlockWorkaroundEnabledFlow: StateFlow<Boolean> = dataSource.isInputBlockWorkaroundEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isInputBlockWorkaroundEnabledFlow: Flow<Boolean> = _isInputBlockWorkaroundEnabledFlow

    override val scenarioSortSettings: Flow<ScenarioSortSettings> = scenarioSortSettingsDatasource.getSortConfig()



    override fun toggleFilterScenarioUi() {
        coroutineScope.launch {
            dataSource.toggleFilterScenarioUi()
        }
    }

    override fun isLegacyActionUiEnabled(): Boolean =
        _isLegacyActionUiEnabledFlow.value

    override fun toggleLegacyActionUi() {
        coroutineScope.launch {
            dataSource.toggleLegacyActionUi()
        }
    }


    override fun isLegacyNotificationUiEnabled(): Boolean =
        _isLegacyNotificationUiEnabledFlow.value

    override fun toggleLegacyNotificationUi() {
        coroutineScope.launch {
            dataSource.toggleLegacyNotificationUi()
        }
    }


    override fun isEntireScreenCaptureForced(): Boolean =
        _isEntireScreenCaptureForcedFlow.value

    override fun toggleForceEntireScreenCapture() {
        coroutineScope.launch {
            dataSource.toggleForceEntireScreenCapture()
        }
    }


    override fun isInputBlockWorkaroundEnabled(): Boolean =
        _isInputBlockWorkaroundEnabledFlow.value

    override fun toggleInputBlockWorkaround() {
        coroutineScope.launch {
            dataSource.toggleInputBlockWorkaround()
        }
    }

    override fun setScenarioSortType(type: ScenarioSortType) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setSortType(type) }
    }

    override fun setScenarioSortOrder(invertSortOrder: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setSortOrder(invertSortOrder) }
    }

    override fun setScenarioSortShowDumb(show: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setShowDumb(show) }
    }

    override fun setScenarioSortShowSmart(show: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setShowSmart(show) }
    }
}