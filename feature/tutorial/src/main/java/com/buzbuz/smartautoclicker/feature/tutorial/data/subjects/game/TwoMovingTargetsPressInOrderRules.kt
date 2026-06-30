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

import com.buzbuz.smartautoclicker.core.base.extensions.nextPositionIn
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

import kotlin.random.Random

internal class TwoMovingTargetsPressInOrderRules : TutorialGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var targetsArea: RectF? = null
    private var targetHalfSize: Float? = null
    private var score: Int = 0

    override fun getScore(): Int =
        score

    override fun onStart(area: Rect, targetSize: Int): Map<TutorialGameTargetType, PointF> {
        targetsArea = RectF(
            area.left.toFloat() + TARGET_MARGIN,
            area.top.toFloat() + TARGET_MARGIN,
            area.right.toFloat() - targetSize - TARGET_MARGIN,
            area.bottom.toFloat() - targetSize - TARGET_MARGIN,
        )
        targetHalfSize = targetSize / 2f

        return getNewTargets()
    }

    override fun onValidTargetHit(
        current: Map<TutorialGameTargetType, PointF>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, PointF> =
        when (type) {
            TutorialGameTargetType.RED if current.containsKey(TutorialGameTargetType.RED) && current.containsKey(TutorialGameTargetType.BLUE) ->
                removeRedTarget(current)

            TutorialGameTargetType.BLUE if !current.containsKey(TutorialGameTargetType.RED) && current.containsKey(TutorialGameTargetType.BLUE) -> {
                score += 2
                getNewTargets()
            }

            else -> {
                score--
                current
            }
        }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, PointF>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, PointF> = current

    private fun getNewTargets(): Map<TutorialGameTargetType, PointF> {
        val area = targetsArea ?: return emptyMap()
        val targetHalfSize = targetHalfSize ?: return emptyMap()

        // Find two positions and ensure the target will not overlaps
        val bluePosition = random.nextPositionIn(area)
        var redPosition: PointF
        do {
            redPosition = random.nextPositionIn(area)
        } while (bluePosition.enclosingRectIntersects(redPosition, targetHalfSize))

        return mapOf(
            TutorialGameTargetType.BLUE to bluePosition,
            TutorialGameTargetType.RED to redPosition,
        )
    }

    private fun removeRedTarget(targets: Map<TutorialGameTargetType, PointF>): Map<TutorialGameTargetType, PointF> =
        targets.toMutableMap().apply { remove(TutorialGameTargetType.RED) }
}

private fun PointF.enclosingRectIntersects(other: PointF, shapeHalfSize: Float): Boolean =
    toEnclosingRect(shapeHalfSize).intersect(other.toEnclosingRect(shapeHalfSize))

private fun PointF.toEnclosingRect(halfSize: Float): RectF =
    RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize)

private const val TARGET_MARGIN = 10