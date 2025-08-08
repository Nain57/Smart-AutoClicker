
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider

import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MoreViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val appComponentsProvider: AppComponentsProvider,
    private val debuggingRepository: DebuggingRepository,
) : ViewModel() {

    /** Tells if the debug view is enabled or not. */
    private val _isDebugViewEnabled = MutableStateFlow(debuggingRepository.isDebugViewEnabled(context))
    val isDebugViewEnabled: Flow<Boolean> = _isDebugViewEnabled

    /** Tells if the debug report is enabled or not. */
    private val _isDebugReportEnabled = MutableStateFlow(debuggingRepository.isDebugReportEnabled(context))
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
