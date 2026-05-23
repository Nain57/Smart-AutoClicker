/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.switcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject

class ScenarioSwitchViewModel @Inject constructor(
    smartRepository: IRepository,
    smartProcessingRepository: SmartProcessingRepository,
) : ViewModel() {

    private val currentScenarioId: StateFlow<Long?> = smartProcessingRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val switchableScenarios: StateFlow<List<Scenario>> = combine(
        smartRepository.scenarios,
        currentScenarioId,
    ) { scenarios, currentId ->
        scenarios.filter { scenario -> scenario.id.databaseId != currentId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
