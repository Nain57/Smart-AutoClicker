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

internal class OneStillChangingTextRules : QuickClickGameRules {

    private var score: Int = 0
    private val targetPosition: PointF = PointF(0.5f, 0.5f)

    override fun getScore(): Int = score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0

        return mapOf(QuickClickGameTargetType.TEXT_HELLO to QuickClickGameTargetState.StaticContent(targetPosition))
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
        val newType =
            if (timeLeft % 2 == 1L) QuickClickGameTargetType.TEXT_HELLO
            else QuickClickGameTargetType.TEXT_GOODBYE

        return mapOf(newType to QuickClickGameTargetState.StaticContent(targetPosition))
    }

}