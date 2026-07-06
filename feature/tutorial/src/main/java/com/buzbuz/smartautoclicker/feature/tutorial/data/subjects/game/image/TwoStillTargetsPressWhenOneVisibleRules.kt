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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.image

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

internal class TwoStillTargetsPressWhenOneVisibleRules : TutorialGameRules {

    private var redTargetPosition: PointF? = null
    private var score: Int = 0


    override fun getScore(): Int =
        score

    override fun onStart(): Map<TutorialGameTargetType, TutorialGameTargetState> {
        score = 0
        redTargetPosition = PointF(0.75f, 0.5f)

        return toggleRedVisibility(
            mapOf(TutorialGameTargetType.IMAGE_BLUE to TutorialGameTargetState.StaticContent(PointF(0.25f, 0.5f)))
        )
    }

    override fun onTargetHit(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, TutorialGameTargetState> {
        val blueIsVisible = current.containsKey(TutorialGameTargetType.IMAGE_BLUE)
        val redIsVisible = current.containsKey(TutorialGameTargetType.IMAGE_RED)

        if (type == TutorialGameTargetType.IMAGE_BLUE && blueIsVisible && !redIsVisible) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, TutorialGameTargetState> =
        toggleRedVisibility(current)

    private fun toggleRedVisibility(
        targets: Map<TutorialGameTargetType, TutorialGameTargetState>
    ): Map<TutorialGameTargetType, TutorialGameTargetState> {
        val redPosition = redTargetPosition ?: return targets

        val newTargets = targets.toMutableMap().apply {
            if (containsKey(TutorialGameTargetType.IMAGE_RED)) remove(TutorialGameTargetType.IMAGE_RED)
            else put(TutorialGameTargetType.IMAGE_RED, TutorialGameTargetState.StaticContent(redPosition))
        }

        return newTargets
    }
}