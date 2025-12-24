/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.domain.model

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition

/** Result of the processing of a condition, provided by the detection engine. */
sealed class ProcessedConditionResult {

    /** Tells if the condition has been fulfilled or not.*/
    abstract val isFulfilled: Boolean

    /**
     * Results for an ImageCondition.
     *
     * @param haveBeenDetected true if the image have been detected, false if not.
     * @param condition the condition that triggered this result.
     * @param confidenceRate the confidence rate of the detection algorithm on this result. Between [0 - 100].
     * @param position the position at which the condition image have been detected. Null if not detected.
     */
    data class Image(
        override val isFulfilled: Boolean,
        val haveBeenDetected: Boolean,
        val condition: ImageCondition,
        val confidenceRate: Double,
        val position: Point?,
    ) : ProcessedConditionResult()

    /**
     * Results for a TriggerCondition.
     *
     * @param condition the condition that triggered this result.
     */
    data class Trigger(
        override val isFulfilled: Boolean,
        val condition: TriggerCondition,
    )  : ProcessedConditionResult()
}