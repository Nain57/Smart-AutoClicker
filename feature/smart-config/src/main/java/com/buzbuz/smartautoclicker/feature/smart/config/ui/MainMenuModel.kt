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
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.Context
import android.util.Log
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.core.smart.training.TrainingRepository
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextDataSyncState
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val smartRepository: IRepository,
    private val editionRepository: EditionRepository,
    private val tutorialRepository: TutorialRepository,
    private val revenueRepository: IRevenueRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val debugRepository: DebuggingRepository,
    trainingRepository: TrainingRepository,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = detectionRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var paywallResultJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isLanguageFileDownloadRequired: StateFlow<Boolean> = scenarioDbId
        .flatMapLatest { id -> id?.let(smartRepository::getEventsFlow) ?: emptyFlow() }
        .map { events -> events.getAllTextLanguages() }
        .combine(trainingRepository.trainedTextLanguagesSyncState) { scenarioLanguages, syncStates ->
            scenarioLanguages.find { syncStates[it] !is TrainedTextDataSyncState.Downloaded } != null
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the paywall is currently displayed. */
    val paywallIsVisible: Flow<Boolean> =
        revenueRepository.isBillingFlowInProgress

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = detectionRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Idle)

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


    fun getScenarioId(): Long? =
        scenarioDbId.value

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

        detectionRepository.stopDetection()
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
            detectionRepository.startDetection(
                context,
                debugRepository.getDebugDetectionListenerIfNeeded(context),
                revenueRepository.consumeTrial(),
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

    fun shouldDownloadTextLanguageFiles(): Boolean =
        isLanguageFileDownloadRequired.value

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        detectionState.value == UiState.Idle && tutorialRepository.shouldShowStopWithVolumeDownTutorialDialog()

    fun setStopWithVolumeDownDontShowAgain(): Unit =
        tutorialRepository.setStopWithVolumeDownDontShowAgain()

    private fun UserBillingState.isAdRequested(): Boolean =
        this == UserBillingState.AD_REQUESTED
}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

private fun List<Event>.getAllTextLanguages() : Set<TrainedTextLanguage> =
    buildSet {
        this@getAllTextLanguages.forEach { event ->
            event.conditions.forEach { condition ->
                if (condition is TextCondition) add(condition.textLanguage)
            }
        }
    }

private const val TAG = "MainMenuViewModel"