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

import android.graphics.Bitmap
import android.media.Image

import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.detection.ImageDetector
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.domain.WHOLE_SCREEN

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [Event] on it.
 *
 * @param imageDetector the detector for images.
 * @param detectionQuality the quality of the detection.
 * @param events the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param androidExecutor execute the actions requiring an interaction with Android..
 * @param endConditionOperator the operator to apply between the end conditions.
 * @param endConditions the list of end conditions for the current scenario.
 * @param onEndConditionReached called when a end condition of the scenario have been reached.
 * @param debugEngine the engine for the debugging. Can be null if not required.
 */
internal class ScenarioProcessor(
    private val imageDetector: ImageDetector,
    private val detectionQuality: Int,
    private val events: List<Event>,
    private val bitmapSupplier: (String, Int, Int) -> Bitmap?,
    androidExecutor: AndroidExecutor,
    @ConditionOperator endConditionOperator: Int,
    endConditions: List<EndCondition>,
    onEndConditionReached: () -> Unit,
    private val debugEngine: DebugEngine? = null,
) {

    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor(androidExecutor)
    /** Verifies the end conditions of a scenario. */
    private val endConditionVerifier = EndConditionVerifier(endConditions, endConditionOperator, onEndConditionReached)

    /** The bitmap of the currently processed image. Kept in order to avoid instantiating a new one everytime. */
    private var processedScreenBitmap: Bitmap? = null

    /**
     * Find an event with the conditions fulfilled on the current image.
     *
     * @param screenImage the image containing the current screen display.
     *
     * @return the first Event with all conditions fulfilled, or null if none has been found.
     */
    suspend fun process(screenImage: Image) {
        // Set the current screen image
        processedScreenBitmap = screenImage.toBitmap(processedScreenBitmap).apply {
            imageDetector.setupDetection(this, detectionQuality.toDouble())
        }

        for (event in events) {
            // No conditions ? This should not happen, skip this event
            if (event.conditions?.isEmpty() == true) {
                continue
            }

            // If conditions are fulfilled, execute this event's actions !
            val result = verifyConditions(event)
            if (result.eventMatched) {
                // If scenario debug mode is ON, notifies its engine.
                debugEngine?.sendNewResult(result)

                event.actions?.let { actions ->
                    actionExecutor.executeActions(actions, result.detectionResult?.position)
                }

                // Check if an event has reached its max execution count.
                if (endConditionVerifier.onEventTriggered(event)) return

                break
            } else {
                // If scenario debug mode is ON, notifies its engine, even for detection failure
                debugEngine?.sendNewResult(result)
            }

            // Stop processing if requested
            yield()
        }

        return
    }

    /**
     * Verifies if all conditions of a events are fulfilled.
     *
     * Applies the provided conditions the currently processed [Image] according to the provided operator.
     *
     * @param event the event to verify the conditions of.
     */
    private suspend fun verifyConditions(event: Event) : ProcessorResult {
        event.conditions?.forEachIndexed { index, condition ->
            // Verify if the condition is fulfilled.
            val result = checkCondition(condition) ?: return ProcessorResult(false)

            if (result.isDetected xor condition.shouldBeDetected) {
                if (event.conditionOperator == AND) {
                    // One of the condition isn't fulfilled, it's a false for a AND operator.
                    return ProcessorResult(
                        false,
                        event,
                        event.conditions?.get(index),
                        result,
                    )
                }
            } else if (event.conditionOperator == OR) {
                // One of the condition is fulfilled, it's a yes for a OR operator.
                return ProcessorResult(
                    true,
                    event,
                    event.conditions?.get(index),
                    result,
                )
            }

            // All conditions passed for AND, none are for OR.
            if (index == (event.conditions?.size ?: 0) - 1) {
                return if (event.conditionOperator == AND) {
                    ProcessorResult(
                        true,
                        event,
                        event.conditions?.get(index),
                        result,
                    )
                } else {
                    ProcessorResult(
                        false,
                        event,
                        event.conditions?.get(index),
                        result,
                    )
                }
            }

            yield()
        }

        return ProcessorResult(false)
    }

    /**
     * Check if the provided condition is fulfilled.
     *
     * Check if the condition bitmap match the content of the condition area on the currently processed [Image].
     *
     * @param condition the event condition to be verified.
     *
     * @return the result of the detection, or null of the detection is not possible.
     */
    private fun checkCondition(condition: Condition) : DetectionResult? {
        condition.path?.let { path ->
            bitmapSupplier(path, condition.area.width(), condition.area.height())?.let { conditionBitmap ->
                return detect(condition, conditionBitmap)
            }
        }

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
            else -> throw IllegalArgumentException("Unexpected detection type")
        }
}

/**
 * Send a new detection result to the debug engine, if any.
 * @param result the results to be sent.
 */
private suspend fun DebugEngine?.sendNewResult(result: ProcessorResult) = this?.let { debug ->
    if (result.event != null && result.condition != null && result.detectionResult != null) {
        debug.onNewDetectionResult(result.event, result.condition, result.detectionResult)
    }
}

/**
 * The results of a the scenario processing.
 * @param eventMatched true if the event conditions have been matched.
 * @param event the event tested.
 * @param condition the condition detected.
 * @param detectionResult the results of the detection.
 */
private data class ProcessorResult(
    val eventMatched: Boolean,
    val event: Event? = null,
    val condition: Condition? = null,
    val detectionResult: DetectionResult? = null,
)