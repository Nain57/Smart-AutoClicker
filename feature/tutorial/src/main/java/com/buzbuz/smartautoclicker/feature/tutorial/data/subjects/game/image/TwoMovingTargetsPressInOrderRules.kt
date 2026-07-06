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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.image

import android.graphics.PointF
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.nextPositionIn
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

import kotlin.random.Random

internal class TwoMovingTargetsPressInOrderRules : TutorialGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var score: Int = 0

    override fun getScore(): Int =
        score

    override fun onStart(): Map<TutorialGameTargetType, TutorialGameTargetState> {
        score = 0
        return getNewTargets()
    }

    override fun onTargetHit(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        type: TutorialGameTargetType
    ): Map<TutorialGameTargetType, TutorialGameTargetState> =
        when (type) {
            TutorialGameTargetType.IMAGE_RED if current.containsKey(TutorialGameTargetType.IMAGE_RED) && current.containsKey(TutorialGameTargetType.IMAGE_BLUE) ->
                removeRedTarget(current)

            TutorialGameTargetType.IMAGE_BLUE if !current.containsKey(TutorialGameTargetType.IMAGE_RED) && current.containsKey(TutorialGameTargetType.IMAGE_BLUE) -> {
                score += 2
                getNewTargets()
            }

            else -> {
                score--
                current
            }
        }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        timeLeft: Long
    ): Map<TutorialGameTargetType, TutorialGameTargetState> = current

    private fun getNewTargets(): Map<TutorialGameTargetType, TutorialGameTargetState> {
        val area = RectF(TARGET_MARGIN, TARGET_MARGIN, 1f - TARGET_MARGIN, 1f - TARGET_MARGIN)

        // Find two positions and ensure the targets do not overlap
        val bluePosition = random.nextPositionIn(area)
        var redPosition: PointF
        do {
            redPosition = random.nextPositionIn(area)
        } while (bluePosition.enclosingRectIntersects(redPosition, TARGET_HALF_SIZE))

        return mapOf(
            TutorialGameTargetType.IMAGE_BLUE to TutorialGameTargetState.StaticContent(bluePosition),
            TutorialGameTargetType.IMAGE_RED to TutorialGameTargetState.StaticContent(redPosition),
        )
    }

    private fun removeRedTarget(
        targets: Map<TutorialGameTargetType, TutorialGameTargetState>,
    ): Map<TutorialGameTargetType, TutorialGameTargetState> =
        targets.toMutableMap().apply { remove(TutorialGameTargetType.IMAGE_RED) }
}

private fun PointF.enclosingRectIntersects(other: PointF, shapeHalfSize: Float): Boolean =
    toEnclosingRect(shapeHalfSize).intersect(other.toEnclosingRect(shapeHalfSize))

private fun PointF.toEnclosingRect(halfSize: Float): RectF =
    RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize)

private const val TARGET_MARGIN = 0.05f
private const val TARGET_HALF_SIZE = 0.15f