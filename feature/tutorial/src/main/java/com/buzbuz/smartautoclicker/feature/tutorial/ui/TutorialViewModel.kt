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
package com.buzbuz.smartautoclicker.feature.tutorial.ui

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialStep

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TutorialViewModel @Inject constructor(
    detectionRepository: DetectionRepository,
    private val tutorialRepository: TutorialRepository
) : ViewModel() {

    val shouldBeStopped: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.INACTIVE }

    val onFloatingUiVisibilityStep: Flow<Boolean> = tutorialRepository.activeStep
        .filterIsInstance<TutorialStep.ChangeFloatingUiVisibility>()
        .map { it.isVisible }

    fun validateFloatingUiVisibilityStep() {
        tutorialRepository.nextTutorialStep()
    }

    fun startTutorialMode(): Unit = tutorialRepository.setupTutorialMode()

    fun stopTutorialMode(): Unit = tutorialRepository.stopTutorialMode()
}