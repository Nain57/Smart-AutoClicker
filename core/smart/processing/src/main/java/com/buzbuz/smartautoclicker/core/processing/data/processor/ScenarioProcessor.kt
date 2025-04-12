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
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.detection.ScreenDetector
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [ScreenEvent] on it.
 *
 * @param screenDetector the detector for images.
 * @param detectionQuality the quality of the detection.
 * @param randomize true to randomize the actions values a bit to avoid being taken for a bot.
 * @param screenEvents the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param androidExecutor execute the actions requiring an interaction with Android..
 * @param onStopRequested called when a end condition of the scenario have been reached or all events are disabled.
 * @param progressListener the object to notify for detection progress. Can be null if not required.
 */
internal class ScenarioProcessor(
    private val processingTag: String,
    private val screenDetector: ScreenDetector,
    private val detectionQuality: Int,
    randomize: Boolean,
    screenEvents: List<ScreenEvent>,
    triggerEvents: List<TriggerEvent>,
    private val bitmapSupplier: suspend (ImageCondition) -> Bitmap?,
    androidExecutor: SmartActionExecutor,
    unblockWorkaroundEnabled: Boolean = false,
    private val onStopRequested: () -> Unit,
    private val progressListener: ScenarioProcessingListener? = null,
) {

    /** Handle the processing state of the scenario. */
    @VisibleForTesting internal val processingState: ProcessingState = ProcessingState(screenEvents, triggerEvents)
    /** Check conditions and tell if they are fulfilled. */
    private val conditionsVerifier = ConditionsVerifier(processingState, screenDetector, bitmapSupplier, progressListener)
    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor(
        androidExecutor = androidExecutor,
        processingState = processingState,
        randomize = randomize,
        unblockWorkaroundEnabled = unblockWorkaroundEnabled,
    )

    /** Tells if the screen metrics have been invalidated and should be updated. */
    private var invalidateScreenMetrics = true

    fun onScenarioStart(context: Context) {
        processingState.onProcessingStarted(context)
    }

    fun onScenarioEnd() {
        processingState.onProcessingStopped()
    }

    /** Drop all current cache related to screen metrics. */
    fun invalidateScreenMetrics() {
        invalidateScreenMetrics = true
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

        // Handle all trigger events enabled during previous processing
        if (!processingState.areAllTriggerEventsDisabled()) {
            processTriggerEvents(processingState.getEnabledTriggerEvents()) { triggerEvent, results ->
                actionExecutor.executeActions(triggerEvent, results)
            }
        }

        // Reset any values that needs to be reset for each iteration
        // After the triggers to let them handle changes, before the image processing to start capturing values before
        processingState.clearIterationState()

        // Handle the image detection
        progressListener?.onScreenEventsProcessingStarted()
        if (!processingState.areAllImageEventsDisabled()) {
            processImageEvents(screenFrame, processingState.getEnabledImageEvents()) { imageEvent, results ->
                actionExecutor.executeActions(imageEvent, results)
            }
        }
        progressListener?.onScreenEventsProcessingCompleted()

        // Loop is completed
        actionExecutor.onScenarioLoopFinished()

        return
    }

    private suspend fun processTriggerEvents(
        events: Collection<TriggerEvent>,
        onFulfilled: suspend (TriggerEvent, ConditionsResult) -> Unit,
    ) {
        for (triggerEvent in events) {
            // Enabled state of the event might have changed during the loop
            if (!processingState.isEventEnabled(triggerEvent.id.databaseId)) continue

            // No conditions ? This should not happen, skip this event
            if (triggerEvent.conditions.isEmpty()) continue

            progressListener?.onTriggerEventProcessingStarted(triggerEvent)

            val results = conditionsVerifier.verifyConditions(triggerEvent.conditionOperator, triggerEvent.conditions)
            if (results.fulfilled == true) onFulfilled(triggerEvent, results)

            progressListener?.onTriggerEventProcessingCompleted(triggerEvent, results.getAllResults())
        }
    }

    private suspend fun processImageEvents(
        screenFrame: Bitmap,
        events: Collection<ScreenEvent>,
        onFulfilled: suspend (ScreenEvent, ConditionsResult) -> Unit,
    ) {
        // Set the current screen image
        if (invalidateScreenMetrics) {
            screenDetector.setScreenMetrics(processingTag, screenFrame, detectionQuality.toDouble())
            invalidateScreenMetrics = false
        }
        screenDetector.setupDetection(screenFrame)

        // Check all events
        for (imageEvent in events) {
            // No conditions ? This should not happen, skip this event
            if (imageEvent.conditions.isEmpty()) continue

            progressListener?.onScreenEventProcessingStarted(imageEvent)
            val results = conditionsVerifier.verifyConditions(imageEvent.conditionOperator, imageEvent.conditions)
            progressListener?.onScreenEventProcessingCompleted(imageEvent, results)

            if (results.fulfilled == true) {
                onFulfilled(imageEvent, results)
                if (!imageEvent.keepDetecting) return
            }

            // Stop processing if requested
            yield()
        }
    }
}
