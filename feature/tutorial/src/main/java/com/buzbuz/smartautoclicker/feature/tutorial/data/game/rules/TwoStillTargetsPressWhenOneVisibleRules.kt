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

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

internal class TwoStillTargetsPressWhenOneVisibleRules(highScore: Int) : BaseGameRules(highScore) {

    private var redTargetPosition: PointF? = null
    private var nextRedVisibilityToggleTimerValue: Int? = null

    override fun onStart(area: Rect, targetSize: Int) {
        redTargetPosition = PointF(area.width() - (targetSize * 1.5f), (area.height() - targetSize) / 2f)

        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to PointF(targetSize / 2f, (area.height() - targetSize) / 2f),
        )
        toggleRedVisibility()
    }

    override fun onTimerTick(timeLeft: Int) {
        if (timeLeft != nextRedVisibilityToggleTimerValue) return
        toggleRedVisibility()
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        val blueIsVisible = _targets.value.containsKey(TutorialGameTargetType.BLUE)
        val redIsVisible = _targets.value.containsKey(TutorialGameTargetType.RED)

        if (type == TutorialGameTargetType.BLUE && blueIsVisible && !redIsVisible) score.value++
        else score.value--
    }

    private fun toggleRedVisibility() {
        val redPosition = redTargetPosition ?: return

        _targets.value = _targets.value.toMutableMap().apply {
            if (containsKey(TutorialGameTargetType.RED)) remove(TutorialGameTargetType.RED)
            else put(TutorialGameTargetType.RED, redPosition)
        }
        nextRedVisibilityToggleTimerValue = timer.value - 1
    }
}