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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialOverlayViewModel @Inject constructor(
    private val monitoredViewsManager: MonitoredViewsManager,
    private val tutorialRepository: TutorialRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    private val tutorialOverlayStep: Flow<TutorialStep.TutorialOverlay?> = tutorialRepository.tutorialState
        .map { state ->
            if (state !is TutorialState.Started) return@map null
            state.currentStep as? TutorialStep.TutorialOverlay
        }

    val uiState: StateFlow<TutorialFullscreenUiState?> = tutorialOverlayStep
        .flatMapLatest { step ->
            step ?: return@flatMapLatest flowOf(null)

            when (val endCondition = step.stepEndCondition) {
                TutorialStepEndCondition.NextButton -> flowOf(
                    TutorialFullscreenUiState(
                        instructionsResId = step.contentTextResId,
                        image = step.getImage(),
                        exitButton = TutorialExitButtonUiState.Next,
                    )
                )

                is TutorialStepEndCondition.MonitoredViewClicked -> {
                    monitoredViewsManager.getViewPosition(endCondition.type)?.map { position ->
                        TutorialFullscreenUiState(
                            instructionsResId = step.contentTextResId,
                            image = step.getImage(),
                            exitButton = TutorialExitButtonUiState.MonitoredView(endCondition.type, position),
                            isDisplayedInTopHalf = position.centerY() > displayConfigManager.displayConfig.sizePx.y / 2,
                        )
                    } ?: flowOf(TutorialFullscreenUiState(step.contentTextResId))
                }

                TutorialStepEndCondition.Immediate -> flowOf(null)
                TutorialStepEndCondition.OverlayStackVisibilityChanged -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)

    fun performClickOnMonitoredView() {
        val viewType = (uiState.value?.exitButton as? TutorialExitButtonUiState.MonitoredView)?.type ?: return
        monitoredViewsManager.performClick(viewType)
    }

    fun toNextTutorialStep() {
        tutorialRepository.nextTutorialStep()
    }

    fun toLastTutorialStep() {
        tutorialRepository.skipToLastTutorialStep()
    }
}

private fun TutorialStep.TutorialOverlay.getImage() : TutorialStepImageUiState? =
    image?.let { TutorialStepImageUiState(it.imageResId, it.imageDescResId) }
