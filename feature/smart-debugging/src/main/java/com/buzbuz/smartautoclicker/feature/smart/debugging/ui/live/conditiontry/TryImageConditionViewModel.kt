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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.live.conditiontry

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebugDetectionResultUseCase
import com.buzbuz.smartautoclicker.core.smart.debugging.utils.formatDebugConfidenceRate
import com.buzbuz.smartautoclicker.feature.smart.debugging.uistate.ImageConditionResultUiState
import com.buzbuz.smartautoclicker.feature.smart.debugging.uistate.mapping.toUiState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TryImageConditionViewModel @Inject constructor(
    detectionResultUseCase: DebugDetectionResultUseCase,
    private val detectionRepository: DetectionRepository,
) : ViewModel() {

    private val isPlaying: Flow<Boolean> = detectionRepository.detectionState
        .map { state -> state == DetectionState.DETECTING }
        .distinctUntilChanged()

    private val userThreshold: MutableStateFlow<Int> = MutableStateFlow(0)

    private val detectionResult: Flow<ImageConditionResultUiState?> = detectionResultUseCase()
        .combine(isPlaying) { results, playing -> if (playing) results else null }
        .map { results ->
            if (results == null || results.imageConditionsResults.isEmpty()) null
            else results.imageConditionsResults.first().toUiState()
        }

    val displayResults: Flow<ImageConditionResultUiState?> =
        combine(userThreshold, detectionResult) { userThreshold, result ->
            result?.copy(positive = (1.0 - (userThreshold / 100.0)) < result.confidenceRate)
        }

    val thresholdText: Flow<String> =
        userThreshold.map { threshold -> (1 - (threshold / 100.0)).formatDebugConfidenceRate() }


    fun setThreshold(newThreshold: Int) {
        viewModelScope.launch {
            userThreshold.value = newThreshold
        }
    }

    fun startTry(context: Context, scenario: Scenario, imageCondition: ImageCondition) {
        viewModelScope.launch {
            userThreshold.value = imageCondition.threshold

            delay(500)
            detectionRepository.tryImageCondition(context, scenario, imageCondition)
        }
    }

    fun stopTry() {
        viewModelScope.launch {
            detectionRepository.stopDetection()
        }
    }

    fun getSelectedThreshold(): Int = userThreshold.value
}

/** The minimum threshold value selectable by the user. */
internal const val MIN_THRESHOLD = 0f
/** The maximum threshold value selectable by the user. */
internal const val MAX_THRESHOLD = 20f