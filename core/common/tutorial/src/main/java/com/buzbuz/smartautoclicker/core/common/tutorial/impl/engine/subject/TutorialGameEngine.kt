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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialSubjectState

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


internal class TutorialGameEngine (
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val game: TutorialSubject.Game,
) : TutorialSubjectController.Game {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var gameJob: Job? = null
    private var onGameCompleted: ((isWon: Boolean) -> Unit)? = null


    private val _state: MutableStateFlow<TutorialSubjectState.Game> = MutableStateFlow(defaultState())
    override val state: StateFlow<TutorialSubjectState.Game> = _state


    override fun startGame() {
        if (gameJob != null) return

        gameJob = coroutineScopeIo.launch {
            Log.d(TAG, "Start game")

            // Init game values
            val initialTargets = game.rules.onStart()
            val gameDuration = game.durationSeconds
            _state.update { old ->
                old.copy(
                    isFinished = false,
                    score = 0,
                    timeLeft = gameDuration,
                    targets = initialTargets,
                )
            }

            // Loop for the total duration of the game, and update the timer one by one
            for (i in gameDuration - 1 downTo  0) {
                delay(1.seconds)

                Log.d(TAG, "onTimerTick $i")

                _state.update { old ->
                    val newTargets = game.rules.onTimerTick(old.targets, i)
                    old.copy(timeLeft = i, targets = newTargets, score = game.rules.getScore())
                }
            }

            // Game is over
            val isWon = game.rules.getScore() > game.scoreToReach
            Log.d(TAG, "Game over, victory=${isWon}")

            _state.update { old ->
                old.copy(
                    isFinished = true,
                    isWon = isWon,
                    timeLeft = 0,
                    targets = emptyMap()
                )
            }
            onGameCompleted?.invoke(isWon)
            onGameCompleted = null
            gameJob = null
        }
    }

    override fun stop() {
        Log.d(TAG, "Stop game")

        gameJob?.cancel()
        gameJob = null

        _state.update { defaultState() }
    }

    override fun onGameTargetHit(target: TutorialGameTargetType) {
        _state.update { old ->
            val newTargets = game.rules.onTargetHit(old.targets, target)
            old.copy(
                targets = newTargets,
                score = game.rules.getScore(),
            )
        }
    }

    fun monitorNextCompletion(listener: ((isWon: Boolean) -> Unit)?) {
        onGameCompleted = listener
    }

    private fun defaultState(): TutorialSubjectState.Game =
        TutorialSubjectState.Game(
            subject = game,
            isFinished = false,
            isWon = null,
            timeLeft = 0,
            score = 0,
            targets = emptyMap(),
        )
}



private const val TAG = "TutorialGameEngine"