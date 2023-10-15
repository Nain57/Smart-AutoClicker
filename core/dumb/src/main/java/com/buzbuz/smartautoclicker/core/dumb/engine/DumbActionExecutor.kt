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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Path

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.base.extensions.getRandomizedDuration
import com.buzbuz.smartautoclicker.core.base.extensions.getRandomizedGestureDuration
import com.buzbuz.smartautoclicker.core.base.extensions.lineTo
import com.buzbuz.smartautoclicker.core.base.extensions.moveTo
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal class DumbActionExecutor(private val androidExecutor: AndroidExecutor) {

    private val random: Random = Random(System.currentTimeMillis())

    suspend fun executeDumbAction(action: DumbAction, randomize: Boolean) {
        when (action) {
            is DumbAction.DumbClick -> executeDumbClick(action, randomize)
            is DumbAction.DumbSwipe -> executeDumbSwipe(action, randomize)
            is DumbAction.DumbPause -> executeDumbPause(action, randomize)
        }
    }

    private suspend fun executeDumbClick(dumbClick: DumbAction.DumbClick, randomize: Boolean) {
        val clickPath = Path()
        val clickBuilder = GestureDescription.Builder()

        clickPath.moveTo(dumbClick.position.x, dumbClick.position.y, getRandomizedParameter(randomize))
        clickBuilder.addStroke(
            GestureDescription.StrokeDescription(
                clickPath,
                0,
                getRandomizedParameter(randomize)
                    ?.getRandomizedGestureDuration(dumbClick.pressDurationMs)
                    ?: dumbClick.pressDurationMs.coerceIn(1, GESTURE_DURATION_MAX_VALUE),
            )
        )

        executeRepeatableGesture(clickBuilder.build(), dumbClick)
    }

    private suspend fun executeDumbSwipe(dumbSwipe: DumbAction.DumbSwipe, randomize: Boolean) {
        val swipePath = Path()
        val swipeBuilder = GestureDescription.Builder()
        val randomizedParam = getRandomizedParameter(randomize)

        swipePath.moveTo(dumbSwipe.fromPosition.x, dumbSwipe.fromPosition.y, randomizedParam)
        swipePath.lineTo(dumbSwipe.toPosition.x, dumbSwipe.toPosition.y, randomizedParam)
        swipeBuilder.addStroke(
            GestureDescription.StrokeDescription(
                swipePath,
                0,
                getRandomizedParameter(randomize)
                    ?.getRandomizedGestureDuration(dumbSwipe.swipeDurationMs)
                    ?: dumbSwipe.swipeDurationMs.coerceIn(1, GESTURE_DURATION_MAX_VALUE),
            )
        )

        executeRepeatableGesture(swipeBuilder.build(), dumbSwipe)
    }

    private suspend fun executeDumbPause(dumbPause: DumbAction.DumbPause, randomize: Boolean) {
        delay(
            getRandomizedParameter(randomize)
                ?.getRandomizedDuration(dumbPause.pauseDurationMs)
                ?: dumbPause.pauseDurationMs
        )
    }

    private suspend fun executeRepeatableGesture(gesture: GestureDescription, repeatable: Repeatable) {
        repeatable.repeat {
            withContext(Dispatchers.Main) {
                androidExecutor.executeGesture(gesture)
            }
        }
    }

    private fun getRandomizedParameter(randomize: Boolean): Random? =
        if (randomize) random else null
}