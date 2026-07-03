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
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

internal class TwoStillTargetsPressWhenOneVisibleRules : TutorialGameRules {

    private var redTargetPosition: PointF? = null
    private var score: Int = 0


    override fun getScore(): Int =
        score

    override fun onStart(area: Rect): Map<TutorialGameTargetType, PointF> {
        score = 0
        redTargetPosition = PointF(area.width() * 0.75f, area.height() / 2f)

        return toggleRedVisibility(
            mapOf(TutorialGameTargetType.IMAGE_BLUE to PointF(area.width() * 0.25f, area.height() / 2f))
        )
    }

    override fun onValidTargetHit(
        current: Map<TutorialGameTargetType, PointF>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, PointF> {
        val blueIsVisible = current.containsKey(TutorialGameTargetType.IMAGE_BLUE)
        val redIsVisible = current.containsKey(TutorialGameTargetType.IMAGE_RED)

        if (type == TutorialGameTargetType.IMAGE_BLUE && blueIsVisible && !redIsVisible) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, PointF>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, PointF> =
        toggleRedVisibility(current)

    private fun toggleRedVisibility(targets: Map<TutorialGameTargetType, PointF>): Map<TutorialGameTargetType, PointF> {
        val redPosition = redTargetPosition ?: return targets

        val newTargets = targets.toMutableMap().apply {
            if (containsKey(TutorialGameTargetType.IMAGE_RED)) remove(TutorialGameTargetType.IMAGE_RED)
            else put(TutorialGameTargetType.IMAGE_RED, redPosition)
        }

        return newTargets
    }
}