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
package com.buzbuz.smartautoclicker.detection

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
import com.buzbuz.smartautoclicker.opencv.ImageDetector

import kotlinx.coroutines.yield

/**
 * Process a screen image and tries to detect the list of [Event] on it.
 *
 * @param events the list of scenario events to be detected.
 * @param bitmapSupplier provides the conditions bitmaps.
 * @param gestureExecutor execute the actions gestures on the user screen.
 * @param onEndConditionReached called when a end condition of the scenario have been reached.
 */
internal class ScenarioProcessor(
    private val events: List<Event>,
    private val bitmapSupplier: (String, Int, Int) -> Bitmap?,
    gestureExecutor: (GestureDescription) -> Unit,
    private val onEndConditionReached: () -> Unit,
) : AutoCloseable {

    /** Detect the condition images on the screen image. */
    private val imageDetector = ImageDetector()
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
        // Check if an event has reached its max execution count.
        executedEvents.forEach { (event, executedCount) ->
            event.stopAfter?.let { stopAfter ->
                if (stopAfter <= executedCount) {
                    onEndConditionReached()
                    return
                }
            }
        }

        // Stop processing if requested
        yield()

        // Set the current screen image
        imageDetector.setScreenImage(screenImage.toBitmap(processedScreenBitmap))

        for (event in events) {
            // No conditions ? This should not happen, skip this event
            if (event.conditions?.isEmpty() == true) {
                continue
            }

            // If conditions are fulfilled, execute this event's actions !
            if (verifyConditions(imageDetector, event.conditionOperator, event.conditions!!)) {

                executedEvents[event] = executedEvents[event]?.plus(1)
                    ?: throw IllegalStateException("Can' find the event in the executed events map.")

                event.actions?.let { actions ->
                    actionExecutor.executeActions(actions)
                }
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
        imageDetector: ImageDetector,
        @ConditionOperator operator: Int,
        conditions: List<Condition>
    ) : Boolean {

        for (condition in conditions) {
            // Verify if the condition is fulfilled.
            if (!checkCondition(imageDetector, condition)) {
                if (operator == AND) {
                    // One of the condition isn't fulfilled, it's a false for a AND operator.
                    return false
                }
            } else if (operator  == OR) {
                // One of the condition is fulfilled, it's a yes for a OR operator.
                return true
            }
        }

        // All conditions passed for AND, none are for OR.
        return operator == AND
    }

    /**
     * Check if the provided condition is fulfilled.
     *
     * Check if the condition bitmap match the content of the condition area on the currently processed [Image].
     *
     * @param condition the event condition to be verified.
     *
     * @return true if the currently processed [Image] contains the condition bitmap at the condition area.
     */
    private fun checkCondition(imageDetector: ImageDetector, condition: Condition) : Boolean {
        val path = condition.path ?: return false
        val conditionBitmap = bitmapSupplier(path, condition.area.width(), condition.area.height()) ?: return false
        val detectionRatio = (100 - condition.threshold).toDouble() / 100

        return when (condition.detectionType) {
            EXACT -> imageDetector.detectCondition(conditionBitmap, condition.area, detectionRatio)
            WHOLE_SCREEN -> imageDetector.detectCondition(conditionBitmap, detectionRatio)
            else -> false
        }
    }

    override fun close() {
        imageDetector.close()
    }
}