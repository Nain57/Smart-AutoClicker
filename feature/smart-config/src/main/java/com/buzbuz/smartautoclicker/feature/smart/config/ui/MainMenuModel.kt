/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.Context
import android.util.Log
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository

import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugLiveDetectionResultUseCase
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.core.smart.debugging.utils.formatDebugConfidenceRate
import com.buzbuz.smartautoclicker.core.smart.debugging.utils.formatConditionResultsDisplayText
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

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
import kotlin.time.Duration.Companion.milliseconds

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val smartProcessingRepository: SmartProcessingRepository,
    private val editionRepository: EditionRepository,
    private val tutorialRepository: TutorialRepository,
    private val revenueRepository: IRevenueRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    debuggingRepository: DebuggingRepository,
    debugDetectionResultUseCase: GetDebugLiveDetectionResultUseCase,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = smartProcessingRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private var paywallResultJob: Job? = null

    /** Tells if the paywall is currently displayed. */
    val paywallIsVisible: Flow<Boolean> =
        revenueRepository.isBillingFlowInProgress

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = smartProcessingRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState.Idle,
        )

    val isMediaProjectionStarted: StateFlow<Boolean> = smartProcessingRepository.detectionState
        .map { it == DetectionState.RECORDING || it == DetectionState.DETECTING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    val isStartButtonEnabled: Flow<Boolean> = combine(
        smartProcessingRepository.canStartDetection,
        editionRepository.isEditionSynchronized,
        isMediaProjectionStarted
    ) { canStartDetection, isSynchronized, isProjectionStarted ->
        (canStartDetection || !isProjectionStarted) && isSynchronized
    }

    /** Tells if the detector can't work due to a native library load error. */
    val nativeLibError: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = debuggingRepository.isLiveDebugging

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<DebugInfoUiState> = debugDetectionResultUseCase
        .invoke(displayDuration = POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
        .combine(isDebugging) { results, isDebugging -> if (isDebugging) results else null }
        .map { result -> result?.toLastPositiveDebugInfo() ?: DebugInfoUiState() }

    /** Load an advertisement, if needed. Should be called before showing the paywall to reduce user waiting time. */
    fun loadAdIfNeeded(context: Context) {
        revenueRepository.loadAdIfNeeded(context)
    }

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context) {
        when (detectionState.value) {
            UiState.Detecting -> stopDetection()
            UiState.Idle -> {
                if (revenueRepository.userBillingState.value.isAdRequested()) startPaywall(context)
                else startDetection(context)
            }
        }
    }

    /** Stop the detection. Returns true if it was started, false if not. */
    fun stopDetection(): Boolean {
        if (detectionState.value !is UiState.Detecting) return false

        smartProcessingRepository.stopDetection()
        return true
    }

    private fun startPaywall(context: Context) {
        revenueRepository.startPaywallUiFlow(context)

        paywallResultJob = combine(revenueRepository.isBillingFlowInProgress, revenueRepository.userBillingState) { inProgress, state ->
            if (inProgress) return@combine

            Log.d(TAG, "onPaywall finished")

            if (!state.isAdRequested()) startDetection(context)
            paywallResultJob?.cancel()
            paywallResultJob = null
        }.launchIn(viewModelScope)
    }

    private fun startDetection(context: Context) {
        viewModelScope.launch {
            smartProcessingRepository.startDetection(
                context = context,
                autoStopDuration = revenueRepository.consumeTrial(),
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

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        detectionState.value == UiState.Idle && tutorialRepository.shouldShowStopWithVolumeDownTutorialDialog()

    fun setStopWithVolumeDownDontShowAgain(): Unit =
        tutorialRepository.setStopWithVolumeDownDontShowAgain()

    private fun UserBillingState.isAdRequested(): Boolean =
        this == UserBillingState.AD_REQUESTED

    private fun DebugLiveImageEventOccurrence.toLastPositiveDebugInfo(): DebugInfoUiState {
        val firstPositiveCondition = imageConditionsResults.find { conditionResult -> conditionResult.isFulfilled }
        return DebugInfoUiState(
            eventText = event.name,
            conditionText = formatConditionResultsDisplayText(),
            confidenceRateText = firstPositiveCondition?.confidenceRate?.formatDebugConfidenceRate() ?: "",
        )
    }
}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

/**
 * Info on the last positive detection.
 * @param eventText name of the event
 * @param conditionText the name of the condition detected.
 * @param confidenceRateText the text to display for the confidence rate
 */
data class DebugInfoUiState(
    val eventText: String = "",
    val conditionText: String = "",
    val confidenceRateText: String = "",
)

/** Delay before removing the last positive result display in debug. */
private val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500.milliseconds

private const val TAG = "MainMenuViewModel"