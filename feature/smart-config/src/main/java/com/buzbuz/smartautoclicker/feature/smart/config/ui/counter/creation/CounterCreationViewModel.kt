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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import javax.inject.Inject


class CountersCreationViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val name: MutableStateFlow<String?> = MutableStateFlow("")
    private val startingValue: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val uiState: StateFlow<CounterCreationUiState?> = name
        .map { counterName -> toUiState(counterName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setName(counterName: String) {
        name.update { counterName }
    }

    fun setStartingValue(value: Double) {
        startingValue.update { value }
    }

    fun createCounter() {
        val scenarioId = editionRepository.editionState.getScenario()?.id ?: return
        val counterName = name.value ?: return
        val startingValue = startingValue.value

        if (editionRepository.editionState.getCounter(counterName) != null) return
        editionRepository.addNewCounter(
            Counter(
                counterName = counterName,
                defaultValue = startingValue,
                scenarioId = scenarioId,
            )
        )
    }

    private fun toUiState(name: String?): CounterCreationUiState {
        val nameIsValid = name?.isNotBlank() == true
        val isAlreadyDefined = nameIsValid && editionRepository.editionState.getCounter(name) != null

        return CounterCreationUiState(
            canBeSaved = nameIsValid && !isAlreadyDefined,
            nameError = isAlreadyDefined,
        )
    }
}
