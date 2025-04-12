/*
 * Copyright (C) 2024 Kevin Buzeau
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
import androidx.annotation.Keep

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from github.
 */
class NativeDetector private constructor() : ScreenDetector {

    companion object {
        fun newInstance(): NativeDetector? = try {
            System.loadLibrary("smartautoclicker")
            NativeDetector()
        } catch (ex: UnsatisfiedLinkError) {
            null
        }
    }

    /** The results of the detection. Modified by native code. */
    @Keep
    private val detectionResult = DetectionResult()
    /** Native pointer of the detector object. */
    @Keep
    private var nativePtr: Long = -1

    private val detectionQualityMin: Double = DETECTION_QUALITY_MIN.toDouble()

    private var isClosed: Boolean = false

    override fun init() {
        nativePtr = newDetector()
    }

    override fun close() {
        if (isClosed) return

        isClosed = true
        deleteDetector()
    }

    override fun setTextMatchingLanguages(langCodes: String, trainingFilesPath: String) {
        if (isClosed) return
        if (langCodes.isBlank()) return

        setLanguages(langCodes, trainingFilesPath)
    }

    override fun setScreenMetrics(metricsKey: String, screenBitmap: Bitmap, detectionQuality: Double) {
        if (isClosed) return

        updateScreenMetrics(
            metricsKey,
            screenBitmap,
            detectionQuality.coerceIn(detectionQualityMin, 10000.0),
        )
    }

    override fun setupDetection(screenBitmap: Bitmap) {
        if (isClosed) return

        setScreenImage(screenBitmap)
    }

    override fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detect(conditionBitmap, threshold, detectionResult)
        return detectionResult.copy()
    }

    override fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detectAt(conditionBitmap, position.left, position.top, position.width(), position.height(), threshold, detectionResult)
        return detectionResult.copy()
    }

    override fun detectText(text: String, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detectText(text, threshold, detectionResult)
        return detectionResult.copy()
    }

    override fun detectText(text: String, position: Rect, threshold: Int): DetectionResult {
        if (isClosed) return detectionResult.copy()

        detectTextAt(text, position.left, position.top, position.width(), position.height(), threshold, detectionResult)
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

    /** Set the languages supported by the text matching. */
    private external fun setLanguages(langCodes: String, trainingFilesPath: String)

    /**
     * Native method for screen metrics setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     * @param detectionQuality the quality of the detection. The higher the preciser, the lower the faster. Must be
     *                         contained in [DETECTION_QUALITY_MIN] and [DETECTION_QUALITY_MAX].
     */
    private external fun updateScreenMetrics(metricsKey: String, screenBitmap: Bitmap, detectionQuality: Double)

    /**
     * Native method for detection setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    private external fun setScreenImage(screenBitmap: Bitmap)

    /**
     * Native method for detecting if the bitmap is in the whole current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detect(conditionBitmap: Bitmap, threshold: Int, result: DetectionResult)

    /**
     * Native method for detecting if the bitmap is at a specific position in the current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
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

    /**
     * Native method for detecting if the text is in the whole current screen bitmap.
     *
     * @param text the text to detect in the screen.
     * @param threshold the allowed error threshold allowed for the text.
     */
    private external fun detectText(text: String, threshold: Int, result: DetectionResult)

    /**
     * Native method for detecting if the text is at a specific position in the current screen bitmap.
     *
     * @param text the text to detect in the screen.
     * @param x the horizontal position of the text.
     * @param y the vertical position of the text.
     * @param width the width of the text.
     * @param height the height of the text.
     * @param threshold the allowed error threshold allowed for the text.
     */
    private external fun detectTextAt(
        text: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        result: DetectionResult
    )
}