
package com.buzbuz.smartautoclicker.core.settings

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.settings.data.SettingsDataSource
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

@Singleton
internal class SettingsRepositoryImpl @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val dataSource: SettingsDataSource,
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


    override fun isFilterScenarioUiEnabled(): Boolean =
        _isFilterScenarioUiEnabled.value


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
}