/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.floatingmenu.ui

import android.app.Application
import android.content.Context
import android.view.View

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.billing.domain.BillingRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

/**
 * View model for the [MainMenu].
 * @param application the Android application.
 */
class MainMenuModel(application: Application) : AndroidViewModel(application) {

    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** The repository for the scenarios. */
    private val repository: Repository = Repository.getRepository(application)
    /** The detection repository. */
    private val detectionRepository: DetectionRepository = DetectionRepository.getDetectionRepository(application)

    /** The currently loaded scenario info. */
    private val editionRepository: EditionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository: BillingRepository = IBillingRepository.getRepository(application.applicationContext)
    /** The repository for the scenario debugging info. */
    private val debugRepository: DebuggingRepository = DebuggingRepository.getDebuggingRepository(application)
    /** The repository for the tutorials data. */
    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository(application)

    /** Tells if the pro mode is purchased. */
    private val isProModePurchased: StateFlow<Boolean> = billingRepository.isProModePurchased
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false,
        )

    private val scenarioDbId: StateFlow<Long?> = detectionRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /** Coroutine Job stopping the detection automatically if user is not in pro mode. */
    private var autoStopJob: Job? = null

    val isBillingFlowInProgress: Flow<Boolean> = billingRepository.isBillingFlowInProcess

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

    /** Tells if the . */
    val nativeLibError: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        when (detectionState.value) {
            UiState.Detecting -> stopDetection()
            UiState.Idle -> startDetection(context, onStoppedByLimitation)
        }
    }

    /** Stop the detection. Returns true if it was started, false if not. */
    fun stopDetection(): Boolean {
        if (detectionState.value !is UiState.Detecting) return false

        autoStopJob?.cancel()
        autoStopJob = null

        detectionRepository.stopDetection()
        return true
    }

    private fun startDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        viewModelScope.launch {
            detectionRepository.startDetection(context, debugRepository.detectionProgressListener)
        }

        if (!isProModePurchased.value) {
            autoStopJob = viewModelScope.launch {
                delay(ProModeAdvantage.Limitation.DETECTION_DURATION_MINUTES_LIMIT.limit.minutes.inWholeMilliseconds)

                detectionRepository.stopDetection()
                onStoppedByLimitation()
                billingRepository.startBillingActivity(
                    context,
                    ProModeAdvantage.Limitation.DETECTION_DURATION_MINUTES_LIMIT,
                )
            }
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

    override fun onCleared() {
        super.onCleared()
        repository.cleanCache()
    }
}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}