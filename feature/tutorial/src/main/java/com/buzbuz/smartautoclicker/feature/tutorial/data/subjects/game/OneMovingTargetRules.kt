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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.nextFloat
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

import kotlin.random.Random

internal class OneMovingTargetRules : TutorialGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var score: Int = 0
    private var targetsArea: RectF? = null

    override fun getScore(): Int = score

    override fun onStart(area: Rect, targetSize: Int): Map<TutorialGameTargetType, PointF> {
        targetsArea = RectF(
            area.left.toFloat() + TARGET_MARGIN,
            area.top.toFloat() + TARGET_MARGIN,
            area.right.toFloat() - targetSize - TARGET_MARGIN,
            area.bottom.toFloat() - targetSize - TARGET_MARGIN,
        )

        return updateTargetPosition()
    }

    override fun onValidTargetHit(
        current: Map<TutorialGameTargetType, PointF>,
        type: TutorialGameTargetType,
    ): Map<TutorialGameTargetType, PointF> {

        if (type != TutorialGameTargetType.BLUE) return current

        score++
        return updateTargetPosition()
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, PointF>,
        timeLeft: Long,
    ): Map<TutorialGameTargetType, PointF> = current

    private fun updateTargetPosition() : Map<TutorialGameTargetType, PointF> {
        val area = targetsArea ?: return emptyMap()

        return mapOf(
            TutorialGameTargetType.BLUE to PointF(
                random.nextFloat(area.left, area.right),
                random.nextFloat(area.top, area.bottom),
            ),
        )
    }
}

private const val TARGET_MARGIN = 10