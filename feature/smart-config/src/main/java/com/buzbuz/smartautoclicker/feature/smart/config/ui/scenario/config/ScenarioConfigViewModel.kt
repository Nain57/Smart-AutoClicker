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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config

import android.content.Context
import android.graphics.Point
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.feature.smart.config.R
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject


/** View model for the [ScenarioConfigContent]. */
class ScenarioConfigViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val displayConfigManager: DisplayConfigManager,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** Currently configured scenario. */
    private val configuredScenario = editionRepository.editionState.scenarioState
        .mapNotNull { it.value }

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> = configuredScenario
        .map { it.name }
        .filterNotNull()
        .take(1)
    /** Tells if the scenario name is valid or not. */
    val scenarioNameError: Flow<Boolean> = configuredScenario
        .map { it.name.isEmpty() }

    /** The randomization value for the scenario. */
    val randomization: Flow<Boolean> = configuredScenario
        .map { it.randomize }

    /** Tells if we should keep the screen awake while running the scenario */
    val keepScreenOn: Flow<Boolean> = configuredScenario
        .map { it.keepScreenOn }

    /** State of the FPS limiter card. */
    val fpsLimit: Flow<ScenarioConfigUiState.FpsLimit> = configuredScenario
        .map { it.toFpsLimitUiState() }

    /** The detection resolution */
    val detectionQuality: Flow<ScenarioConfigUiState.DetectionQuality> = configuredScenario
        .map { scenario ->
            context.getUiDetectionQuality(displayConfigManager.displayConfig.sizePx, scenario.detectionQuality)
        }

    /** Set a new name for the scenario. */
    fun setScenarioName(name: String) {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(name = name))
            }
        }
    }

    /** Toggle the randomization value. */
    fun toggleRandomization() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(randomize = !scenario.randomize))
            }
        }
    }

    /** Toggle the randomization value. */
    fun toggleKeepScreenOn() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(keepScreenOn = !scenario.keepScreenOn))
            }
        }
    }

    fun toggleFpsLimiter() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(
                    scenario.copy(frameLimit = if (scenario.frameLimit != 0) 0 else FRAME_LIMIT_DEFAULT_VALUE.toInt())
                )
            }
        }
    }

    /** @return true if the value was valid and set, false if not. */
    fun setFpsLimit(value: Float): Boolean {
        if (value !in FRAME_LIMIT_MIN_VALUE..FRAME_LIMIT_MAX_VALUE) return false

        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(frameLimit = value.toInt()))
            }
            return true
        }

        return false
    }

    /** Remove one to the detection quality */
    fun decreaseDetectionQuality() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            setDetectionQuality(scenario.detectionQuality - 1)
        }
    }

    /** Add one to the detection quality */
    fun increaseDetectionQuality() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            setDetectionQuality(scenario.detectionQuality + 1)
        }
    }

    /**
     * Set the detection quality for the scenario.
     * @param quality the value from the seekbar.
     */
    fun setDetectionQuality(quality: Int) {
        val maxVal = displayConfigManager.getMaxDetectionQuality()
        val newQuality = quality.coerceIn(DETECTION_QUALITY_MIN.toInt(), maxVal)

        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(detectionQuality = newQuality))
            }
        }
    }

    private fun Scenario.toFpsLimitUiState(): ScenarioConfigUiState.FpsLimit =
        ScenarioConfigUiState.FpsLimit(
            isEnabled = frameLimit != 0,
            value = frameLimit.toFloat(),
            minValue = FRAME_LIMIT_MIN_VALUE,
            maxValue = FRAME_LIMIT_MAX_VALUE,
        )

    private fun Context.getUiDetectionQuality(displaySize: Point, resolution: Int): ScenarioConfigUiState.DetectionQuality {
        val maxVal = maxOf(displaySize.x, displaySize.y, 1).toFloat()
        val minVal = minOf(displaySize.x, displaySize.y).toFloat()
        val quality = resolution.toFloat().coerceIn(DETECTION_QUALITY_MIN.toFloat(), maxVal)

        return ScenarioConfigUiState.DetectionQuality(
            displayText = getString(
                R.string.field_scenario_quality_resolution,
                quality.toInt(),
                (minVal * (quality / maxVal)).toInt(),
            ),
            qualityValue = quality,
            min = DETECTION_QUALITY_MIN.toFloat(),
            max = maxVal,
        )
    }

    private fun DisplayConfigManager.getMaxDetectionQuality(): Int =
        maxOf(displayConfig.sizePx.x, displayConfig.sizePx.y, 1)
}

internal const val FRAME_LIMIT_DEFAULT_VALUE = 60f
internal const val FRAME_LIMIT_MIN_VALUE = 1f
internal const val FRAME_LIMIT_MAX_VALUE = 240f
