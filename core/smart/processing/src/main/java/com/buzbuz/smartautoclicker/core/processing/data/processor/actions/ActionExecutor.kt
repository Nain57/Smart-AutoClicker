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
package com.buzbuz.smartautoclicker.core.processing.data.processor.actions

import android.accessibilityservice.AccessibilityService
import android.content.Intent as AndroidIntent

import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationRequest
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.common.actions.text.replaceCounterReferences
import com.buzbuz.smartautoclicker.core.common.actions.utils.getPauseDurationMs
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release.Hold
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.putDomainExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
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
 * @param currentTimeMs provides the current time in milliseconds, used to time the [Hold.ForDuration].
 */
internal class ActionExecutor(
    private val androidExecutor: AndroidActionExecutor,
    private val processingState: ProcessingState,
    randomize: Boolean,
    private val unblockWorkaroundEnabled: Boolean = false,
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
) {

    init { androidExecutor.resetState() }

    private val random: Random? =
        if (randomize) Random(System.currentTimeMillis()) else null

    private val gestureBuilder: AndroidGestureBuilder = AndroidGestureBuilder.createNewBuilder(random)
    private val fingerResolver: FingerResolver = FingerResolver(processingState, gestureBuilder)


    suspend fun onScenarioLoopFinished() {
        if (!unblockWorkaroundEnabled) return

        // The unblock gesture continues nothing, so the system would cancel the held pointer to dispatch it.
        if (androidExecutor.getHeldPointer() != null) return

        withContext(Dispatchers.Main) {
            androidExecutor.dispatchPixelWorkaroundGestureIfNeeded()
        }
    }

    /**
     * Execute all the actions of an event.
     *
     * A [Hold.ForDuration] can elapse while the actions are executed. The executor doesn't release it by itself, it
     * only tells when it is time to, through [onHoldDurationElapsed]: before each action, and during a pause.
     *
     * @param event the event to execute the actions of.
     * @param results the results of the conditions detection for this event, if any.
     * @param onHoldDurationElapsed called when the time of the [Hold.ForDuration] of the held pointer has elapsed.
     */
    suspend fun executeActions(
        event: Event,
        results: ConditionsResults? = null,
        onHoldDurationElapsed: suspend () -> Unit = {},
    ) {
        event.actions.forEach { action ->
            if (isHeldDurationElapsed()) onHoldDurationElapsed()

            when (action) {
                is Click -> executeClick(event, action, results)
                is Gesture -> executeGesture(event, action, results)
                is Pause -> executePause(action, onHoldDurationElapsed)
                is Intent -> executeIntent(action)
                is ToggleEvent -> executeToggleEvent(action)
                is ChangeCounter -> executeChangeCounter(action, results)
                is Notification -> executeNotification(event, action)
                is SystemAction -> executeSystemAction(action)
                is SetText -> executeSetText(action)
            }
        }
    }

    /**
     * The event owning the pointer currently held on the screen, or null if there is none.
     *
     * It is read from the held pointer itself, so it follows whatever happened to it: taken over by another event,
     * dropped by the system after a cancelled gesture, or released.
     */
    fun getHeldEvent(): Event? =
        getHeldGesture()?.event

    fun isEventHeld(event: Event): Boolean =
        getHeldEvent()?.id == event.id

    /** The hold of the pointer currently held on the screen, or null if there is none. */
    fun getHeldGesture(): GestureHold? =
        androidExecutor.getHeldPointer()?.owner as? GestureHold

    /** @return true if the pointer currently held is a [Hold.ForDuration] whose time has elapsed. */
    fun isHeldDurationElapsed(): Boolean =
        getHeldDurationRemainingMs() == 0L

    private fun getHeldDurationRemainingMs(): Long? =
        getHeldGesture()?.getRemainingDurationMs(currentTimeMs())

    /**
     * Verify the pointer a gesture is holding on the screen, if any, has not been lifted by the system.
     *
     * Called on every frame while a pointer is held, and again right before the pointer is used: the user can touch the
     * screen while the frame is being detected. The executor only compares the start of the hold with the last user
     * touch, so it stays on the processing thread.
     *
     * @return true if it is still held, false if it has been lost or if nothing was held.
     */
    fun checkHeldFinger(): Boolean =
        androidExecutor.getHeldPointer() != null && androidExecutor.checkHeldPointer()

    /**
     * Lift the pointer a gesture is holding on the screen, if any. A pointer lifted by the user is already up.
     *
     * @return true if a pointer was released, false if it has been lost or if nothing was held.
     */
    suspend fun releaseHeldFinger(): Boolean {
        if (!checkHeldFinger()) return false

        withContext(Dispatchers.Main) {
            androidExecutor.releaseHeldPointer()
        }
        return true
    }

    private suspend fun executeClick(event: Event, click: Click, results: ConditionsResults?) {
        val resolvedClickFinger = fingerResolver.resolve(click, event, results) ?: return
        dispatchResolvedFingers(listOf(resolvedClickFinger), click.name, GestureHold(event, Hold.None))
    }

    private suspend fun executeGesture(event: Event, gesture: Gesture, results: ConditionsResults?) {
        // Only the first finger of a gesture can be held
        val hold = (gesture.fingers.firstOrNull()?.motions?.lastOrNull() as? GestureFingerMotion.Release)?.hold
            ?: Hold.None

        dispatchResolvedFingers(
            fingers = fingerResolver.resolve(gesture, event, results),
            gestureName = gesture.name,
            gestureHold = GestureHold(event, hold),
        )
    }

    /**
     * Build and dispatch a gesture from its resolved fingers, carrying along the pointer a previous gesture is
     * holding, if any.
     *
     * The system only honours a continuation when the first step of the dispatched gesture holds exactly one stroke
     * for each pointer currently down, so the held pointer has to be part of every gesture executed while it is held.
     * The builder adds it as an extra finger: a gesture using all of its own fingers while one is held puts six
     * pointers down at the same time.
     *
     * A pointer held by this gesture is owned by [gestureHold], see [getHeldGesture]. Its hold starts once the gesture
     * is fully dispatched. The held pointer is checked again first: a gesture continuing a pointer lifted by the user
     * would be refused, or silently misplayed on recent Android versions.
     */
    private suspend fun dispatchResolvedFingers(
        fingers: List<ResolvedFinger>,
        gestureName: String?,
        gestureHold: GestureHold,
    ) {
        checkHeldFinger()
        val builtGesture = gestureBuilder
            .build(fingers, gestureName, androidExecutor.getHeldPointer(), gestureHold) ?: return

        withContext(Dispatchers.Main) {
            androidExecutor.dispatchBuiltGesture(builtGesture)
        }

        if (getHeldGesture() === gestureHold) gestureHold.onHoldStarted(currentTimeMs())
    }

    /**
     * Execute the provided pause.
     *
     * A [Hold.ForDuration] elapsing during the pause is notified on time through [onHoldDurationElapsed], instead of
     * once the pause is over.
     *
     * @param pause the pause to be executed.
     */
    private suspend fun executePause(pause: Pause, onHoldDurationElapsed: suspend () -> Unit) {
        val pauseDurationMs = pause.pauseDuration!!.getPauseDurationMs(random)
        val holdRemainingMs = getHeldDurationRemainingMs()

        if (holdRemainingMs == null || holdRemainingMs >= pauseDurationMs) {
            delay(pauseDurationMs.milliseconds)
            return
        }

        delay(holdRemainingMs.milliseconds)
        onHoldDurationElapsed()
        delay((pauseDurationMs - holdRemainingMs).milliseconds)
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
     *
     * A [CounterOperationValue.Condition] referencing a condition that has not been detected (possible with the OR
     * operator) leaves the counter untouched.
     *
     * @param changeCounter the changeCounter action to be executed.
     * @param results the results of the conditions detection for this event, if any.
     */
    private fun executeChangeCounter(changeCounter: ChangeCounter, results: ConditionsResults?) {
        val oldValue = processingState.getCounterValue(changeCounter.counterName) ?: return

        val operandValue = when (val operationValue = changeCounter.operationValue) {
            is CounterOperationValue.Counter -> processingState.getCounterValue(operationValue.counterName) ?: 0.0
            is CounterOperationValue.Number -> operationValue.value
            is CounterOperationValue.Condition -> results?.getDetectedConditionCoordinate(operationValue) ?: return
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

/** Waiting delay after a start activity to avoid overflowing the system. */
private const val INTENT_START_ACTIVITY_DELAY = 1000L
/** Waiting delay after a broadcast to avoid overflowing the system. */
private const val INTENT_BROADCAST_DELAY = 100L
