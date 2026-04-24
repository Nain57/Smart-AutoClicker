/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition

/** Event Condition results during a live debugging session. */
sealed interface DebugLiveEventConditionResult {

    /** The condition that triggered this result. */
    val condition: Condition
    /** Tells if the condition have been fulfilled or not. */
    val isFulfilled: Boolean

    /**
     * @param isDetected tells if the image of this condition have been detected or not.
     * @param confidenceRate the confidence rate of the detection algorithm on this result. Between [0 - 100].
     * @param detectionArea the area of the image that have been detected. Null if not detected.
     */
    data class Image(
        override val condition: ImageCondition,
        override val isFulfilled: Boolean,
        val isDetected: Boolean,
        val confidenceRate: Double,
        val detectionArea: Rect?,
    ) : DebugLiveEventConditionResult

    data class Trigger(
        override val condition: TriggerCondition,
        override val isFulfilled: Boolean,
    ) : DebugLiveEventConditionResult

}