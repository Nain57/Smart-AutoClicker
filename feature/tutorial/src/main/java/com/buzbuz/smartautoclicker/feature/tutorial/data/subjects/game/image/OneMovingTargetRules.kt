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

import com.buzbuz.smartautoclicker.core.base.extensions.nextFloat
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

import kotlin.random.Random

internal class OneMovingTargetRules : TutorialGameRules {

    private val random: Random = Random(System.currentTimeMillis())

    private var score: Int = 0

    override fun getScore(): Int = score

    override fun onStart(): Map<TutorialGameTargetType, TutorialGameTargetState> {
        score = 0
        return updateTargetPosition()
    }

    override fun onTargetHit(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        type: TutorialGameTargetType,
    ): Map<TutorialGameTargetType, TutorialGameTargetState> {

        if (type != TutorialGameTargetType.IMAGE_BLUE) return current

        score++
        return updateTargetPosition()
    }

    override fun onTimerTick(
        current: Map<TutorialGameTargetType, TutorialGameTargetState>,
        timeLeft: Long,
    ): Map<TutorialGameTargetType, TutorialGameTargetState> = current

    private fun updateTargetPosition() : Map<TutorialGameTargetType, TutorialGameTargetState> =
        mapOf(
            TutorialGameTargetType.IMAGE_BLUE to TutorialGameTargetState.StaticContent(
                position = PointF(
                    random.nextFloat(TARGET_MARGIN, 1f - TARGET_MARGIN),
                    random.nextFloat(TARGET_MARGIN, 1f - TARGET_MARGIN),
                )
            ),
        )
}

private const val TARGET_MARGIN = 0.05f