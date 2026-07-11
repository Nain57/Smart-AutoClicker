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

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.ColorInt

/**
 * Detects bitmaps within other bitmaps for conditions detection on the screen.
 * All calls should be made on the same thread.
 */
interface ImageDetector : AutoCloseable {

    /** Initialize the detector. Must be called on the same thread as the detection. */
    fun init()


    /**
     * Loads the text detection models for the detector.
     *
     * @param detectionModelPath Path on the filesystem to the detection model folder. Must contain det.ncnn.bin &
     * det.ncnn.param files
     * @param recognitionModels Map of recognition model identifier to their path on the filesystem. Identifier will be
     * used to specify the model to use when detecting with [detectText]. Each model folder must contain rec.ncnn.bin,
     * rec.ncnn.param and dict.txt files.
     */
    fun loadTextDetectionModels(detectionModelPath: String, recognitionModels: Map<String, String>): Boolean

    /**
     * Set the bitmap for the screen.
     * All following calls to [detectImage] methods will be verified against this bitmap.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    fun setScreenBitmap(screenBitmap: Bitmap, metadata: String)

    /**
     * Detect if the bitmap is at a specific position in the current screen bitmap.
     * [setScreenBitmap] must have been called first with the content of the screen.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param conditionWidth the expected width of the condition at detection time.
     * @param conditionHeight the expected height of the condition at detection time.
     * @param detectionArea the position on the screen where the condition should be detected.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the results of the detection.
     */
    fun detectImage(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult

    /**
     * Detect if the average color of the provided area match the condition color.
     * [setScreenBitmap] must have been called first with the content of the screen.
     *
     * @param conditionColor the color to detect.
     * @param detectionArea the area of the detected color.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the results of the detection.
     */
    fun detectColor(
        @ColorInt conditionColor: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult

    /**
     * Detect if a text is visible the provided area.
     * [setScreenBitmap] must have been called first with the content of the screen.
     *
     * @param conditionText the text to detect.
     * @param recognitionModelId the identifier of the model to use, as specified during [loadTextDetectionModels] call.
     * @param detectionArea the area to search for the text.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the results of the detection.
     */
    fun detectText(
        conditionText: String,
        recognitionModelId: String,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult

    /**
     * Detect if a number is visible in the provided area.
     * [setScreenBitmap] must have been called first with the content of the screen.
     *
     * @param detectionArea the area to search for the number.
     * @param threshold the allowed error threshold allowed for the condition.
     *
     * @return the numeric value detected, or Double.MIN_VALUE if none.
     */
    fun detectNumber(
        detectionArea: Rect,
        threshold: Int,
        numberFormatType: NumberFormatType = NumberFormatType.AUTO,
    ): DetectionResult

    /** Release the resources of the screen image set with [setScreenBitmap]. */
    fun releaseScreenBitmap(screenBitmap: Bitmap)
}

/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = 400L