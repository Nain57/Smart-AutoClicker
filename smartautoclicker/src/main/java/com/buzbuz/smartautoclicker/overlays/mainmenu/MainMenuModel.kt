/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.mainmenu

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.billing.IBillingRepository
import com.buzbuz.smartautoclicker.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.billing.model.BillingRepository
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.engine.DetectorEngine
import com.buzbuz.smartautoclicker.engine.DetectorState
import com.buzbuz.smartautoclicker.overlays.base.utils.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.overlays.base.utils.getIsDebugReportEnabled
import com.buzbuz.smartautoclicker.overlays.base.utils.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * View model for the [MainMenu].
 * @param application the Android application.
 */
class MainMenuModel(application: Application) : AndroidViewModel(application) {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getDebugConfigPreferences()
    /** The detector engine. */
    private val detectorEngine: DetectorEngine = DetectorEngine.getDetectorEngine(application)
    /** The repository for the scenarios. */
    private val repository: Repository = Repository.getRepository(application)
    /** The currently loaded scenario info. */
    private val editionRepository: EditionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository: BillingRepository = IBillingRepository.getRepository(application.applicationContext)

    /** Coroutine Job stopping the detection automatically if user is not in pro mode. */
    private var autoStopJob: Job? = null

    val isBillingFlowInProgress: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** The current of the detection. */
    val detectionState: Flow<UiState> = detectorEngine.state
        .map { if (it == DetectorState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    val canStartScenario: Flow<Boolean> = detectorEngine.canStartDetection
        .combine(editionRepository.isEditionSynchronized) { canStartDetection, isSynchronized ->
            canStartDetection && isSynchronized
        }

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        autoStopJob?.cancel()

        when (detectorEngine.state.value) {
            DetectorState.DETECTING -> detectorEngine.stopDetection()
            DetectorState.RECORDING -> startDetection(context, onStoppedByLimitation)
            else -> { /* Nothing to do */ }
        }
    }

    private fun startDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        detectorEngine.startDetection(
            sharedPreferences.getIsDebugViewEnabled(getApplication<Application>()),
            sharedPreferences.getIsDebugReportEnabled(getApplication<Application>()),
        )

        if (!billingRepository.isProModePurchased.value) {
            autoStopJob = viewModelScope.launch {
                delay(ProModeAdvantage.Limitation.DETECTION_DURATION_MINUTES_LIMIT.limit.milliseconds)

                detectorEngine.stopDetection()
                onStoppedByLimitation()
                billingRepository.startBillingActivity(
                    context,
                    ProModeAdvantage.Limitation.DETECTION_DURATION_MINUTES_LIMIT,
                )
            }
        }
    }

    fun setConfiguredScenario(scenarioId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.setConfiguredScenario(scenarioId)
        }
    }

    fun startScenarioEdition() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.startEditions()
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.saveEditions()
        }
    }

    /** Cancel all changes made by the user. */
    fun cancelScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.cancelEditions()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanCache()
    }
}

sealed class UiState {
    object Detecting: UiState()
    object Idle: UiState()
}