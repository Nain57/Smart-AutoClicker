/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortItem
import com.buzbuz.smartautoclicker.core.settings.domain.model.sortedByScenarioSortSettings

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject

data class ScenarioSwitchUiState(
    val currentScenario: Scenario?,
    val alternatives: List<Scenario>,
    val isPaused: Boolean,
    val isLoading: Boolean,
)

class ScenarioSwitchViewModel @Inject constructor(
    smartRepository: IRepository,
    smartProcessingRepository: SmartProcessingRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ScenarioSwitchUiState> = combine(
        smartRepository.scenarios,
        smartProcessingRepository.scenarioId,
        smartProcessingRepository.detectionState,
        settingsRepository.scenarioSortSettings,
    ) { scenarios, currentScenarioId, detectionState, sortSettings ->
        val currentScenario = scenarios.firstOrNull { it.id == currentScenarioId }
        val alternatives = currentScenario?.let {
            scenarios
                .filterNot { scenario -> scenario.id == currentScenarioId }
                .sortedByScenarioSortSettings(sortSettings) { scenario ->
                    ScenarioSortItem(
                        id = scenario.id.databaseId,
                        name = scenario.name,
                        lastStartTimestamp = scenario.stats?.lastStartTimestampMs ?: 0L,
                        startCount = scenario.stats?.startCount ?: 0L,
                    )
                }
        } ?: emptyList()

        ScenarioSwitchUiState(
            currentScenario = currentScenario,
            alternatives = alternatives,
            isPaused = currentScenario != null && detectionState == DetectionState.RECORDING,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ScenarioSwitchUiState(
            currentScenario = null,
            alternatives = emptyList(),
            isPaused = false,
            isLoading = true,
        ),
    )
}
