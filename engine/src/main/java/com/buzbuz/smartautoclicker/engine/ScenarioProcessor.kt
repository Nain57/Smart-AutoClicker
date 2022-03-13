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

import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.media.Image

import com.buzbuz.smartautoclicker.database.domain.AND
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.database.domain.ConditionOperator
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.database.domain.EXACT
import com.buzbuz.smartautoclicker.database.domain.OR
import com.buzbuz.smartautoclicker.database.domain.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.detection.ImageDetector

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [Event] on it.
 *
 * @param imageDetector the detector for images.
 * @param events the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param gestureExecutor execute the actions gestures on the user screen.
 * @param onEndConditionReached called when a end condition of the scenario have been reached.
 */
internal class ScenarioProcessor(
    private val imageDetector: ImageDetector,
    private val events: List<Event>,
    private val bitmapSupplier: (String, Int, Int) -> Bitmap?,
    gestureExecutor: (GestureDescription) -> Unit,
    private val onEndConditionReached: () -> Unit,
) {

    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor(gestureExecutor)
    /** Number of execution count for each events since the processing start. */
    private val executedEvents = HashMap<Event, Int>().apply {
        events.forEach { put(it, 0) }
    }
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
            imageDetector.setScreenImage(this)
        }

        for (event in events) {
            // No conditions ? This should not happen, skip this event
            if (event.conditions?.isEmpty() == true) {
                continue
            }

            // If conditions are fulfilled, execute this event's actions !
            val result = verifyConditions(event.conditionOperator, event.conditions!!)
            if (result.first) {

                executedEvents[event] = executedEvents[event]?.plus(1)
                    ?: throw IllegalStateException("Can' find the event in the executed events map.")

                event.actions?.let { actions ->
                    actionExecutor.executeActions(actions, result.second?.position)
                }

                // Check if an event has reached its max execution count.
                executedEvents.forEach { (event, executedCount) ->
                    event.stopAfter?.let { stopAfter ->
                        if (stopAfter <= executedCount) {
                            onEndConditionReached()
                            return
                        }
                    }
                }

                break
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
     * @param operator the operator to apply between the conditions. Must be one of [ConditionOperator] values.
     * @param conditions the condition to be checked on the currently processed [Image].
     */
    private fun verifyConditions(
        @ConditionOperator operator: Int,
        conditions: List<Condition>
    ) : Pair<Boolean, DetectionResult?> {

        conditions.forEachIndexed { index, condition ->
            // Verify if the condition is fulfilled.
            val result = checkCondition(condition) ?: return false to null
            if (result.isDetected xor condition.shouldBeDetected) {
                if (operator == AND) {
                    // One of the condition isn't fulfilled, it's a false for a AND operator.
                    return false to result
                }

            } else if (operator  == OR) {
                // One of the condition is fulfilled, it's a yes for a OR operator.
                return true to result
            }

            // All conditions passed for AND, none are for OR.
            if (index == conditions.size - 1) {
                return (operator == AND) to result
            }
        }

        return false to null
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