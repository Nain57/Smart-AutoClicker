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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private val userComputeRateUnit: MutableStateFlow<ComputeRateUnitDropdownItem?> =
        MutableStateFlow(editionRepository.editionState.getScenario()?.getInitialComputeRateUnitItem())

    private var cachedComputeRate: Double = editionRepository.editionState.getScenario()?.computeRate?.takeIf { it > 0.0 } ?: FRAME_LIMIT_DEFAULT_VALUE

    val uiState: StateFlow<ScenarioConfigUiState?> = configuredScenario
        .combine(userComputeRateUnit) { scenario, userUnit ->
            scenario.toUiState(
                context = context,
                displaySize = displayConfigManager.displayConfig.sizePx,
                computeRateUnit = userUnit,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


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
                    scenario.copy(computeRate = if (scenario.computeRate != 0.0) 0.0 else cachedComputeRate)
                )
            }
        }
    }

    fun setComputeRateUnit(unit: ComputeRateUnitDropdownItem) {
        userComputeRateUnit.update { unit }
    }

    fun setComputeRate(value: Double) {
        if (value <= FRAME_LIMIT_MIN_VALUE) return

        val unit = userComputeRateUnit.value ?: ComputeRateUnitDropdownItem.Second
        val newValue =
            if (unit is ComputeRateUnitDropdownItem.Minute) value / 60
            else value

        if (newValue > FRAME_LIMIT_MAX_VALUE) return

        cachedComputeRate = newValue

        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(computeRate = newValue))
            }
        }
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
}


private fun Scenario.toUiState(
    context: Context,
    displaySize: Point,
    computeRateUnit: ComputeRateUnitDropdownItem?,
): ScenarioConfigUiState =
    ScenarioConfigUiState(
        name = name,
        randomizeChecked = randomize,
        keepScreenOnChecked = keepScreenOn,
        computeRateState = toComputeRateUiState(computeRateUnit),
        qualityUiState = toDetectionQualityUiState(context, displaySize),
    )

private fun Scenario.toComputeRateUiState(userUnit: ComputeRateUnitDropdownItem?): ComputeRateLimitUiState {
    val unit = userUnit ?: ComputeRateUnitDropdownItem.Second
    return when (unit) {
        ComputeRateUnitDropdownItem.Second -> ComputeRateLimitUiState(
            isEnabled = computeRate > 0.0,
            unit = unit,
            maxValue = FRAME_LIMIT_MAX_VALUE,
            value = computeRate,
        )

        ComputeRateUnitDropdownItem.Minute -> ComputeRateLimitUiState(
            isEnabled = computeRate > 0.0,
            unit = unit,
            maxValue = FRAME_LIMIT_MAX_VALUE * 60.0,
            value = computeRate * 60,
        )
    }
}

private fun Scenario.toDetectionQualityUiState(context: Context, displaySize: Point): DetectionQualityUiState {
    val maxVal = maxOf(displaySize.x, displaySize.y, 1).toFloat()
    val minVal = minOf(displaySize.x, displaySize.y).toFloat()
    val quality = detectionQuality.toFloat().coerceIn(DETECTION_QUALITY_MIN.toFloat(), maxVal)

    return DetectionQualityUiState(
        displayText = context.getString(
            R.string.field_scenario_quality_resolution,
            quality.toInt(),
            (minVal * (quality / maxVal)).toInt(),
        ),
        qualityValue = quality,
        min = DETECTION_QUALITY_MIN.toFloat(),
        max = maxVal,
    )
}

private fun Scenario.getInitialComputeRateUnitItem(): ComputeRateUnitDropdownItem =
     if (computeRate == 0.0|| computeRate >= 1.0) ComputeRateUnitDropdownItem.Second
     else ComputeRateUnitDropdownItem.Minute


private fun DisplayConfigManager.getMaxDetectionQuality(): Int =
    maxOf(displayConfig.sizePx.x, displayConfig.sizePx.y, 1)

internal const val FRAME_LIMIT_DEFAULT_VALUE = 60.0
internal const val FRAME_LIMIT_MIN_VALUE = 0.0
internal const val FRAME_LIMIT_MAX_VALUE = 1000.0
