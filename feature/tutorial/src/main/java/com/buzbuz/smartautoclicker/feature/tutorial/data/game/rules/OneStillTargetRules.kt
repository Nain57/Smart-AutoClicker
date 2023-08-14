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

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

internal class OneStillTargetRules(highScore: Int) : BaseGameRules(highScore) {

    override fun onStart(area: Rect, targetSize: Int) {
        _targets.value = mapOf(
            TutorialGameTargetType.BLUE to PointF((area.width() - targetSize) / 2f, (area.height() - targetSize) / 2f),
        )
    }

    override fun onValidTargetHit(type: TutorialGameTargetType) {
        if (type != TutorialGameTargetType.BLUE) return
        score.value++
    }

}