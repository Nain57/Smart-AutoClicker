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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType

internal class TwoStillTargetsPressAnyRules : QuickClickGameRules {

    private val bluePosition = PointF(0.25f, 0.5f)
    private val redPosition = PointF(0.75f, 0.5f)
    private var score: Int = 0

    override fun getScore(): Int = score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0
        return buildTargetsForTimeLeft(0)
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        when (type) {
            QuickClickGameTargetType.IMAGE_RED -> score += 2
            QuickClickGameTargetType.IMAGE_BLUE -> score += 1
            else -> Unit
        }
        return current
    }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        buildTargetsForTimeLeft(timeLeft)

    private fun buildTargetsForTimeLeft(timeLeft: Long): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        when (timeLeft % 3) {
            0L -> mapOf(
                QuickClickGameTargetType.IMAGE_BLUE to QuickClickGameTargetState.StaticContent(bluePosition),
                QuickClickGameTargetType.IMAGE_RED to QuickClickGameTargetState.StaticContent(redPosition),
            )
            1L -> mapOf(
                QuickClickGameTargetType.IMAGE_BLUE to QuickClickGameTargetState.StaticContent(bluePosition),
            )
            else -> mapOf(
                QuickClickGameTargetType.IMAGE_RED to QuickClickGameTargetState.StaticContent(redPosition),
            )
        }
}