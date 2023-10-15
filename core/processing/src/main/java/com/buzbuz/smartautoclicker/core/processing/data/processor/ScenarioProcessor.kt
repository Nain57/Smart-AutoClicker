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
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.processing.data.ActionExecutor
import com.buzbuz.smartautoclicker.core.processing.data.EndConditionVerifier
import com.buzbuz.smartautoclicker.core.processing.data.ScenarioState

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [Event] on it.
 *
 * @param imageDetector the detector for images.
 * @param detectionQuality the quality of the detection.
 * @param randomize true to randomize the actions values a bit to avoid being taken for a bot.
 * @param events the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param androidExecutor execute the actions requiring an interaction with Android..
 * @param endConditionOperator the operator to apply between the end conditions.
 * @param endConditions the list of end conditions for the current scenario.
 * @param onStopRequested called when a end condition of the scenario have been reached or all events are disabled.
 * @param progressListener the object to notify for detection progress. Can be null if not required.
 */
internal class ScenarioProcessor(
    private val imageDetector: ImageDetector,
    private val detectionQuality: Int,
    randomize: Boolean,
    events: List<Event>,
    private val bitmapSupplier: suspend (String, Int, Int) -> Bitmap?,
    androidExecutor: AndroidExecutor,
    @ConditionOperator endConditionOperator: Int,
    endConditions: List<EndCondition>,
    private val onStopRequested: () -> Unit,
    private val progressListener: ProgressListener? = null,
) {

    /** Handle the processing state of the scenario. */
    private val scenarioState = ScenarioState(events)
    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor(androidExecutor, scenarioState, randomize)
    /** Verifies the end conditions of a scenario. */
    private val endConditionVerifier = EndConditionVerifier(endConditions, endConditionOperator, onStopRequested)
    /** Keep track of the detection results during the processing. */
    private val processingResults = ProcessingResults(events)

    /** Tells if the screen metrics have been invalidated and should be updated. */
    private var invalidateScreenMetrics = true

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
        if (scenarioState.areAllEventsDisabled()) {
            onStopRequested()
            return
        }

        progressListener?.onImageProcessingStarted()

        // Set the current screen image
        initScreenFrame(screenFrame)

        // Clear previous results
        processingResults.clearResults()

        for (event in scenarioState.getEnabledEvents()) {
            // No conditions ? This should not happen, skip this event
            if (event.conditions.isEmpty()) {
                continue
            }

            // Event conditions verification
            progressListener?.onEventProcessingStarted(event)
            val conditionAreFulfilled = verifyConditions(event)
            progressListener?.onEventProcessingCompleted(event, conditionAreFulfilled, processingResults.getFirstMatchResult())

            // If conditions are fulfilled, execute this event's actions !
            if (conditionAreFulfilled) {
                event.actions.let { actions ->
                    actionExecutor.executeActions(event, actions, processingResults)
                }

                // Check if an event has reached its max execution count.
                if (endConditionVerifier.onEventTriggered(event)) return

                break
            }

            // Stop processing if requested
            yield()
        }

        progressListener?.onImageProcessingCompleted()
        return
    }

    /**
     * Initialize the detection algorithm with the current screen frame.
     * @return the image, as a Bitmap.
     */
    private fun initScreenFrame(screenFrame: Bitmap) {
        if (invalidateScreenMetrics) {
            imageDetector.setScreenMetrics(screenFrame, detectionQuality.toDouble())
            invalidateScreenMetrics = false
        }

        imageDetector.setupDetection(screenFrame)
    }

    /**
     * Verifies if all conditions of an event are fulfilled.
     * Applies the provided conditions the currently processed [Image] according to the provided operator.
     *
     * @param event the event to verify the conditions of.
     * @return true if the conditions are fulfilled, false if not.
     */
    private suspend fun verifyConditions(event: Event) : Boolean {
        event.conditions.forEach { condition ->
            // Verify if the condition is fulfilled.
            progressListener?.onConditionProcessingStarted(condition)
            checkCondition(condition)
                ?.let { result ->
                    processingResults.addResult(condition, result.isDetected, result.position, result.confidenceRate)
                    progressListener?.onConditionProcessingCompleted(result)

                    if (condition.isNotFulfilled(result)) {
                        // One of the condition isn't fulfilled, it's a false for a AND operator.
                        if (event.conditionOperator == AND) return false
                    } else if (event.conditionOperator == OR) {
                        // One of the condition is fulfilled, it's a yes for a OR operator.
                        return true
                    }
                }
                ?:let {
                    progressListener?.cancelCurrentConditionProcessing()
                    return false
                }

            yield()
        }

        // All conditions passed for AND, none are for OR.
        return event.conditionOperator == AND
    }

    private fun Condition.isNotFulfilled(result: DetectionResult): Boolean =
        result.isDetected xor shouldBeDetected

    /**
     * Check if the provided condition is fulfilled.
     *
     * Check if the condition bitmap match the content of the condition area on the currently processed [Image].
     *
     * @param condition the event condition to be verified.
     *
     * @return the result of the detection, or null of the detection is not possible.
     */
    private suspend fun checkCondition(condition: Condition) : DetectionResult? {
        condition.path?.let { path ->
            bitmapSupplier(path, condition.area.width(), condition.area.height())?.let { conditionBitmap ->
                return detect(condition, conditionBitmap)
            }
        }

        Log.w(TAG, "Bitmap for condition with path ${condition.path} not found.")
        return null
    }

    /**
     * Detect the condition on the screen.
     *
     * @param condition the condition to be detected.
     * @param conditionBitmap the bitmap representing the condition.
     *
     * @return the result of the detection.
     */
    private fun detect(condition: Condition, conditionBitmap: Bitmap): DetectionResult =
         when (condition.detectionType) {
             EXACT -> imageDetector.detectCondition(conditionBitmap, condition.area, condition.threshold)
             WHOLE_SCREEN -> imageDetector.detectCondition(conditionBitmap, condition.threshold)
             IN_AREA -> condition.detectionArea?.let { area ->
                 imageDetector.detectCondition(conditionBitmap, area, condition.threshold)
             } ?: throw IllegalArgumentException("Invalid IN_AREA condition, no area defined")
             else -> throw IllegalArgumentException("Unexpected detection type")
         }
}

/** Tag for logs. */
private const val TAG = "ScenarioProcessor"