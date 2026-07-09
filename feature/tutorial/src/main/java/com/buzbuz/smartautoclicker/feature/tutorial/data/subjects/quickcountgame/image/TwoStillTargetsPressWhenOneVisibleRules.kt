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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType

internal class TwoStillTargetsPressWhenOneVisibleRules : QuickClickGameRules {

    private var redTargetPosition: PointF? = null
    private var score: Int = 0


    override fun getScore(): Int =
        score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0
        redTargetPosition = PointF(0.75f, 0.5f)

        return toggleRedVisibility(
            mapOf(QuickClickGameTargetType.IMAGE_BLUE to QuickClickGameTargetState.StaticContent(PointF(0.25f, 0.5f)))
        )
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val blueIsVisible = current.containsKey(QuickClickGameTargetType.IMAGE_BLUE)
        val redIsVisible = current.containsKey(QuickClickGameTargetType.IMAGE_RED)

        if (type == QuickClickGameTargetType.IMAGE_BLUE && blueIsVisible && !redIsVisible) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        toggleRedVisibility(current)

    private fun toggleRedVisibility(
        targets: Map<QuickClickGameTargetType, QuickClickGameTargetState>
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val redPosition = redTargetPosition ?: return targets

        val newTargets = targets.toMutableMap().apply {
            if (containsKey(QuickClickGameTargetType.IMAGE_RED)) remove(QuickClickGameTargetType.IMAGE_RED)
            else put(QuickClickGameTargetType.IMAGE_RED, QuickClickGameTargetState.StaticContent(redPosition))
        }

        return newTargets
    }
}