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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.text

import android.graphics.PointF
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

internal class OneStillChangingTextRules : TutorialGameRules {

    private var score: Int = 0
    private var targetPosition: PointF = PointF(0f, 0f)

    override fun getScore(): Int = score

    override fun onStart(area: Rect): Map<TutorialGameTargetType, PointF> {
        targetPosition = PointF(area.width() / 2f, area.height() / 2f)
        score = 0

        return mapOf(TutorialGameTargetType.TEXT_HELLO to targetPosition)
    }

    override fun onValidTargetHit(
        current: Map<TutorialGameTargetType, PointF>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, PointF> {
        if (type == TutorialGameTargetType.TEXT_HELLO) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, PointF>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, PointF> {
        val newType =
            if (timeLeft % 2 == 1L) TutorialGameTargetType.TEXT_HELLO
            else TutorialGameTargetType.TEXT_GOODBYE

        return mapOf(newType to targetPosition)
    }

}