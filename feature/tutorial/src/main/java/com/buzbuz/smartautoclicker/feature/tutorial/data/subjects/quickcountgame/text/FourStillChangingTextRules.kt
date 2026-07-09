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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.text

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType

internal class FourStillChangingTextRules : QuickClickGameRules {

    private var score: Int = 0

    private val topLeftPosition: PointF = PointF(0.25f, 0.25f)
    private val topRightPosition: PointF = PointF(0.75f, 0.25f)
    private val bottomLeftPosition: PointF = PointF(0.25f, 0.75f)
    private val bottomRightPosition: PointF = PointF(0.75f, 0.75f)


    override fun getScore(): Int = score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0

        return mapOf(
            QuickClickGameTargetType.TEXT_HELLO to QuickClickGameTargetState.StaticContent(topLeftPosition),
            QuickClickGameTargetType.TEXT_GOODBYE to QuickClickGameTargetState.StaticContent(topRightPosition),
            QuickClickGameTargetType.TEXT_NIGHT to QuickClickGameTargetState.StaticContent(bottomLeftPosition),
            QuickClickGameTargetType.TEXT_DAY to QuickClickGameTargetState.StaticContent(bottomRightPosition),
        )
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        if (type == QuickClickGameTargetType.TEXT_HELLO) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val positions = listOf(topLeftPosition, topRightPosition, bottomLeftPosition, bottomRightPosition).shuffled()
        return mapOf(
            QuickClickGameTargetType.TEXT_HELLO to QuickClickGameTargetState.StaticContent(positions[0]),
            QuickClickGameTargetType.TEXT_GOODBYE to QuickClickGameTargetState.StaticContent(positions[1]),
            QuickClickGameTargetType.TEXT_NIGHT to QuickClickGameTargetState.StaticContent(positions[2]),
            QuickClickGameTargetType.TEXT_DAY to QuickClickGameTargetState.StaticContent(positions[3]),
        )
    }

}