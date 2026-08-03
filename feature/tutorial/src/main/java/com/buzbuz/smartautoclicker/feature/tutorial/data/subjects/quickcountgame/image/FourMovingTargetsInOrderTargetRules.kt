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
package com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image

import android.graphics.RectF

import com.buzbuz.smartautoclicker.core.base.extensions.nextPositionIn
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.subject.quickclickgame.QuickClickGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.subject.quickclickgame.QuickClickGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.subject.quickclickgame.QuickClickGameTargetType

import kotlin.random.Random



internal class FourMovingTargetsInOrderTargetRules : QuickClickGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var score: Int = 0
    /** Index into [CLICK_ORDER] of the next target the user must click. */
    private var nextExpectedIndex: Int = 0

    override fun getScore(): Int = score

    override fun onStart(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        score = 0
        nextExpectedIndex = 0
        return getNewTargets()
    }

    override fun onTargetHit(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        type: QuickClickGameTargetType,
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val expected = CLICK_ORDER[nextExpectedIndex]

        if (type != expected) {
            // Wrong target — shuffle everything back
            nextExpectedIndex = 0
            return getNewTargets()
        }

        val isLastInSequence = nextExpectedIndex == CLICK_ORDER.lastIndex

        return if (isLastInSequence) {
            // Yellow clicked correctly — bonus points, restart sequence
            score += YELLOW_BONUS_SCORE
            nextExpectedIndex = 0
            getNewTargets()
        } else {
            // Correct non-final target — award 1 pt and remove it
            score += 1
            nextExpectedIndex++
            current.toMutableMap().apply { remove(type) }
        }
    }

    override fun onTimerTick(
        current: Map<QuickClickGameTargetType, QuickClickGameTargetState>,
        timeLeft: Long,
    ): Map<QuickClickGameTargetType, QuickClickGameTargetState> = current

    private fun getNewTargets(): Map<QuickClickGameTargetType, QuickClickGameTargetState> {
        val left = TARGET_MARGIN
        val right = 1f - TARGET_MARGIN
        val midX = (left + right) / 2f
        val midY = (left + right) / 2f

        // One target per quadrant — guarantees full-area coverage with no overlap
        val quadrants = listOf(
            RectF(left, left, midX - QUADRANT_GAP, midY - QUADRANT_GAP),
            RectF(midX + QUADRANT_GAP, left, right, midY - QUADRANT_GAP),
            RectF(left, midY + QUADRANT_GAP, midX - QUADRANT_GAP, right),
            RectF(midX + QUADRANT_GAP, midY + QUADRANT_GAP, right, right),
        ).shuffled(random)

        return CLICK_ORDER.zip(quadrants).associate { (type, quadrant) ->
            type to QuickClickGameTargetState.StaticContent(random.nextPositionIn(quadrant))
        }
    }
}

/** Expected click order: blue → red → green → yellow */
private val CLICK_ORDER = listOf(
    QuickClickGameTargetType.IMAGE_BLUE,
    QuickClickGameTargetType.IMAGE_RED,
    QuickClickGameTargetType.IMAGE_GREEN,
    QuickClickGameTargetType.IMAGE_YELLOW,
)

private const val TARGET_MARGIN = 0.12f
private const val QUADRANT_GAP = 0.12f
private const val YELLOW_BONUS_SCORE = 10
