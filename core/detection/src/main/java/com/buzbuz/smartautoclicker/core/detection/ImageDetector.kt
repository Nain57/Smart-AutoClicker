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
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.Keep

/**
 * Detects bitmaps within other bitmaps for conditions detection on the screen.
 * All calls should be made on the same thread.
 */
interface ImageDetector : AutoCloseable {

    /**
     * Set the current metrics of the screen.
     * This MUST be called before the first detection, with the first bitmap provided by the screen. All orientation
     * changes must also trigger a call to this method.
     * All following calls to [detectCondition] methods will be verified against the metrics of this bitmap.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     * @param detectionQuality the quality of the detection. The higher the preciser, the lower the faster. Must be
     *                         contained in [DETECTION_QUALITY_MIN] and [DETECTION_QUALITY_MAX].
     */
    fun setScreenMetrics(screenBitmap: Bitmap, detectionQuality: Double)

    /**
     * Set the bitmap for the screen.
     * All following calls to [detectCondition] methods will be verified against this bitmap.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    fun setupDetection(screenBitmap: Bitmap)

    /**
     * Detect if the bitmap is in the whole current screen bitmap.
     * [setupDetection] must have been called first with the content of the screen.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the results of the detection.
     */
    fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult

    /**
     * Detect if the bitmap is at a specific position in the current screen bitmap.
     * [setupDetection] must have been called first with the content of the screen.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param position the position on the screen where the condition should be detected.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the results of the detection.
     */
    fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult
}

/** The maximum detection quality for the algorithm. */
const val DETECTION_QUALITY_MAX = 3216L
/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = 400L

/**
 * The results of a condition detection.
 * @param isDetected true if the condition have been detected. false if not.
 * @param position contains the center of the detected condition in screen coordinates.
 * @param confidenceRate
 */
data class DetectionResult(
    var isDetected: Boolean = false,
    val position: Point = Point(),
    var confidenceRate: Double = 0.0
) {

    /**
     * Set the results of the detection.
     * Used by native code only.
     */
    @Keep
    fun setResults(isDetected: Boolean, centerX: Int, centerY: Int, confidenceRate: Double) {
        this.isDetected = isDetected
        position.set(centerX, centerY)
        this.confidenceRate = confidenceRate
    }
}