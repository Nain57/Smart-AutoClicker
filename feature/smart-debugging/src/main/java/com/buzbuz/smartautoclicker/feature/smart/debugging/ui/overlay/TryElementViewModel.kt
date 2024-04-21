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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.content.Context
import android.graphics.Rect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.core.processing.domain.ImageConditionResult
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.formatConfidenceRate

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class TryElementViewModel @Inject constructor(
    private val detectionRepository: DetectionRepository
) : ViewModel() {

    private val triedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    private var resetJob: Job? = null

    private val isPlaying: StateFlow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.DETECTING }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    private val _detectionResults: MutableStateFlow<List<DetectionResultInfo>> = MutableStateFlow(emptyList())
    private val detectionResults: Flow<List<DetectionResultInfo>> = _detectionResults
        .combine(isPlaying) { results, playing -> if (playing) results else emptyList() }
        .onEach { results ->
            resetJob?.cancel()
            resetJob = null
            if (results.isEmpty()) return@onEach

            resetJob = viewModelScope.launch {
                delay(3.seconds)
                _detectionResults.emit(emptyList())
            }
        }

    val displayResults: Flow<ResultsDisplay?> = triedElement.combine(detectionResults) { element, results ->
        if (element == null || results.isEmpty()) return@combine null

        val text = when (element) {
            is Element.ImageEventTry -> {
                if (element.imageEvent.conditions.size == 1) {
                    results.first().confidenceRate.formatConfidenceRate()
                } else {
                    val detected = results.fold(0) { acc, detectionResultInfo ->
                        acc + (if (detectionResultInfo.positive) 1 else 0)
                    }
                    "$detected/${element.imageEvent.conditions.size}"
                }
            }

            is Element.ImageConditionTry -> results.first().confidenceRate.formatConfidenceRate()
        }

        ResultsDisplay(text, results)
    }

    fun setTriedElement(scenario: Scenario, element: Any) {
        viewModelScope.launch {
            triedElement.emit(
                when (element) {
                    is ImageEvent -> Element.ImageEventTry(scenario, element)
                    is ImageCondition -> Element.ImageConditionTry(scenario, element)
                    else -> throw UnsupportedOperationException("Can't try $element")
                }
            )
        }
    }

    fun startTry(context: Context) {
        if (isPlaying.value) return

        viewModelScope.launch {
            triedElement.value?.let { element ->

                when (element) {
                    is Element.ImageEventTry ->
                        detectionRepository.tryEvent(context, element.scenario, element.imageEvent) { results ->
                            _detectionResults.value = results.getAllResults().mapNotNull { result ->
                                if (result is ImageConditionResult) result.toDetectionResultInfo()
                                else null
                            }
                        }

                    is Element.ImageConditionTry ->
                        detectionRepository.tryImageCondition(context, element.scenario, element.imageCondition) { result ->
                            _detectionResults.value = listOf(result.toDetectionResultInfo())
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

    private fun ImageConditionResult.toDetectionResultInfo(): DetectionResultInfo {
        val halfWidth = condition.area.width() / 2
        val halfHeight = condition.area.height() / 2

        return DetectionResultInfo(
            positive = haveBeenDetected,
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

data class ResultsDisplay(
    val resultText: String,
    val detectionResults: List<DetectionResultInfo>,
)

private sealed class Element {

    abstract val scenario: Scenario

    data class ImageEventTry(
        override val scenario: Scenario,
        val imageEvent: ImageEvent,
    ) : Element()

    data class ImageConditionTry(
        override val scenario: Scenario,
        val imageCondition: ImageCondition,
    ) : Element()
}