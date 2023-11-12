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
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain.DumbEditionRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DumbMainMenuModel(application: Application) : AndroidViewModel(application) {

    private val dumbEditionRepository = DumbEditionRepository.getInstance(application)
    private val dumbEngine = DumbEngine.getInstance(application)

    /** The repository for the tutorials data. */
    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository(application)

    val canPlay: Flow<Boolean> =
        combine(dumbEditionRepository.isEditionSynchronized, dumbEngine.dumbScenario) { isSync, scenario ->
            isSync && scenario?.isValid() ?: false
        }
    val isPlaying: StateFlow<Boolean> =
        dumbEngine.isRunning

    fun startEdition(dumbScenarioId: Identifier, onStarted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dumbEditionRepository.startEdition(dumbScenarioId.databaseId)) {
                withContext(Dispatchers.Main) {
                    onStarted()
                }
            }
        }
    }

    fun saveEditions() {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.apply {
                saveEditions()
            }
        }
    }

    fun stopEdition() {
        dumbEditionRepository.stopEdition()
    }

    fun toggleScenarioPlay() {
        viewModelScope.launch {
            if (isPlaying.value) dumbEngine.stopDumbScenario()
            else dumbEngine.startDumbScenario()
        }
    }

    fun stopScenarioPlay(): Boolean {
        if (!isPlaying.value) return false

        viewModelScope.launch {
            dumbEngine.stopDumbScenario()
        }
        return true
    }

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        !tutorialRepository.isTutorialStopVolumeDownPopupShown()

    fun onStopVolumeDownTutorialDialogShown(): Unit =
        tutorialRepository.setIsTutorialStopVolumeDownPopupShown()
}