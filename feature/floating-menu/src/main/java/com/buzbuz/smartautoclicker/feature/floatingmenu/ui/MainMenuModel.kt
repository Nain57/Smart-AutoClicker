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

    /** The detection repository. */
    private val detectionRepository: DetectionRepository = DetectionRepository.getDetectionRepository(application)
    /** The repository for the scenarios. */
    private val repository: Repository = Repository.getRepository(application)
    /** The currently loaded scenario info. */
    private val editionRepository: EditionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository: BillingRepository = IBillingRepository.getRepository(application.applicationContext)
    /** The repository for the scenario debugging info. */
    private val debugRepository: DebuggingRepository = DebuggingRepository.getDebuggingRepository(application)

    /** Tells if the pro mode is purchased. */
    private val isProModePurchased: StateFlow<Boolean> = billingRepository.isProModePurchased
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false,
        )

    private val scenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)

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

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        autoStopJob?.cancel()
        autoStopJob = null

        when (detectionState.value) {
            UiState.Detecting -> detectionRepository.stopDetection()
            UiState.Idle -> startDetection(context, onStoppedByLimitation)
        }
    }

    private fun startDetection(context: Context, onStoppedByLimitation: () -> Unit) {
        viewModelScope.launch {
            detectionRepository.startDetection(debugRepository.detectionProgressListener)
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

    fun setConfiguredScenario(scenarioId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            scenarioDbId.value = scenarioId
        }
    }

    fun startScenarioEdition() {
        scenarioDbId.value?.let { scenarioDatabaseId ->
            viewModelScope.launch(Dispatchers.IO) {
                editionRepository.startEdition(scenarioDatabaseId)
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

    override fun onCleared() {
        super.onCleared()
        repository.cleanCache()
    }
}

sealed class UiState {
    object Detecting: UiState()
    object Idle: UiState()
}