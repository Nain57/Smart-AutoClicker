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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class DumbScenarioConfigViewModel @Inject constructor(
    private val dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val userModifications: StateFlow<DumbScenario?> =
        dumbEditionRepository.editedDumbScenario

    val canBeSaved: Flow<Boolean> = userModifications.map { dumbScenario ->
        dumbScenario?.isValid() == true
    }

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> =  userModifications
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the scenario name is valid or not. */
    val scenarioNameError: Flow<Boolean> = userModifications
        .map { it?.name?.isEmpty() == true }

    /** The number of times to repeat the scenario. */
    val repeatCount: Flow<String> = userModifications
        .filterNotNull()
        .map { it.repeatCount.toString() }
        .take(1)
    /** Tells if the repeat count value is valid or not. */
    val repeatCountError: Flow<Boolean> = userModifications
        .map { it == null || it.repeatCount <= 0 }
    /** Tells if the scenario should be repeated infinitely. */
    val repeatInfiniteState: Flow<Boolean> = userModifications
        .map { it == null || it.isRepeatInfinite }

    /** The maximum duration of the execution in minutes. */
    val maxDurationMin: Flow<String> = userModifications
        .filterNotNull()
        .map { it.maxDurationMin.toString() }
        .take(1)
    /** Tells if the repeat count value is valid or not. */
    val maxDurationMinError: Flow<Boolean> = userModifications
        .map { it == null || it.maxDurationMin <= 0 }
    /** Tells if there is no maximum duration. */
    val maxDurationMinInfiniteState: Flow<Boolean> = userModifications
        .map { it == null || it.isDurationInfinite }

    /** The randomization value for the scenario. */
    val randomization: Flow<Boolean> = userModifications
        .map { it?.randomize == true }

    fun setDumbScenarioName(name: String) {
        userModifications.value?.copy(name = name)?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun setRepeatCount(repeatCount: Int) {
        userModifications.value?.copy(repeatCount = repeatCount)?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun toggleInfiniteRepeat() {
        val currentValue = userModifications.value?.isRepeatInfinite ?: return
        userModifications.value?.copy(isRepeatInfinite = !currentValue)?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun setMaxDurationMinutes(durationMinutes: Int) {
        userModifications.value?.copy(maxDurationMin = durationMinutes)?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun toggleInfiniteMaxDuration() {
        val currentValue = userModifications.value?.isDurationInfinite ?: return
        userModifications.value?.copy(isDurationInfinite = !currentValue)?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun toggleRandomization() {
        userModifications.value?.let { scenario ->
            dumbEditionRepository.updateDumbScenario(scenario.copy(randomize = !scenario.randomize))
        }
    }
}