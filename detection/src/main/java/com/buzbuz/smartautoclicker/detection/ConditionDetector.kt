/*
 * Copyright (C) 2021 Nain57
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

import android.graphics.Rect
import android.media.Image

import androidx.annotation.WorkerThread

import com.buzbuz.smartautoclicker.database.domain.AND
import com.buzbuz.smartautoclicker.database.domain.OR
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.database.domain.ConditionOperator
import com.buzbuz.smartautoclicker.database.domain.Event

import kotlin.math.abs

/**
 * Process [Image] from the [Cache] and tries to detect the list of [Event] on it.
 **
 * @param cache the image cache to apply the detection to.
 */
@WorkerThread
internal class ConditionDetector(private val cache: Cache) {

    /**
     * Find an event with the conditions fulfilled on the current image.
     *
     * @param events the list of events to be verified.
     *
     * @return the first Event with all conditions fulfilled, or null if none has been found.
     */
    fun detect(events: List<Event>) : Event? {
        for (event in events) {

            // No conditions ? This should not happen, skip this event
            if (event.conditions?.isEmpty() == true) {
                continue
            }

            // If conditions are fulfilled, execute this event's actions !
            if (verifyConditions(event.conditionOperator, event.conditions!!)) {
                return event
            }
        }

        return null
    }

    /**
     * Verifies if all conditions of a events are fulfilled.
     *
     * Applies the provided conditions the currently processed [Image] according to the provided operator.
     *
     * @param operator the operator to apply between the conditions. Must be one of [ConditionOperator] values.
     * @param conditions the condition to be checked on the currently processed [Image].
     */
    private fun verifyConditions(@ConditionOperator operator: Int, conditions: List<Condition>) : Boolean {

        for (condition in conditions) {
            // Verify if the condition is fulfilled.
            if (!checkCondition(condition)) {
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
    private fun checkCondition(condition: Condition) : Boolean {
        // Now we have a condition cache, so let's detect !
        cache.pixelsCache.get(condition)?.let { pixels ->

            // Check if the condition is contained in the screen
            if (!cache.displaySize.contains(condition.area)) {
                return false
            }

            // Get the pixels of the part of the [Image] that will be compared.
            getCroppedPixels(pixels.second, condition.area)

            // For each pixel, compare the RGB values of the condition pixels and the cropped image pixels and keep
            // the difference.
            cache.currentDiff = 0
            for (i in pixels.second.indices) {
                cache.currentDiff += abs((pixels.first[i] shr 16 and 0xff) - (pixels.second[i] shr 16 and 0xff)) +
                        abs((pixels.first[i] shr 8 and 0xff) - (pixels.second[i] shr 8 and 0xff)) +
                        abs((pixels.first[i] and 0xff) - (pixels.second[i] and 0xff))
            }

            // If the difference % is lower than the threshold, the condition is fulfilled; returns true.
            return 100.0 * cache.currentDiff / (3L * 255 * pixels.second.size) < condition.threshold
        }

        // The cache could not initialize the pixels for the condition. This can be caused by a corrupted bitmap file.
        return false
    }

    /**
     * Fills the provided array with the pixels at the area on the currently processed [Image]
     *
     * @param pixels the array to be filled. It's size must match the area one or an [IndexOutOfBoundsException] will
     *               thrown.
     * @param area the area on the currently processed [Image] to take the pixels from.
     */
    private fun getCroppedPixels(pixels: IntArray, area: Rect) {
        cache.screenBitmap!!.apply {
            cache.cropIndex = 0

            // Pixels are ordered by row in the image they represents; first value is the upper left pixel and last
            // value is the lower right one. A row length is screenBitmap.width.

            // For each row between the crop area top and bottom
            for (y in (area.top * width) until area.bottom * width step width) {
                // In this row, for each pixel between the crop area left and right
                for (x in (area.left + y) until area.right + y) {
                    // We want this pixel in the crop, put it and continue
                    pixels[cache.cropIndex] = cache.screenPixels!![x]
                    cache.cropIndex++
                }
            }
        }
    }
}