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

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
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

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val editionRepository: EditionRepository,
    private val tutorialRepository: TutorialRepository,
    private val revenueRepository: IRevenueRepository,
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

    /** Tells if the paywall is currently displayed. */
    val paywallIsVisible: Flow<Boolean> =
        revenueRepository.isBillingFlowInProgress

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = detectionRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState.Idle,
        )

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    val canStartScenario: Flow<Boolean> = detectionRepository.canStartDetection
        .combine(editionRepository.isEditionSynchronized) { canStartDetection, isSynchronized ->
            canStartDetection && isSynchronized
        }

    /** Tells if the detector can't work due to a native library load error. */
    val nativeLibError: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

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
                debugRepository.detectionProgressListener,
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

    fun monitorPlayPauseButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.FLOATING_MENU_BUTTON_PLAY, view, ViewPositioningType.SCREEN)
    }

    fun monitorConfigButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.FLOATING_MENU_BUTTON_CONFIG, view, ViewPositioningType.SCREEN)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.FLOATING_MENU_BUTTON_PLAY)
            detach(MonitoredViewType.FLOATING_MENU_BUTTON_CONFIG)
        }
    }

    fun shouldShowFirstTimeTutorialDialog(): Boolean =
        !tutorialRepository.isTutorialFirstTimePopupShown()

    fun onFirstTimeTutorialDialogShown(): Unit =
        tutorialRepository.setIsTutorialFirstTimePopupShown()

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        !tutorialRepository.isTutorialStopVolumeDownPopupShown()

    fun onStopVolumeDownTutorialDialogShown(): Unit =
        tutorialRepository.setIsTutorialStopVolumeDownPopupShown()

    private fun UserBillingState.isAdRequested(): Boolean =
        this == UserBillingState.AD_REQUESTED
}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

private const val TAG = "MainMenuViewModel"