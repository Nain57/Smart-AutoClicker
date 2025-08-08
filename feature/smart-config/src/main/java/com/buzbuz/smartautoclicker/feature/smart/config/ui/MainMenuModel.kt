
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.Context
import android.util.Log
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val debugRepository: DebuggingRepository,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = detectionRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private var paywallResultJob: Job? = null

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = detectionRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState.Idle,
        )

    val isMediaProjectionStarted: StateFlow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.RECORDING || it == DetectionState.DETECTING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    val isStartButtonEnabled: Flow<Boolean> = combine(
        detectionRepository.canStartDetection,
        editionRepository.isEditionSynchronized,
        isMediaProjectionStarted
    ) { canStartDetection, isSynchronized, isProjectionStarted ->
        (canStartDetection || !isProjectionStarted) && isSynchronized
    }

    /** Tells if the detector can't work due to a native library load error. */
    val nativeLibError: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context) {
        when (detectionState.value) {
            UiState.Detecting -> stopDetection()
            UiState.Idle -> {
                startDetection(context)
            }
        }
    }

    /** Stop the detection. Returns true if it was started, false if not. */
    fun stopDetection(): Boolean {
        if (detectionState.value !is UiState.Detecting) return false

        detectionRepository.stopDetection()
        return true
    }

    private fun startDetection(context: Context) {
        viewModelScope.launch {
            detectionRepository.startDetection(
                context,
                debugRepository.getDebugDetectionListenerIfNeeded(context),
            )
        }
    }

    fun startScenarioEdition(onEditionStarted: () -> Unit) {
        scenarioDbId.value?.let { scenarioDatabaseId ->
            viewModelScope.launch(Dispatchers.IO) {
                if (editionRepository.startEdition(scenarioDatabaseId)) {
                    withContext(Dispatchers.Main) { onEditionStarted() }
                }
            }
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges(onCompleted: (success: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = editionRepository.saveEditions()

            withContext(Dispatchers.Main) {
                onCompleted(result)
            }
        }
    }

    /** Cancel all changes made by the user. */
    fun cancelScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.stopEdition()
        }
    }

    fun monitorViews(playMenuButton: View, configMenuButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY, playMenuButton, ViewPositioningType.SCREEN)
            attach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG, configMenuButton, ViewPositioningType.SCREEN)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
            detach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG)
        }
    }

    fun shouldRestartMediaProjection(): Boolean =
        !isMediaProjectionStarted.value

}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

private const val TAG = "MainMenuViewModel"