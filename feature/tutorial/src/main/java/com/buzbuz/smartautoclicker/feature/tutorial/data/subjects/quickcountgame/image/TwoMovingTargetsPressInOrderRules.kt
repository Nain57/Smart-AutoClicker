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
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.nextPositionIn
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType

import kotlin.random.Random

internal class TwoMovingTargetsPressInOrderRules : QuickClickGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var score: Int = 0

    override fun getScore(): Int =
        score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0
        return getNewTargets()
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        when (type) {
            QuickClickGameTargetType.IMAGE_RED if current.containsKey(QuickClickGameTargetType.IMAGE_RED) && current.containsKey(QuickClickGameTargetType.IMAGE_BLUE) ->
                removeRedTarget(current)

            QuickClickGameTargetType.IMAGE_BLUE if !current.containsKey(QuickClickGameTargetType.IMAGE_RED) && current.containsKey(QuickClickGameTargetType.IMAGE_BLUE) -> {
                score += 2
                getNewTargets()
            }

            else -> {
                score--
                current
            }
        }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> = current

    private fun getNewTargets(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val area = RectF(TARGET_MARGIN, TARGET_MARGIN, 1f - TARGET_MARGIN, 1f - TARGET_MARGIN)

        // Find two positions and ensure the targets do not overlap
        val bluePosition = random.nextPositionIn(area)
        var redPosition: PointF
        do {
            redPosition = random.nextPositionIn(area)
        } while (bluePosition.enclosingRectIntersects(redPosition, TARGET_HALF_SIZE))

        return mapOf(
            QuickClickGameTargetType.IMAGE_BLUE to QuickClickGameTargetState.StaticContent(bluePosition),
            QuickClickGameTargetType.IMAGE_RED to QuickClickGameTargetState.StaticContent(redPosition),
        )
    }

    private fun removeRedTarget(
        targets: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> =
        targets.toMutableMap().apply { remove(QuickClickGameTargetType.IMAGE_RED) }
}

private fun PointF.enclosingRectIntersects(other: PointF, shapeHalfSize: Float): Boolean =
    toEnclosingRect(shapeHalfSize).intersect(other.toEnclosingRect(shapeHalfSize))

private fun PointF.toEnclosingRect(halfSize: Float): RectF =
    RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize)

private const val TARGET_MARGIN = 0.05f
private const val TARGET_HALF_SIZE = 0.15f