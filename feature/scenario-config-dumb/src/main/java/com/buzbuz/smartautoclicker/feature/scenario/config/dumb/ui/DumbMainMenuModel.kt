/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui

import android.app.Application
import android.graphics.Point

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain.DumbEditionRepository
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DumbMainMenuModel(application: Application) : AndroidViewModel(application) {

    private val dumbRepository: DumbRepository = DumbRepository.getRepository(application)
    private val dumbEditionRepository = DumbEditionRepository.getInstance(application)

    val isPlaying: StateFlow<Boolean> = dumbRepository.isEngineRunning

    fun startEdition(dumbScenarioId: Identifier) {
        viewModelScope.launch {
            dumbEditionRepository.startEdition(dumbScenarioId.databaseId)
        }
    }

    fun stopEdition() {
        dumbEditionRepository.stopEdition()
    }

    fun createNewDumbClick(position: Point): DumbAction.DumbClick =
        dumbEditionRepository.dumbActionBuilder.createNewDumbClick(getApplication(), position)

    fun createNewDumbSwipe(from: Point, to: Point): DumbAction.DumbSwipe =
        dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(getApplication(), from, to)

    fun createNewDumbPause(): DumbAction.DumbPause =
        dumbEditionRepository.dumbActionBuilder.createNewDumbPause(getApplication())

    fun addNewDumbAction(dumbAction: DumbAction) {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.apply {
                addNewDumbAction(dumbAction)
                saveEditions()
            }
        }
    }

    fun updateDumbScenario(dumbScenario: DumbScenario) {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.apply {
                updateDumbScenario(dumbScenario)
                saveEditions()
            }
        }
    }

    fun toggleScenarioPlay() {
        dumbEditionRepository.editedDumbScenario.value?.let { dumbScenario ->
            viewModelScope.launch {
                if (isPlaying.value) dumbRepository.stopDumbScenarioExecution()
                else dumbRepository.startDumbScenarioExecution(dumbScenario)
            }
        }
    }
}