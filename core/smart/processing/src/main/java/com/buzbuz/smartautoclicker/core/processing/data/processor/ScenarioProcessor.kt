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

import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingListener

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [ImageEvent] on it.
 *
 * @param imageDetector the detector for images.
 * @param randomize true to randomize the actions values a bit to avoid being taken for a bot.
 * @param imageEvents the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param androidExecutor execute the actions requiring an interaction with Android..
 * @param onStopRequested called when a end condition of the scenario have been reached or all events are disabled.
 * @param progressListener the object to notify for detection progress. Can be null if not required.
 */
internal class ScenarioProcessor(
    private val processingTag: String,
    private val imageDetector: ImageDetector,
    scalingManager: ScalingManager,
    randomize: Boolean,
    imageEvents: List<ImageEvent>,
    triggerEvents: List<TriggerEvent>,
    private val bitmapSupplier: suspend (String, Int, Int) -> Bitmap?,
    androidExecutor: AndroidActionExecutor,
    unblockWorkaroundEnabled: Boolean = false,
    private val onStopRequested: () -> Unit,
    private val progressListener: DebuggingListener? = null,
) {

    /** Handle the processing state of the scenario. */
    @VisibleForTesting internal val processingState: ProcessingState = ProcessingState(imageEvents, triggerEvents)
    /** Check conditions and tell if they are fulfilled. */
    private val conditionsVerifier = ConditionsVerifier(
        state = processingState,
        imageDetector = imageDetector,
        scalingManager = scalingManager,
        bitmapSupplier = bitmapSupplier,
        progressListener = progressListener,
    )
    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor(
        androidExecutor = androidExecutor,
        processingState = processingState,
        randomize = randomize,
        unblockWorkaroundEnabled = unblockWorkaroundEnabled,
    )

    fun onScenarioStart(context: Context) {
        processingState.onProcessingStarted(context)
    }

    fun onScenarioEnd() {
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
        progressListener?.onImageEventsProcessingStarted()
        if (!processingState.areAllImageEventsDisabled()) {
            processImageEvents(screenFrame, processingState.getEnabledImageEvents()) { imageEvent, results ->
                actionExecutor.executeActions(imageEvent, results)
            }
        }
        progressListener?.onImageEventsProcessingCompleted()

        // Loop is completed
        actionExecutor.onScenarioLoopFinished()

        return
    }

    private suspend fun processTriggerEvents(
        events: Collection<TriggerEvent>,
        onFulfilled: suspend (TriggerEvent, ConditionsResults) -> Unit,
    ) {
        for (triggerEvent in events) {
            // Enabled state of the event might have changed during the loop
            if (!processingState.isEventEnabled(triggerEvent.id.databaseId)) continue

            // No conditions ? This should not happen, skip this event
            if (triggerEvent.conditions.isEmpty()) continue

            val results = conditionsVerifier.verifyConditions(
                operator = triggerEvent.conditionOperator,
                conditions = triggerEvent.conditions,
            )

            if (results.fulfilled  == true) {
                progressListener?.onTriggerEventFulfilled(
                    event = triggerEvent,
                    results = results.getAllTriggerConditionsResults(),
                )

                onFulfilled(triggerEvent, results)
            }
        }
    }

    private suspend fun processImageEvents(
        screenFrame: Bitmap,
        events: Collection<ImageEvent>,
        onFulfilled: suspend (ImageEvent, ConditionsResults) -> Unit,
    ) {
        // Set the current screen image
        imageDetector.setScreenBitmap(screenFrame, processingTag)

        try {
            // Check all events
            for (imageEvent in events) {
                // No conditions ? This should not happen, skip this event
                if (imageEvent.conditions.isEmpty()) continue

                progressListener?.onImageEventProcessingStarted()
                val results = conditionsVerifier.verifyConditions(
                    operator = imageEvent.conditionOperator,
                    conditions = imageEvent.conditions,
                )

                if (results.fulfilled == true) {
                    progressListener?.onImageEventFulfilled(
                        event = imageEvent,
                        results = results.getAllImageConditionsResults(),
                    )

                    onFulfilled(imageEvent, results)
                    if (!imageEvent.keepDetecting) break
                }

                // Stop processing if requested
                yield()
            }
        } finally {
            // We are done processing this frame, release it
            imageDetector.releaseScreenBitmap(screenFrame)
        }
    }
}
