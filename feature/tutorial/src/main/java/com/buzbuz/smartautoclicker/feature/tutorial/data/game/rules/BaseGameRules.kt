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

import com.buzbuz.smartautoclicker.feature.tutorial.domain.game.TutorialGameRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal abstract class BaseGameRules : TutorialGameRules {

    private val _isStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    final override val isStarted: StateFlow<Boolean> = _isStarted

    private val _timer: MutableStateFlow<Int> = MutableStateFlow(GAME_DURATION_SECONDS)
    final override val timer: StateFlow<Int> = _timer

    protected val _score: MutableStateFlow<Int> = MutableStateFlow(0)
    final override val score: StateFlow<Int> = _score

    protected val _targets: MutableStateFlow<Map<TutorialGameTargetType, PointF>> = MutableStateFlow(emptyMap())
    final override val targets: StateFlow<Map<TutorialGameTargetType, PointF>> = _targets

    private var gameJob: Job? = null

    final override fun start(coroutineScope: CoroutineScope, area: Rect, targetSize: Int) {
        if (_isStarted.value) return

        gameJob = coroutineScope.launch {
            // Init game values
            _isStarted.value = true
            _timer.value = GAME_DURATION_SECONDS
            _score.value = 0

            onStart(area, targetSize)
            onTimerTick(GAME_DURATION_SECONDS)

            // Loop for the total duration of the game, and update the timer one by one
            for (i in GAME_DURATION_SECONDS - 1 downTo  0) {
                delay(1.seconds)
                _timer.value = i
                onTimerTick(i)
            }

            // Game is over
            _isStarted.value = false
            _targets.value = emptyMap()
        }
    }

    final override fun stop() {
        if (!_isStarted.value) return

        gameJob?.cancel()
        gameJob = null

        _isStarted.value = false
        _timer.value = 0
        _targets.value = emptyMap()
    }

    abstract fun onStart(area: Rect, targetSize: Int)
    open fun onTimerTick(timeLeft: Int) { /* Default impl does nothing. */ }
}

private const val GAME_DURATION_SECONDS = 20