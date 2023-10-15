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
package com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.getNextPositionIn
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlin.random.Random

internal class TwoMovingTargetsPressInOrderRules(highScore: Int) : BaseGameRules(highScore) {

    private val random: Random = Random(System.currentTimeMillis())

    private var targetsArea: RectF? = null
    private var targetHalfSize: Float? = null

    override fun onStart(area: Rect, targetSize: Int) {
        targetsArea = RectF(
            area.left.toFloat() + TARGET_MARGIN,
            area.top.toFloat() + TARGET_MARGIN,
            area.right.toFloat() - targetSize - TARGET_MARGIN,
            area.bottom.toFloat() - targetSize - TARGET_MARGIN,
        )
        targetHalfSize = targetSize / 2f

        showNewTargets()
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        val targets = _targets.value
        when {
            type == TutorialGameTargetType.RED && targets.containsKey(TutorialGameTargetType.RED) && targets.containsKey(
                TutorialGameTargetType.BLUE
            ) -> removeRedTarget()
            type == TutorialGameTargetType.BLUE && !targets.containsKey(TutorialGameTargetType.RED) && targets.containsKey(
                TutorialGameTargetType.BLUE
            ) -> {
                score.value += 2
                showNewTargets()
            }
            else -> score.value--
        }
    }

    private fun showNewTargets() {
        val area = targetsArea ?: return
        val targetHalfSize = targetHalfSize ?: return

        // Find two positions and ensure the target will not overlaps
        val bluePosition = random.getNextPositionIn(area)
        var redPosition: PointF
        do {
            redPosition = random.getNextPositionIn(area)
        } while (bluePosition.enclosingRectIntersects(redPosition, targetHalfSize))

        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to bluePosition,
            TutorialGameTargetType.RED to redPosition,
        )
    }

    private fun removeRedTarget() {
        _targets.value = _targets.value.toMutableMap().apply {
            remove(TutorialGameTargetType.RED)
        }
    }
}

private fun PointF.enclosingRectIntersects(other: PointF, shapeHalfSize: Float): Boolean =
    toEnclosingRect(shapeHalfSize).intersect(other.toEnclosingRect(shapeHalfSize))

private fun PointF.toEnclosingRect(halfSize: Float): RectF =
    RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize)