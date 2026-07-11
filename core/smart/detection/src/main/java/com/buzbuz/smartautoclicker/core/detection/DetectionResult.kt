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
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Point

/**
 * The results of a condition detection.
 * @param isDetected true if the condition have been detected. false if not.
 * @param confidenceRate confidence rate of the algorithm for this result
 * @param position contains the center of the detected condition in screen coordinates.
 * @param size size of the detected condition.
 * @param numberDetected defined only for a positive number capture request, null for others.
 */
data class DetectionResult(
    val isDetected: Boolean = false,
    val confidenceRate: Double = 0.0,
    val position: Point = Point(),
    val size: Point = Point(),
    val numberDetected: Double? = null,
)

/** Build the detection result object from a native call returned value. */
internal fun DoubleArray?.toDetectionResult(): DetectionResult {
    if (this == null || size < 7) return DetectionResult()

    val numberDetected = this[6]
    return DetectionResult(
        isDetected = this[0] > 0.5,
        position = Point(this[1].toInt(), this[2].toInt()),
        size = Point(this[3].toInt(), this[4].toInt()),
        confidenceRate = this[5],
        numberDetected = if(numberDetected == -Double.MAX_VALUE) null else numberDetected,
    )
}
