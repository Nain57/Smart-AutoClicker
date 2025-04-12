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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.content.Context
import android.graphics.Rect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.core.processing.domain.ScreenConditionResult
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.formatConfidenceRate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TryImageConditionViewModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
) : ViewModel() {

    private val triedImageCondition: MutableStateFlow<TriedImageCondition?> = MutableStateFlow(null)
    private val userThreshold: MutableStateFlow<Int> = MutableStateFlow(0)
    private val tryResults: MutableStateFlow<ScreenConditionResult?> = MutableStateFlow(null)

    private val isPlaying: StateFlow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.DETECTING }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)


    private val detectionResults: Flow<DetectionResultInfo> =
        combine(tryResults, userThreshold, isPlaying) { results, threshold, playing ->
            results ?: return@combine null
            if (playing) results.toDetectionResultInfo(threshold) else null
        }.filterNotNull().transformLatest { results ->
            emit(results)
            delay(3.seconds)
            tryResults.emit(null)
        }

    val displayResults: Flow<ImageConditionResultsDisplay?> =
        combine(triedImageCondition, detectionResults) { element, results ->
            if (element == null) return@combine null
            val text = results.confidenceRate.formatConfidenceRate()
            ImageConditionResultsDisplay(text, results)
        }

    val thresholdText: Flow<String> =
        userThreshold.map { threshold -> (1 - (threshold / 100.0)).formatConfidenceRate() }

    fun setImageConditionElement(scenario: Scenario, imageCondition: ImageCondition) {
        viewModelScope.launch {
            triedImageCondition.value = TriedImageCondition(scenario, imageCondition)
            userThreshold.value = imageCondition.threshold
        }
    }

    fun setThreshold(newThreshold: Int) {
        viewModelScope.launch {
            userThreshold.value = newThreshold
        }
    }

    fun startTry(context: Context) {
        viewModelScope.launch {
            if (isPlaying.value) return@launch
            val tryElement = triedImageCondition.value ?: return@launch

            delay(500)
            detectionRepository.tryImageCondition(context, tryElement.scenario, tryElement.imageCondition) { result ->
                tryResults.value = result
            }
        }
    }

    fun stopTry() {
        if (!isPlaying.value) return

        viewModelScope.launch {
            detectionRepository.stopDetection()
        }
    }

    fun getSelectedThreshold(): Int = userThreshold.value

    private fun ScreenConditionResult.toDetectionResultInfo(overriddenThreshold: Int): DetectionResultInfo {
        return DetectionResultInfo(
            positive = (1.0 - (overriddenThreshold / 100.0)) < confidenceRate,
            coordinates = position,
            confidenceRate = confidenceRate,
        )
    }
}

data class ImageConditionResultsDisplay(
    val resultText: String,
    val detectionResults: DetectionResultInfo,
)

private data class TriedImageCondition(
    val scenario: Scenario,
    val imageCondition: ImageCondition,
)

/** The minimum threshold value selectable by the user. */
const val MIN_THRESHOLD = 0f
/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f