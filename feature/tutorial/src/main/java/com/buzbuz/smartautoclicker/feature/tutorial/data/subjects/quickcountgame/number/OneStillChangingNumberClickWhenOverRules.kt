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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.number

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType
import kotlin.random.Random

internal class OneStillChangingNumberClickWhenOverRules(
    private val validWhenClickIsOver: Int,
    private val maxValue: Int,
) : QuickClickGameRules {

    private val random = Random(System.currentTimeMillis())

    private var score: Int = 0
    private val targetPosition: PointF = PointF(0.5f, 0.5f)

    override fun getScore(): Int = score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0

        return mapOf(QuickClickGameTargetType.NUMBER to QuickClickGameTargetState.ChangingContent(
            position = targetPosition,
            content = getNextRandomNumberTargetType(false),
        ))
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType,
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        if (type == QuickClickGameTargetType.NUMBER) {
            val state = current[type]
            if (state is QuickClickGameTargetState.ChangingContent && state.content >= validWhenClickIsOver) {
                score++
                return current
            }
        }

        score--
        return current
    }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        mapOf(QuickClickGameTargetType.NUMBER to QuickClickGameTargetState.ChangingContent(
            position = targetPosition,
            content =  getNextRandomNumberTargetType(valid = timeLeft % 2 == 1L)
        ))

    private fun getNextRandomNumberTargetType(valid: Boolean): Int =
        if (valid) random.nextInt(validWhenClickIsOver + 1 , maxValue)
        else random.nextInt(0, validWhenClickIsOver + 1)
}