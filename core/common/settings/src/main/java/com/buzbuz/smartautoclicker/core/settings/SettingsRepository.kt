
package com.buzbuz.smartautoclicker.core.settings

import kotlinx.coroutines.flow.Flow


interface SettingsRepository {

    val isLegacyActionUiEnabledFlow: Flow<Boolean>
    fun isLegacyActionUiEnabled(): Boolean
    fun toggleLegacyActionUi()

    val isLegacyNotificationUiEnabledFlow: Flow<Boolean>
    fun isLegacyNotificationUiEnabled(): Boolean
    fun toggleLegacyNotificationUi()

    val isEntireScreenCaptureForcedFlow: Flow<Boolean>
    fun isEntireScreenCaptureForced(): Boolean
    fun toggleForceEntireScreenCapture()

    val isFilterScenarioUiEnabledFlow: Flow<Boolean>
    fun isFilterScenarioUiEnabled(): Boolean
    fun toggleFilterScenarioUi()

    val isInputBlockWorkaroundEnabledFlow: Flow<Boolean>
    fun isInputBlockWorkaroundEnabled(): Boolean
    fun toggleInputBlockWorkaround()
}