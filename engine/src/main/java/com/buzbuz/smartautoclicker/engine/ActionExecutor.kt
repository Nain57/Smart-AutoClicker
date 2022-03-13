/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.database.domain.Action.Click
import com.buzbuz.smartautoclicker.database.domain.Action.Pause
import com.buzbuz.smartautoclicker.database.domain.Action.Swipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Execute the actions of an event.
 *
 * @param gestureExecutor The executor for the actions requiring a gesture on the user screen.
 */
internal class ActionExecutor(private val gestureExecutor: (GestureDescription) -> Unit) {

    /**
     * Execute the provided actions.
     * @param actions the actions to be executed.
     * @param conditionPosition the position of the detected condition.
     */
    suspend fun executeActions(actions: List<Action>, conditionPosition: Point?) {
        actions.forEach { action ->
            when (action) {
                is Click -> executeClick(action, conditionPosition)
                is Swipe -> executeSwipe(action)
                is Pause -> executePause(action)
            }
        }
    }

    /**
     * Execute the provided click.
     * @param click the click to be executed.
     */
    private suspend fun executeClick(click: Click, conditionPosition: Point?) {
        val clickPath = Path()
        val clickBuilder = GestureDescription.Builder()

        if (click.clickOnCondition) {
            conditionPosition?.let { conditionCenter ->
                clickPath.moveTo(conditionCenter.x.toFloat(), conditionCenter.y.toFloat())
            } ?: run {
                Log.w(TAG, "Can't click on position, there is no condition position")
                return
            }
        } else {
            clickPath.moveTo(click.x!!.toFloat(), click.y!!.toFloat())
        }
        clickBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, click.pressDuration!!))

        withContext(Dispatchers.Main) {
            gestureExecutor(clickBuilder.build())
        }
        delay(click.pressDuration!!)
    }

    /**
     * Execute the provided swipe.
     * @param swipe the swipe to be executed.
     */
    private suspend fun executeSwipe(swipe: Swipe) {
        val swipePath = Path()
        val clickBuilder = GestureDescription.Builder()

        swipePath.moveTo(swipe.fromX!!.toFloat(), swipe.fromY!!.toFloat())
        swipePath.lineTo(swipe.toX!!.toFloat(), swipe.toY!!.toFloat())
        clickBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, swipe.swipeDuration!!))

        withContext(Dispatchers.Main) {
            gestureExecutor(clickBuilder.build())
        }
        delay(swipe.swipeDuration!!)
    }

    /**
     * Execute the provided pause.
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause) {
        delay(pause.pauseDuration!!)
    }
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"