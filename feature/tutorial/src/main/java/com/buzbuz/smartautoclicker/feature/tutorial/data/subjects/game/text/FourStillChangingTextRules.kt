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

internal class FourStillChangingTextRules : TutorialGameRules {

    private var score: Int = 0

    private var topLeftPosition: PointF = PointF(0f, 0f)
    private var topRightPosition: PointF = PointF(0f, 0f)
    private var bottomLeftPosition: PointF = PointF(0f, 0f)
    private var bottomRightPosition: PointF = PointF(0f, 0f)


    override fun getScore(): Int = score

    override fun onStart(area: Rect): Map<TutorialGameTargetType, PointF> {
        topLeftPosition = PointF(area.width() / 4f, area.height() / 4f)
        topRightPosition = PointF(area.width() * 3f / 4f, area.height() / 4f)
        bottomLeftPosition = PointF(area.width() / 4f, area.height() * 3f / 4f)
        bottomRightPosition = PointF(area.width() * 3f / 4f, area.height() * 3f / 4f)
        score = 0

        return mapOf(
            TutorialGameTargetType.TEXT_HELLO to topLeftPosition,
            TutorialGameTargetType.TEXT_GOODBYE to topRightPosition,
            TutorialGameTargetType.TEXT_MORNING to bottomLeftPosition,
            TutorialGameTargetType.TEXT_AFTERNOON to bottomRightPosition,
        )
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
        val positions = listOf(topLeftPosition, topRightPosition, bottomLeftPosition, bottomRightPosition).shuffled()
        return mapOf(
            TutorialGameTargetType.TEXT_HELLO to positions[0],
            TutorialGameTargetType.TEXT_GOODBYE to positions[1],
            TutorialGameTargetType.TEXT_MORNING to positions[2],
            TutorialGameTargetType.TEXT_AFTERNOON to positions[3],
        )
    }

}