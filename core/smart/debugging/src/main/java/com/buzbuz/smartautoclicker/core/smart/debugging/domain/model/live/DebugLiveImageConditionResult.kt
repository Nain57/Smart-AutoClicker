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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live

import android.graphics.Rect

/**
 * Condition detection results during a live debugging session.
 *
 * @param conditionId the unique identifier of the condition that triggered this result.
 * @param isFulfilled tells if the condition have been fulfilled or not.
 * @param isDetected tells if the image of this condition have been detected or not.
 * @param confidenceRate the confidence rate of the detection algorithm on this result. Between [0 - 100].
 * @param detectionArea the area of the image that have been detected. Null if not detected.
 */
data class DebugLiveImageConditionResult(
    val conditionId: Long,
    val isFulfilled: Boolean,
    val isDetected: Boolean,
    val confidenceRate: Double,
    val detectionArea: Rect?,
)