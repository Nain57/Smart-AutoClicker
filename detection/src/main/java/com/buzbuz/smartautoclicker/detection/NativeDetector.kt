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

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from github.
 */
class NativeDetector : ImageDetector {

    companion object {
        // Used to load the 'smartautoclicker' library on application startup.
        init {
            System.loadLibrary("smartautoclicker")
        }
    }

    /** The results of the detection. Modified by native code. */
    private val detectionResult = DetectionResult()

    /** Native pointer of the detector object. */
    @Keep
    private val nativePtr: Long = newDetector()

    override fun close() = deleteDetector()

    override fun setupDetection(screenBitmap: Bitmap, detectionQuality: Double) {
        if (detectionQuality < DETECTION_QUALITY_MIN || detectionQuality > DETECTION_QUALITY_MAX)
            throw IllegalArgumentException("Invalid detection quality")

        setScreenImage(screenBitmap, detectionQuality)
    }

    override fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult {
        detect(conditionBitmap, threshold, detectionResult)
        return detectionResult.copy()
    }

    override fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult {
        detectAt(conditionBitmap, position.left, position.top, position.width(), position.height(), threshold, detectionResult)
        return detectionResult.copy()
    }

    /**
     * Creates the detector. Must be called before any other methods.
     * Call [close] to release resources once the detection process is finished.
     *
     * @return the pointer of the native detector object.
     */
    private external fun newDetector(): Long

    /**
     * Deletes the native detector.
     * Once called, this object can't be used anymore.
     */
    private external fun deleteDetector()

    /**
     * Native method for detection setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     * @param detectionQuality the quality of the detection. The higher the preciser, the lower the faster. Must be
     *                         contained in [DETECTION_QUALITY_MIN] and [DETECTION_QUALITY_MAX].
     */
    private external fun setScreenImage(screenBitmap: Bitmap, detectionQuality: Double)

    /**
     * Native method for detecting if the bitmap is in the whole current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param threshold the allowed error threshold allowed for the condition.
     * @param result stores the results on this detection.
     */
    private external fun detect(conditionBitmap: Bitmap, threshold: Int, result: DetectionResult)

    /**
     * Native method for detecting if the bitmap is at a specific position in the current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condtion.
     * @param threshold the allowed error threshold allowed for the condition.
     * @param result stores the results on this detection.
     */
    private external fun detectAt(
        conditionBitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        result: DetectionResult
    )
}