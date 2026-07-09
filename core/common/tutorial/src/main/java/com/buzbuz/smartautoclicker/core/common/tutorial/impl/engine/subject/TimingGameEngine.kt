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
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialSubjectState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.abs


internal class TimingGameEngine @Inject constructor(
    private val game: TutorialSubject.TimingGame,
) : TutorialSubjectController.TimingGame {

    private val _state: MutableStateFlow<TutorialSubjectState.TimingGame> = MutableStateFlow(defaultState())
    override val state: StateFlow<TutorialSubjectState.TimingGame> = _state

    private var lastClickTimestamp: Long? = null

    override fun start() {
        if (game.clickCount <= 0 || game.targetTotalDiffMs <= 0) {
            Log.e(TAG, "Can't start timing game, declaration values are invalid $game")
            return
        }

        lastClickTimestamp = System.currentTimeMillis()
        _state.update { old ->
            old.copy(
                isStarted = true,
                isWon = null,
                cumulativeTimeDiffMs = 0,
                lastTimeDiffMs = 0,
                clickCount = 1,
            )
        }
    }

    override fun onGameTimingButtonHit() {
        val previousGameState = _state.value
        if (!previousGameState.isStarted || previousGameState.isWon != null) return

        val previousTimestampMs = lastClickTimestamp ?: return
        val newTimestampMs = System.currentTimeMillis()
        lastClickTimestamp = newTimestampMs

        val diffMs = game.frequencyMs - (newTimestampMs - previousTimestampMs)
        val newCumulativeDiffMs = previousGameState.cumulativeTimeDiffMs + diffMs
        val newClickCount = previousGameState.clickCount + 1
        val isWon: Boolean? =
            if (newClickCount >= game.clickCount) {
                abs(newCumulativeDiffMs) <= game.targetTotalDiffMs
            } else null

        _state.update { old ->
            old.copy(
                cumulativeTimeDiffMs = newCumulativeDiffMs,
                lastTimeDiffMs = diffMs,
                clickCount = newClickCount,
                isWon = isWon,
            )
        }
    }

    override fun stop() {
        lastClickTimestamp = null
        _state.update { defaultState() }
    }

    private fun defaultState(): TutorialSubjectState.TimingGame =
        TutorialSubjectState.TimingGame(
            subject = game,
            isStarted = false,
            isWon = null,
            clickCount = 0,
            cumulativeTimeDiffMs = 0,
            lastTimeDiffMs = 0,
        )
}

private const val TAG = "TimingGameEngine"