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
package com.buzbuz.smartautoclicker.core.processing.data

import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.domain.model.action.putDomainExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.processing.data.processor.ProcessingResults

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Execute the actions of an event.
 *
 * @param androidExecutor the executor for the actions requiring an interaction with Android.
 * @param scenarioEditor the executor for the actions modifying the scenario processing.
 * @param randomize true to randomize the actions values a bit (positions, timers...), false to be precise.
 */
internal class ActionExecutor(
    private val androidExecutor: AndroidExecutor,
    private val scenarioEditor: ScenarioEditor,
    private val randomize: Boolean,
) {

    private val random = Random(System.currentTimeMillis())

    /**
     * Execute the provided actions.
     * @param actions the actions to be executed.
     * @param processingResults contains the detection results for actions that needs context.
     */
    suspend fun executeActions(event: Event, actions: List<Action>, processingResults: ProcessingResults) {
        actions.forEach { action ->
            when (action) {
                is Click -> executeClick(event, action, processingResults)
                is Swipe -> executeSwipe(action)
                is Pause -> executePause(action)
                is Action.Intent -> executeIntent(action)
                is ToggleEvent -> executeToggleEvent(action)
            }
        }
    }

    /**
     * Execute the provided click.
     * @param click the click to be executed.
     */
    private suspend fun executeClick(event: Event, click: Click, processingResults: ProcessingResults) {
        val clickPath = Path()
        val clickBuilder = GestureDescription.Builder()

        when (click.positionType) {
            Click.PositionType.USER_SELECTED ->
                clickPath.moveTo(click.x!!, click.y!!, randomize)

            Click.PositionType.ON_DETECTED_CONDITION -> {
                val result = when {
                    event.conditionOperator == OR ->
                        processingResults.getFirstMatchResult()
                    click.clickOnConditionId != null ->
                        processingResults.getResult(click.eventId.databaseId, click.clickOnConditionId!!.databaseId)
                    else -> null
                }

                if (result == null) {
                    Log.w(TAG, "Click is invalid, can't execute")
                    return
                }

                clickPath.moveTo(result.position.x, result.position.y, randomize)
            }
        }

        clickBuilder.addStroke(
            GestureDescription.StrokeDescription(
                clickPath,
                0,
                if (randomize) random.getRandomizedGestureDuration(click.pressDuration!!) else click.pressDuration!!,
            )
        )

        withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(clickBuilder.build())
        }
    }

    /**
     * Execute the provided swipe.
     * @param swipe the swipe to be executed.
     */
    private suspend fun executeSwipe(swipe: Swipe) {
        val swipePath = Path()
        val swipeBuilder = GestureDescription.Builder()

        swipePath.moveTo(swipe.fromX!!, swipe.fromY!!, randomize)
        swipePath.lineTo(swipe.toX!!, swipe.toY!!, randomize)
        swipeBuilder.addStroke(
            GestureDescription.StrokeDescription(
                swipePath,
                0,
                if (randomize) random.getRandomizedGestureDuration(swipe.swipeDuration!!) else swipe.swipeDuration!!,
            )
        )

        withContext(Dispatchers.Main) {
            androidExecutor.executeGesture(swipeBuilder.build())
        }
    }

    /**
     * Execute the provided pause.
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause) {
        delay(if (randomize) random.getRandomizedDuration(pause.pauseDuration!!) else pause.pauseDuration!!)
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

            intent.extras?.forEach { putDomainExtra(it) }
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

    /**
     * Execute the provided toggle event.
     * @param toggleEvent the toggleEvent to be executed.
     */
    private fun executeToggleEvent(toggleEvent: ToggleEvent) {
        scenarioEditor.changeEventState(toggleEvent.toggleEventId!!.databaseId, toggleEvent.toggleEventType!!)
    }

    private fun Path.moveTo(x: Int, y: Int, randomize: Boolean) {
        if (!randomize) moveTo(x.toFloat(), y.toFloat())
        else moveTo(random.getRandomizedPosition(x), random.getRandomizedPosition(y))
    }

    private fun Path.lineTo(x: Int, y: Int, randomize: Boolean) {
        if (!randomize) lineTo(x.toFloat(), y.toFloat())
        else lineTo(random.getRandomizedPosition(x), random.getRandomizedPosition(y))
    }

    private fun Random.getRandomizedPosition(position: Int): Float = nextInt(
        from = max(position - RANDOMIZATION_POSITION_MAX_OFFSET_PX, 0),
        until = position + RANDOMIZATION_POSITION_MAX_OFFSET_PX + 1,
    ).toFloat()

    private fun Random.getRandomizedDuration(duration: Long): Long = nextLong(
        from = max(duration - RANDOMIZATION_DURATION_MAX_OFFSET_MS, 1),
        until = duration + RANDOMIZATION_DURATION_MAX_OFFSET_MS + 1,
    )

    private fun Random.getRandomizedGestureDuration(duration: Long): Long = nextLong(
        from = max(duration - RANDOMIZATION_DURATION_MAX_OFFSET_MS, 1),
        until = min(duration + RANDOMIZATION_DURATION_MAX_OFFSET_MS + 1, GESTURE_DURATION_MAX_VALUE),
    )
}

/** Execute the actions related to Android. */
interface AndroidExecutor {

    /** Execute the provided gesture. */
    suspend fun executeGesture(gestureDescription: GestureDescription)

    /** Start the activity defined by the provided intent. */
    fun executeStartActivity(intent: Intent)

    /** Send a broadcast defined by the provided intent. */
    fun executeSendBroadcast(intent: Intent)
}

/** Execute the actions related to the scenario processing modifications. */
interface ScenarioEditor {

    /** Change the enable state of an event. */
    fun changeEventState(eventId: Long, toggleType: ToggleEvent.ToggleType)
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"
/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L
/** */
private const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
/** */
private const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5