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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.text

import android.graphics.PointF
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

internal class FourStillChangingTextRules : TutorialGameRules {

    private var score: Int = 0

    private var topLeftPosition: PointF = PointF(0f, 0f)
    private var topRightPosition: PointF = PointF(0f, 0f)
    private var bottomLeftPosition: PointF = PointF(0f, 0f)
    private var bottomRightPosition: PointF = PointF(0f, 0f)


    override fun getScore(): Int = score

    override fun onStart(area: Rect): Map<TutorialGameTargetType, TutorialGameTargetState> {
        topLeftPosition = PointF(area.width() / 4f, area.height() / 4f)
        topRightPosition = PointF(area.width() * 3f / 4f, area.height() / 4f)
        bottomLeftPosition = PointF(area.width() / 4f, area.height() * 3f / 4f)
        bottomRightPosition = PointF(area.width() * 3f / 4f, area.height() * 3f / 4f)
        score = 0

        return mapOf(
            TutorialGameTargetType.TEXT_HELLO to TutorialGameTargetState.StaticContent(topLeftPosition),
            TutorialGameTargetType.TEXT_GOODBYE to TutorialGameTargetState.StaticContent(topRightPosition),
            TutorialGameTargetType.TEXT_NIGHT to TutorialGameTargetState.StaticContent(bottomLeftPosition),
            TutorialGameTargetType.TEXT_DAY to TutorialGameTargetState.StaticContent(bottomRightPosition),
        )
    }

    override fun onTargetHit(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, TutorialGameTargetState> {
        if (type == TutorialGameTargetType.TEXT_HELLO) score++
        else score--

        return current
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, TutorialGameTargetState> {
        val positions = listOf(topLeftPosition, topRightPosition, bottomLeftPosition, bottomRightPosition).shuffled()
        return mapOf(
            TutorialGameTargetType.TEXT_HELLO to TutorialGameTargetState.StaticContent(positions[0]),
            TutorialGameTargetType.TEXT_GOODBYE to TutorialGameTargetState.StaticContent(positions[1]),
            TutorialGameTargetType.TEXT_NIGHT to TutorialGameTargetState.StaticContent(positions[2]),
            TutorialGameTargetType.TEXT_DAY to TutorialGameTargetState.StaticContent(positions[3]),
        )
    }

}