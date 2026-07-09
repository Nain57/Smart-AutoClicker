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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialSubjectState
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimingGameViewModel @Inject constructor(
    private val tutorialRepository: TutorialRepository,
    private val smartProcessingRepository: SmartProcessingRepository,
) : ViewModel() {

    private val startedState: Flow<TutorialState.Started> = tutorialRepository.tutorialState
        .filterIsInstance<TutorialState.Started>()

    private val timingGameState: Flow<TutorialSubjectState.TimingGame> = tutorialRepository.tutorialSubjectController
        .flatMapLatest { controller -> controller?.state ?: emptyFlow() }
        .filterIsInstance<TutorialSubjectState.TimingGame>()

    @OptIn(FlowPreview::class)
    val shouldStopGame: Flow<Boolean> = smartProcessingRepository.scenarioId
        .map { id -> id == null }
        .debounce(1.seconds)

    val shouldDisplayStepOverlay: Flow<Boolean> = startedState
        .map { state ->
            state.currentStep is TutorialStep.TutorialOverlay && state.isCurrentStepStarted && !state.isCompleted
        }

    val shouldDisplayFloatingUi: StateFlow<Boolean> = startedState
        .mapNotNull { state ->
            val step = state.currentStep
            if (state.isCurrentStepStarted && step is TutorialStep.ChangeFloatingUiVisibility) step.newVisibility
            else null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), true)

    val shouldDisplayCompletionDialog: Flow<Boolean> = startedState
        .combine(timingGameState) { tutorialState, game ->
            val endStep = tutorialState.currentStep as? TutorialStep.EndStep ?: return@combine false
            endStep.completed && game.isWon == true
        }

    val uiState: StateFlow<TimingGameUiState?> = timingGameState
        .map { game -> game.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)

    fun resetGame() {
        getGameController()?.stop()
    }

    fun onTimingButtonHit() {
        val controller = getGameController() ?: return
        if (uiState.value?.isStarted == false) controller.start()
        else controller.onGameTimingButtonHit()
    }

    fun stopDetection() {
        smartProcessingRepository.stopDetection()
    }

    fun stopTutorial() {
        tutorialRepository.stopTutorial()
    }

    private fun getGameController(): TutorialSubjectController.TimingGame? =
        tutorialRepository.tutorialSubjectController.value as? TutorialSubjectController.TimingGame

    private fun TutorialSubjectState.TimingGame.toUiState(): TimingGameUiState =
        TimingGameUiState(
            instructionsResId = subject.instructionsResId,
            isStarted = isStarted,
            isWon = isWon,
            clickCount = clickCount,
            targetClickCount = subject.clickCount,
            targetTotalDiffMs = subject.targetTotalDiffMs,
            cumulativeTimeDiffMs = cumulativeTimeDiffMs,
            lastTimeDiffMs = lastTimeDiffMs,
        )
}
