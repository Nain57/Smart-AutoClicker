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
package com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules

import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameStateData
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal abstract class BaseGameRules(override val highScore: Int) : TutorialGameRules {

    private val isStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isWon: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    protected val timer: MutableStateFlow<Int> = MutableStateFlow(GAME_DURATION_SECONDS)
    protected val score: MutableStateFlow<Int> = MutableStateFlow(0)
    final override val gameState: Flow<TutorialGameStateData> =
        combine(isStarted, isWon, timer, score) { started, isWon, timer, score ->
            TutorialGameStateData(started, isWon, timer, score)
        }

    protected val _targets: MutableStateFlow<Map<TutorialGameTargetType, PointF>> = MutableStateFlow(emptyMap())
    final override val targets: StateFlow<Map<TutorialGameTargetType, PointF>> = _targets

    private var gameJob: Job? = null

    final override fun start(coroutineScope: CoroutineScope, area: Rect, targetSize: Int, onResult: (Boolean) -> Unit) {
        if (isStarted.value) return

        gameJob = coroutineScope.launch {
            // Init game values
            isStarted.value = true
            timer.value = GAME_DURATION_SECONDS
            score.value = 0
            isWon.value = null

            Log.d(TAG, "Start game")

            onStart(area, targetSize)
            onTimerTick(GAME_DURATION_SECONDS)

            // Loop for the total duration of the game, and update the timer one by one
            for (i in GAME_DURATION_SECONDS - 1 downTo  0) {
                delay(1.seconds)
                timer.value = i

                Log.d(TAG, "onTimerTick $i")
                onTimerTick(i)
            }

            // Game is over
            isStarted.value = false
            _targets.value = emptyMap()
            isWon.value = score.value > highScore

            Log.d(TAG, "Game over, victory=${isWon.value}")
            onResult(isWon.value == true)
        }
    }

    final override fun stop() {
        Log.d(TAG, "Stop game")

        gameJob?.cancel()
        gameJob = null

        isStarted.value = false
        timer.value = 0
        score.value = 0
        isWon.value = null
        _targets.value = emptyMap()
    }

    final override fun onTargetHit(type: TutorialGameTargetType) {
        if (isStarted.value) onValidTargetHit(type)
        else _targets.value = emptyMap()
    }

    abstract fun onStart(area: Rect, targetSize: Int)
    abstract fun onValidTargetHit(type: TutorialGameTargetType)
    open fun onTimerTick(timeLeft: Int) { /* Default impl does nothing. */ }
}

internal const val TARGET_MARGIN = 10
private const val TAG = "TutorialGameRules"
private const val GAME_DURATION_SECONDS = 10