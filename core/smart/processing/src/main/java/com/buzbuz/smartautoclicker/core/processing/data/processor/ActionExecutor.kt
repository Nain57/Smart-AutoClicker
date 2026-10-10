/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent as AndroidIntent
import android.graphics.Path
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.workarounds.UnblockGestureScheduler
import com.buzbuz.smartautoclicker.core.base.workarounds.buildUnblockGesture
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.common.actions.gesture.buildSingleStroke
import com.buzbuz.smartautoclicker.core.common.actions.gesture.moveTo
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationRequest
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.common.actions.text.replaceCounterReferences
import com.buzbuz.smartautoclicker.core.common.actions.utils.getPauseDurationMs
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.putDomainExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Execute the actions of an event.
 *
 * @param androidExecutor the executor for the actions requiring an interaction with Android.
 * @param processingState the state of the current processing (counters, enabled events...).
 * @param randomize true to randomize the actions values a bit (positions, timers...), false to be precise.
 */
internal class ActionExecutor(
    private val androidExecutor: AndroidActionExecutor,
    private val processingState: ProcessingState,
    randomize: Boolean,
    unblockWorkaroundEnabled: Boolean = false,
) {

    init { androidExecutor.resetState() }

    private val random: Random? =
        if (randomize) Random(System.currentTimeMillis()) else null

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null


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

    suspend fun executeActions(event: Event, results: ConditionsResults? = null) {
        event.actions.forEach { action ->
            when (action) {
                is Click -> executeClick(event, action, results)
                is Gesture -> executeGesture(event, action, results)
                is Pause -> executePause(action)
                is Intent -> executeIntent(action)
                is ToggleEvent -> executeToggleEvent(action)
                is ChangeCounter -> executeChangeCounter(action)
                is Notification -> executeNotification(event, action)
                is SystemAction -> executeSystemAction(action)
                is SetText -> executeSetText(action)
            }
        }
    }

    private suspend fun executeClick(event: Event, click: Click, results: ConditionsResults?) {
        val basePosition = when (val position = click.position) {
            is GesturePosition.Coordinates -> getUserSelectedClickPosition(position)
            is GesturePosition.Condition -> getDetectedConditionClickPosition(event, position, results)
            null -> null
        } ?: return

        val offset = getClickOffset(click.offset)
        val clickPath = Path().apply {
            moveTo(
                position = Point(basePosition.x + offset.x, basePosition.y + offset.y),
                random = random,
            )
        }

        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = clickPath,
            durationMs = click.pressDuration!!,
            random = random,
        )

        withContext(Dispatchers.Main) {
            androidExecutor.dispatchGesture(clickGesture)
        }
    }

    private fun getUserSelectedClickPosition(position: GesturePosition.Coordinates): Point? {
        val x = resolveCoordinateValue(position.x) ?: return null
        val y = resolveCoordinateValue(position.y) ?: return null
        return Point(x, y)
    }

    private fun resolveCoordinateValue(coordinatesValue: GestureCoordinatesValue): Int? =
        when (coordinatesValue) {
            is GestureCoordinatesValue.Literal ->
                coordinatesValue.value
            is GestureCoordinatesValue.Counter ->
                processingState.getCounterValue(coordinatesValue.counterName)?.toInt()
        }

    private fun getDetectedConditionClickPosition(
        event: Event,
        position: GesturePosition.Condition,
        results: ConditionsResults?,
    ): Point? {
        if (event !is ScreenEvent) return null

        val conditionId = position.conditionId
        val result = when {
            event.conditionOperator == OR -> results?.getFirstScreenConditionDetectedResult()
            conditionId != null -> results?.getScreenConditionResult(conditionId.databaseId)
            else -> null
        }

        if (result == null) {
            Log.w(TAG, "Click is invalid, can't execute")
            return null
        }

        return Point(result.position?.x ?: 0, result.position?.y ?: 0)
    }

    /** An unresolvable offset axis (no offset, or a counter that doesn't exist) falls back to 0. */
    private fun getClickOffset(clickOffset: GesturePosition?): Point {
        val offset = clickOffset as? GesturePosition.Coordinates ?: return Point(0, 0)
        return Point(
            resolveCoordinateValue(offset.x) ?: 0,
            resolveCoordinateValue(offset.y) ?: 0,
        )
    }

    private suspend fun executeGesture(event: Event, gesture: Gesture, results: ConditionsResults?) {
        val gestureChains = AndroidGestureBuilder
            .createNewBuilder(processingState, random)
            .build(gesture, event, results) ?: return

        withContext(Dispatchers.Main) {
            gestureChains.forEach { chain -> androidExecutor.dispatchGestureChain(chain) }
        }
    }

    /**
     * Execute the provided pause.
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause) {
        delay(pause.pauseDuration!!.getPauseDurationMs(random).milliseconds)
    }

    /**
     * Execute the provided intent.
     * @param intent the intent to be executed.
     */
    private suspend fun executeIntent(intent: Intent) {
        val androidIntent = AndroidIntent().apply {
            action = intent.intentAction!!
            flags = intent.flags!!

            intent.componentName?.let {
                component = intent.componentName
            }

            intent.extras?.forEach { putDomainExtra(it) }
        }

        if (intent.isBroadcast) {
            withContext(Dispatchers.Main) {
                androidExecutor.sendBroadcast(androidIntent)
            }
            delay(INTENT_BROADCAST_DELAY.milliseconds)
        } else {
            withContext(Dispatchers.Main) {
                androidExecutor.startActivity(androidIntent)
            }
            delay(INTENT_START_ACTIVITY_DELAY.milliseconds)
        }
    }

    /**
     * Execute the provided toggle event.
     * @param toggleEvent the toggleEvent to be executed.
     */
    private fun executeToggleEvent(toggleEvent: ToggleEvent) {
        if (toggleEvent.toggleAll) {
            when (toggleEvent.toggleAllType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableAll()
                ToggleEvent.ToggleType.DISABLE -> processingState.disableAll()
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleAll()
                null -> Unit
            }

            return
        }

        toggleEvent.eventToggles.forEach { eventToggle ->
            when (eventToggle.toggleType) {
                ToggleEvent.ToggleType.ENABLE -> processingState.enableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.DISABLE -> processingState.disableEvent(eventToggle.targetEventId!!.databaseId)
                ToggleEvent.ToggleType.TOGGLE -> processingState.toggleEvent(eventToggle.targetEventId!!.databaseId)
            }
        }
    }

    /**
     * Execute the provided change counter.
     * @param changeCounter the changeCounter action to be executed.
     */
    private fun executeChangeCounter(changeCounter: ChangeCounter) {
        val oldValue = processingState.getCounterValue(changeCounter.counterName) ?: return

        val operandValue = when (val operationValue = changeCounter.operationValue) {
            is CounterOperationValue.Counter -> processingState.getCounterValue(operationValue.value) ?: 0.0
            is CounterOperationValue.Number -> operationValue.value
        }

        processingState.setCounterValue(
            counterName = changeCounter.counterName,
            value = when (changeCounter.operation) {
                ChangeCounter.OperationType.ADD -> oldValue + operandValue
                ChangeCounter.OperationType.MINUS -> oldValue - operandValue
                ChangeCounter.OperationType.SET -> operandValue
            }
        )
    }

    private fun executeNotification(event: Event, notification: Notification) {
        val counters = buildMap {
            notification.messageText.findCounterReferences().forEach { counterName ->
                processingState.getCounterValue(counterName)?.let { counterValue ->
                    put(counterName, counterValue)
                }
            }
        }

        androidExecutor.postNotification(
            ActionNotificationRequest(
                actionId = notification.id.databaseId,
                title = notification.name ?: "Klick'r",
                message = notification.messageText.replaceCounterReferences(counters),
                eventId = event.id.databaseId,
                groupName = event.name,
                importance = notification.channelImportance,
            )
        )
    }

    private suspend fun executeSystemAction(action: SystemAction) {
        val globalAction = when (action.type) {
            SystemAction.Type.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            SystemAction.Type.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            SystemAction.Type.RECENT_APPS -> AccessibilityService.GLOBAL_ACTION_RECENTS
        }

        withContext(Dispatchers.Main) {
            androidExecutor.performGlobalAction(globalAction)
        }
    }

    private suspend fun executeSetText(action: SetText) {
        val counters = buildMap {
            action.text.findCounterReferences().forEach { counterName ->
                processingState.getCounterValue(counterName)?.let { counterValue ->
                    put(counterName, counterValue)
                }
            }
        }

        withContext(Dispatchers.Main) {
            androidExecutor.writeTextOnFocusedItem(
                text = action.text.replaceCounterReferences(counters),
                validate = action.validateInput,
            )
        }
    }
}

/** Tag for logs. */
private const val TAG = "ActionExecutor"
/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L
