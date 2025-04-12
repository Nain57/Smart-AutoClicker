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

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TryElementViewModel @Inject constructor(
    private val detectionRepository: DetectionRepository
) : ViewModel() {

    private val triedElement: MutableStateFlow<TriedImageEvent?> = MutableStateFlow(null)

    private val isPlaying: StateFlow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.DETECTING }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    private val tryResults: MutableStateFlow<List<DetectionResultInfo>> = MutableStateFlow(emptyList())
    private val detectionResults: Flow<List<DetectionResultInfo>> = tryResults
        .combine(isPlaying) { results, playing -> if (playing) results else emptyList() }
        .transformLatest { results ->
            if (results.isEmpty()) return@transformLatest

            emit(results)
            delay(3.seconds)
            tryResults.emit(emptyList())
        }

    val displayResults: Flow<ImageEventResultsDisplay?> = triedElement.combine(detectionResults) { element, results ->
        if (element == null || results.isEmpty()) return@combine null

        val text =
            if (element.imageEvent.conditions.size == 1) {
                results.first().confidenceRate.formatConfidenceRate()
            } else {
                val detected = results.fold(0) { acc, detectionResultInfo ->
                    acc + (if (detectionResultInfo.positive) 1 else 0)
                }
                "$detected/${element.imageEvent.conditions.size}"
            }


        ImageEventResultsDisplay(text, results)
    }

    fun setTriedElement(scenario: Scenario, imageEvent: ImageEvent) {
        viewModelScope.launch {
            triedElement.emit(TriedImageEvent(scenario, imageEvent))
        }
    }

    fun startTry(context: Context) {
        viewModelScope.launch {
            if (isPlaying.value) return@launch

            delay(500)
            triedElement.value?.let { element ->
                detectionRepository.tryEvent(context, element.scenario, element.imageEvent) { results ->
                    tryResults.value = results.getAllResults().mapNotNull { result ->
                        if (result is ScreenConditionResult) result.toDetectionResultInfo()
                        else null
                    }
                }
            }
        }
    }

    fun stopTry() {
        if (!isPlaying.value) return

        viewModelScope.launch {
            detectionRepository.stopDetection()
        }
    }

    private fun ScreenConditionResult.toDetectionResultInfo(): DetectionResultInfo {
        val halfWidth = condition.captureArea.width() / 2
        val halfHeight = condition.captureArea.height() / 2

        return DetectionResultInfo(
            positive = isFulfilled,
            coordinates =
                if (position.x == 0 && position.y == 0) Rect()
                else Rect(
                    position.x - halfWidth,
                    position.y - halfHeight,
                    position.x + halfWidth,
                    position.y + halfHeight,
                ),
            confidenceRate = confidenceRate,
        )
    }
}

data class ImageEventResultsDisplay(
    val resultText: String,
    val detectionResults: List<DetectionResultInfo>,
)

private data class TriedImageEvent(
    val scenario: Scenario,
    val imageEvent: ImageEvent,
)