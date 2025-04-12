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
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Rect
import androidx.annotation.Keep

/**
 * The results of a condition detection.
 * @param isDetected true if the condition have been detected. false if not.
 * @param position contains the center of the detected condition in screen coordinates.
 * @param confidenceRate
 */
data class DetectionResult(
    var isDetected: Boolean = false,
    val position: Rect = Rect(),
    var confidenceRate: Double = 0.0
) {

    /**
     * Set the results of the detection.
     * Used by native code only.
     */
    @Keep
    fun setResults(isDetected: Boolean, x: Int, y: Int, width: Int, height: Int, confidenceRate: Double) {
        this.isDetected = isDetected
        position.set(x, y, x + width, y + height)
        this.confidenceRate = confidenceRate
    }
}