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
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.workarounds.UnblockGestureScheduler
import com.buzbuz.smartautoclicker.core.base.workarounds.buildUnblockGesture
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.common.actions.gesture.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.gesture.buildSingleStroke
import com.buzbuz.smartautoclicker.core.common.actions.gesture.moveTo
import com.buzbuz.smartautoclicker.core.common.actions.utils.getPauseDurationMs
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class DumbActionExecutor @Inject constructor(
    private val androidExecutor: AndroidActionExecutor,
) {

    private val random: Random = Random(System.currentTimeMillis())
    private var randomize: Boolean = false

    private var unblockWorkaroundEnabled: Boolean = false

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null

    fun setUnblockWorkaround(isEnabled: Boolean) {
        unblockWorkaroundEnabled = isEnabled
    }

    suspend fun onScenarioLoopFinished() {
        if (unblockGestureScheduler?.shouldTrigger() == true) {
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Injecting unblock gesture")
                androidExecutor.dispatchGesture(
                    GestureDescription.Builder().buildUnblockGesture()
                )
            }
        }
    }

    suspend fun executeDumbAction(action: DumbAction, randomize: Boolean) {
        this.randomize = randomize
        when (action) {
            is DumbAction.DumbClick -> executeDumbClick(action)
            is DumbAction.DumbGesture -> executeDumbGesture(action)
            is DumbAction.DumbPause -> executeDumbPause(action)
        }
    }

    private suspend fun executeDumbGesture(dumbGesture: DumbAction.DumbGesture) {
        val gestureBuilder = AndroidGestureBuilder.createNewBuilder(if (randomize) random else null)
        val gestureChains = gestureBuilder
            .build(dumbGesture.toResolvedFingers(gestureBuilder), dumbGesture.name) ?: return

        dumbGesture.repeat {
            withContext(Dispatchers.Main) {
                gestureChains.forEach { chain -> androidExecutor.dispatchGestureChain(chain) }
            }
        }
    }

    private suspend fun executeDumbClick(dumbClick: DumbAction.DumbClick) {
        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply { moveTo(dumbClick.position, random) },
            durationMs = dumbClick.pressDurationMs,
            random = random,
        )

        executeRepeatableGesture(clickGesture, dumbClick)
    }

    private suspend fun executeDumbPause(dumbPause: DumbAction.DumbPause) {
        delay(dumbPause.pauseDurationMs.getPauseDurationMs(random).milliseconds)
    }

    private suspend fun executeRepeatableGesture(gesture: GestureDescription, repeatable: Repeatable) {
        repeatable.repeat {
            withContext(Dispatchers.Main) {
                androidExecutor.dispatchGesture(gesture)
            }
        }
    }
}

private const val TAG = "DumbActionExecutor"