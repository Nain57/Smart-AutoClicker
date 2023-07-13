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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game

import android.app.Application
import android.graphics.PointF
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGame
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialGameViewModel(application: Application) : AndroidViewModel(application) {

    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository(application)

    val currentGame: StateFlow<TutorialGame?> = tutorialRepository.activeTutorial
        .map { it?.game }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(3_000),
            null,
        )

    val shouldDisplayStepOverlay: Flow<Boolean> = tutorialRepository.tutorialOverlayState
        .map { it != null }

    private val isStarted: Flow<Boolean> = currentGame
        .flatMapLatest { it?.state?.map { it.isStarted } ?: flowOf(false) }
        .distinctUntilChanged()

    val gameTimerValue: Flow<Int> = currentGame
        .flatMapLatest { it?.state?.map { it.timeLeft } ?: flowOf(0) }
        .distinctUntilChanged()

    val gameScore: Flow<Int> = currentGame
        .flatMapLatest { it?.state?.map { it.score } ?: flowOf(0) }
        .distinctUntilChanged()

    val gameTargets: Flow<Map<TutorialGameTargetType, PointF>> = currentGame
        .flatMapLatest { it?.targets ?: flowOf(emptyMap()) }
        .distinctUntilChanged()

    val playRetryBtnState: Flow<PlayRetryButtonState> =
        combine(currentGame, isStarted, gameScore) { game, started, score ->
            when {
                game == null || started -> PlayRetryButtonState.GONE
                score == 0 -> PlayRetryButtonState.PLAY
                else -> PlayRetryButtonState.RETRY
            }
        }

    val nextGameBtnVisibility: Flow<Boolean> =
        combine(currentGame, isStarted, gameScore) { game, started, score ->
            if (game == null) return@combine false
            !started && score >= game.highScore
        }

    fun selectGame(gameIndex: Int) {
        tutorialRepository.startTutorial(gameIndex)
    }

    fun startGame(area: Rect, targetsSize: Int) {
        tutorialRepository.startGame(viewModelScope, area, targetsSize)
    }

    fun onTargetHit(color: TutorialGameTargetType) {
        tutorialRepository.onGameTargetHit(color)
    }

    fun stopTutorial() {
        tutorialRepository.stopTutorial()
    }
}

enum class PlayRetryButtonState {
    GONE,
    PLAY,
    RETRY,
}