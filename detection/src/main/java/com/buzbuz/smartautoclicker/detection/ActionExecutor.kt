/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.detection

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper

import androidx.annotation.WorkerThread

import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.database.domain.Action.Click
import com.buzbuz.smartautoclicker.database.domain.Action.Pause
import com.buzbuz.smartautoclicker.database.domain.Action.Swipe

/**
 * Execute the actions of an event.
 * Execute the list of actions provided by [executeActions]. The state of the executor is provided by [state].
 */
internal class ActionExecutor {

    /** The states of the [ActionExecutor]. */
    enum class State {
        /** The executor is idle, waiting for its next action the execute. */
        IDLE,
        /** The executor is currently executing an action. */
        EXECUTING,
    }

    /** Handler on the main thread. */
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
    /** Handler on the worker thread. */
    private val workerThreadHandler: Handler by lazy {
        Handler(Looper.myLooper()!!)
    }

    /** The executor for the actions requiring a gesture on the user screen. */
    var onGestureExecutionListener: ((GestureDescription) -> Unit)? = null
    /** The current state the this action executor. */
    var state: State = State.IDLE
        private set

    /**
     * Execute the provided actions.
     * @param actions the actions to be executed.
     */
    @WorkerThread
    fun executeActions(actions: List<Action>) {
        if (actions.isEmpty()) {
            state = State.IDLE
            return
        }

        state = State.EXECUTING

        actions.forEachIndexed { index, action ->
            when (action) {
                is Click -> executeClick(action)
                is Swipe -> executeSwipe(action)
                is Pause -> {
                    val actionsLeft = actions.subList(index + 1, actions.size)
                    workerThreadHandler.postDelayed({
                        executeActions(actionsLeft)
                    }, action.pauseDuration!!)
                    return
                }
            }
        }

        state = State.IDLE
    }

    /**
     * Execute the provided click.
     * @param click the click to be executed.
     */
    @WorkerThread
    private fun executeClick(click: Click) {
        val clickPath = Path()
        val clickBuilder = GestureDescription.Builder()

        clickPath.moveTo(click.x!!.toFloat(), click.y!!.toFloat())
        clickBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, click.pressDuration!!))

        mainThreadHandler.post {
            onGestureExecutionListener?.invoke(clickBuilder.build())
        }
    }

    /**
     * Execute the provided swipe.
     * @param swipe the swipe to be executed.
     */
    @WorkerThread
    private fun executeSwipe(swipe: Swipe) {
        val swipePath = Path()
        val clickBuilder = GestureDescription.Builder()

        swipePath.moveTo(swipe.fromX!!.toFloat(), swipe.fromY!!.toFloat())
        swipePath.lineTo(swipe.toX!!.toFloat(), swipe.toY!!.toFloat())
        clickBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, swipe.swipeDuration!!))

        mainThreadHandler.post {
            onGestureExecutionListener?.invoke(clickBuilder.build())
        }
    }
}