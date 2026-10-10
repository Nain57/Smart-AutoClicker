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

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.actions.ActionExecutor
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.domain.EventType
import com.buzbuz.smartautoclicker.core.processing.domain.HoldReleaseReason
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [ScreenEvent] on it.
 *
 * @param imageDetector the detector for images.
 * @param randomize true to randomize the actions values a bit to avoid being taken for a bot.
 * @param screenEvents the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param androidExecutor execute the actions requiring an interaction with Android.
 * @param onStopRequested called when an end condition of the scenario have been reached or all events are disabled.
 * @param progressListener the object to notify for detection progress. Can be null if not required.
 * @param processingState handle the processing state of the scenario.
 * @param conditionsVerifier check conditions and tell if they are fulfilled.
 * @param actionExecutor execute the detected event actions.
 */
internal class ScenarioProcessor(
    private val processingTag: String,
    private val imageDetector: ImageDetector,
    scalingManager: ScalingManager,
    randomize: Boolean,
    screenEvents: List<ScreenEvent>,
    triggerEvents: List<TriggerEvent>,
    counters: List<Counter>,
    androidExecutor: AndroidActionExecutor,
    unblockWorkaroundEnabled: Boolean = false,
    private val bitmapSupplier: suspend (String, Int, Int) -> Bitmap?,
    private val onStopRequested: () -> Unit,
    private val progressListener: SmartProcessingListener?,
    @get:VisibleForTesting internal val processingState: ProcessingState = ProcessingState(
        screenEvents = screenEvents,
        triggerEvents = triggerEvents,
        counters = counters,
        progressListener = progressListener,
    ),
    private val conditionsVerifier: ConditionsVerifier = ConditionsVerifier(
        state = processingState,
        imageDetector = imageDetector,
        scalingManager = scalingManager,
        bitmapSupplier = bitmapSupplier,
        progressListener = progressListener,
    ),
    private val actionExecutor: ActionExecutor = ActionExecutor(
        androidExecutor = androidExecutor,
        processingState = processingState,
        randomize = randomize,
        unblockWorkaroundEnabled = unblockWorkaroundEnabled,
    ),
) {

    /** The last held event notified to the [progressListener]. */
    private var notifiedHeldEvent: Event? = null

    fun onScenarioStart(context: Context) {
        processingState.onProcessingStarted(context)
    }

    suspend fun onScreenRotation() {
        releaseHeldEvent(HoldReleaseReason.ScreenRotation)
    }

    suspend fun onScenarioEnd() {
        releaseHeldEvent(HoldReleaseReason.ScenarioStopped)
        processingState.onProcessingStopped()
    }

    /**
     * Find an event with the conditions fulfilled on the current image.
     *
     * @param screenFrame the bitmap containing the current screen display.
     *
     * @return the first Event with all conditions fulfilled, or null if none has been found.
     */
    suspend fun process(screenFrame: Bitmap) {
        // No more events enabled, there is nothing more to do. Stop the detection.
        if (processingState.areAllEventsDisabled()) {
            onStopRequested()
            return
        }

        // If there is a held event that has been disabled, release the held finger. Otherwise, verify the user didn't
        // lift it by touching the screen: the system already released it, only its cooldown remains to be started.
        val heldEvent = actionExecutor.getHeldEvent()
        if (heldEvent != null) {
            if (!processingState.isEventEnabled(heldEvent.id.databaseId)) {
                Log.d(TAG, "Held event ${heldEvent.name} is disabled, releasing it")
                releaseHeldEvent(HoldReleaseReason.EventDisabled, cooldown = false)
            } else if (!actionExecutor.checkHeldFinger()) {
                Log.d(TAG, "Held event ${heldEvent.name} finger has been lifted by the user")
                startHeldEventCooldown(heldEvent)
            }
        }
        notifyHeldEventChangeIfNeeded(actionExecutor.getHeldEvent(), HoldReleaseReason.Interrupted)

        // Handle all trigger events enabled during previous processing
        if (!processingState.areAllTriggerEventsDisabled()) {
            progressListener?.onEventsListProcessingStarted(EventType.Trigger)
            processTriggerEvents()
            progressListener?.onEventsProcessingCompleted(EventType.Trigger)
        }

        // Reset any values that needs to be reset for each iteration
        // After the triggers to let them handle changes, before the image processing to start capturing values before
        processingState.clearIterationState()

        // Handle the image detection
        if (!processingState.areAllScreenEventsDisabled()) {
            progressListener?.onEventsListProcessingStarted(EventType.Screen)
            processScreenEvents(screenFrame)
            progressListener?.onEventsProcessingCompleted(EventType.Screen)
        }

        // Loop is completed
        actionExecutor.onScenarioLoopFinished()

        return
    }

    private suspend fun processTriggerEvents() {
        // First, if there is a trigger event that keep a gesture held, verify it
        (actionExecutor.getHeldEvent() as? TriggerEvent)?.let { heldEvent ->
            releaseHeldEventIfUnfulfilled(heldEvent)
        }

        for (triggerEvent in processingState.getTriggerEvents()) {
            // Enabled state of the event might have changed during the loop
            if (!processingState.isEventEnabled(triggerEvent.id.databaseId)) continue
            // No conditions ? This should not happen, skip this event
            if (triggerEvent.conditions.isEmpty()) continue
            // Event has a gesture currently held, skip interpretation
            if (actionExecutor.isEventHeld(triggerEvent)) continue

            val results = verifyConditions(triggerEvent)
            if (results.fulfilled  == true) {
                executeEventActions(triggerEvent, results)
            }
        }
    }

    private suspend fun processScreenEvents(screenFrame: Bitmap) {
        // Set the current screen image
        imageDetector.setScreenBitmap(screenFrame, processingTag)

        try {
            // First, if there is a screen event that keep a gesture held, verify it
            (actionExecutor.getHeldEvent() as? ScreenEvent)?.let { heldEvent ->
                releaseHeldEventIfUnfulfilled(heldEvent)
            }

            // Check all events
            for (screenEvent in processingState.getScreenEvents()) {
                // No conditions ? This should not happen, skip this event
                if (screenEvent.conditions.isEmpty()) continue
                // Enabled state of the event might have changed during the loop
                if (!processingState.isEventEnabled(screenEvent.id.databaseId)) continue
                // Event has a gesture currently held, skip interpretation
                if (actionExecutor.isEventHeld(screenEvent)) continue
                // Event is under cooldown, skip it
                if (processingState.isCooldownRunning(screenEvent)) continue

                val results = verifyConditions(screenEvent)
                if (results.fulfilled == true) {
                    executeEventActions(screenEvent, results)
                    if (!screenEvent.keepDetecting) break
                }

                // Stop processing if requested
                yield()
            }
        } finally {
            // We are done processing this frame, release it
            imageDetector.releaseScreenBitmap(screenFrame)
        }
    }

    private suspend fun verifyConditions(event: Event): ConditionsResults {
        progressListener?.onEventProcessingStarted(event)
        val results = conditionsVerifier.verifyConditions(
            operator = event.conditionOperator,
            conditions = event.conditions,
        )
        progressListener?.onEventProcessingCompleted(
            event = event,
            fulfilled = results.fulfilled == true,
            results = when (event) {
                is ScreenEvent -> results.getAllScreenConditionsResults()
                is TriggerEvent -> results.getAllTriggerConditionsResults()
            }
        )

        return results
    }

    /**
     * Execute the actions of a fulfilled event.
     *
     * A gesture of the event holding a finger takes the hold over from the previously held event, if any. For an event
     * left holding a finger, cooldown starts only once the finger is released.
     */
    private suspend fun executeEventActions(event: Event, results: ConditionsResults) {
        val previousHeldEvent = actionExecutor.getHeldEvent()
        val executionStartTimeMs = System.currentTimeMillis()

        // The system may have dropped the held pointer since the last notification: notify it before this event
        // holds again, or a new hold by the same event would be seen as the continuation of the lost one.
        notifyHeldEventChangeIfNeeded(previousHeldEvent, HoldReleaseReason.Interrupted)

        actionExecutor.executeActions(event, results)
        progressListener?.onEventActionsExecuted(
            event = event,
            results = when (event) {
                is ScreenEvent -> results.getAllScreenConditionsResults()
                is TriggerEvent -> results.getAllTriggerConditionsResults()
            },
        )

        // The previously held event lost its hold to this one, its cooldown started with the execution
        val heldEvent = actionExecutor.getHeldEvent()
        if (previousHeldEvent is Event && heldEvent?.id != previousHeldEvent.id) {
            startHeldEventCooldown(previousHeldEvent, executionStartTimeMs)
        }

        // New held pointer, delay cooldown to release time (no cooldown on trigger)
        notifyHeldEventChangeIfNeeded(heldEvent, HoldReleaseReason.OtherEvent)

        if (event !is ScreenEvent || (heldEvent?.id != previousHeldEvent?.id && heldEvent?.id == event.id)) return
        startHeldEventCooldown(event)
    }

    private suspend fun releaseHeldEventIfUnfulfilled(heldEvent: Event) {
        progressListener?.onEventProcessingStarted(heldEvent)
        val results = conditionsVerifier.verifyConditions(
            operator = heldEvent.conditionOperator,
            conditions = heldEvent.conditions,
        )
        progressListener?.onEventProcessingCompleted(
            event = heldEvent,
            fulfilled = results.fulfilled == true,
            results = when (heldEvent) {
                is ScreenEvent -> results.getAllScreenConditionsResults()
                is TriggerEvent -> results.getAllTriggerConditionsResults()
            },
            isHeld = true,
        )

        if (results.fulfilled != true) {
            Log.d(TAG, "Held event ${heldEvent.name} is unfulfilled, releasing it; results=$results")
            releaseHeldEvent(HoldReleaseReason.Unfulfilled)
        }
    }

    private suspend fun releaseHeldEvent(reason: HoldReleaseReason, cooldown: Boolean = true) {
        val heldEvent = actionExecutor.getHeldEvent() ?: return
        actionExecutor.releaseHeldFinger()

        if (cooldown) startHeldEventCooldown(heldEvent)
        notifyHeldEventChangeIfNeeded(null, reason)
    }

    /** Cooldown of an event holding a finger starts once it is released. */
    private fun startHeldEventCooldown(heldEvent: Event, startTimeMs: Long = System.currentTimeMillis()) {
        if (heldEvent is ScreenEvent) processingState.startCooldownIfNeeded(heldEvent, startTimeMs)
    }

    /**
     * Notify the held event to the [progressListener] if it has changed since the last notification.
     * @param releaseReason the reason of the release of the previously notified held event, if any.
     */
    private fun notifyHeldEventChangeIfNeeded(heldEvent: Event?, releaseReason: HoldReleaseReason) {
        if (heldEvent?.id == notifiedHeldEvent?.id) return

        notifiedHeldEvent?.let { previousHeldEvent ->
            progressListener?.onHeldEventReleased(previousHeldEvent, releaseReason)
        }
        notifiedHeldEvent = heldEvent
        progressListener?.onHeldEventChanged(heldEvent)
    }
}

private const val TAG = "ScenarioProcessor"
