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
import android.content.Intent
import android.graphics.Path
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Action.Click
import com.buzbuz.smartautoclicker.domain.Action.Pause
import com.buzbuz.smartautoclicker.domain.Action.Swipe
import com.buzbuz.smartautoclicker.domain.putExtra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Execute the actions of an event.
 *
 * @param androidExecutor the executor for the actions requiring an interaction with Android.
 */
internal class ActionExecutor(private val androidExecutor: AndroidExecutor) {

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
                is Action.Intent -> executeIntent(action)
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
            androidExecutor.executeGesture(clickBuilder.build())
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
            androidExecutor.executeGesture(clickBuilder.build())
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

    /**
     * Execute the provided intent.
     * @param intent the intent to be executed.
     */
    private suspend fun executeIntent(intent: Action.Intent) {
        val androidIntent = Intent().apply {
            action = intent.intentAction!!
            flags = intent.flags!!

            intent.componentName?.let {
                component = intent.componentName
            }

            intent.extras?.forEach { putExtra(it) }
        }

        if (intent.isBroadcast!!) {
            withContext(Dispatchers.Main) {
                androidExecutor.executeSendBroadcast(androidIntent)
            }
            delay(INTENT_BROADCAST_DELAY)
        } else {
            withContext(Dispatchers.Main) {
                androidExecutor.executeStartActivity(androidIntent)
            }
            delay(INTENT_START_ACTIVITY_DELAY)
        }
    }
}

/** Execute the actions related to Android. */
interface AndroidExecutor {

    /** Execute the provided gesture. */
    fun executeGesture(gestureDescription: GestureDescription)

    /** Start the activity defined by the provided intent. */
    fun executeStartActivity(intent: Intent)

    /** Send a broadcast defined by the provided intent. */
    fun executeSendBroadcast(intent: Intent)
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"
/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L