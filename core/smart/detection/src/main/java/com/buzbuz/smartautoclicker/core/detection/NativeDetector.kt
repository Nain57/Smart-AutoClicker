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
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.Keep
import com.buzbuz.smartautoclicker.core.base.extensions.throwWithKeys

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from GitHub.
 */
class NativeDetector private constructor() : ImageDetector {

    companion object {
        fun newInstance(): NativeDetector? = try {
            System.loadLibrary("smartautoclicker")
            NativeDetector()
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }

    /** Native pointer of the detector object. */
    @Keep
    private var nativePtr: Long = -1

    private var isClosed: Boolean = false
    private var screenDimensions: Point = Point(0, 0)

    override fun init() {
        nativePtr = newDetector()
    }

    override fun close() {
        if (isClosed) return

        isClosed = true
        deleteDetector()
    }

    override fun loadTextDetectionModels(detectionModelPath: String, recognitionModels: Map<String, String>): Boolean {
        if (isClosed) return false

        return loadDetectionModels(
            detectionModelPath = detectionModelPath,
            recognitionModelIds = recognitionModels.keys.toTypedArray(),
            recognitionModelsPaths = recognitionModels.values.toTypedArray(),
        )
    }


    override fun setScreenBitmap(screenBitmap: Bitmap, metadata: String) {
        if (isClosed) return

        screenDimensions.x = screenBitmap.width
        screenDimensions.y = screenBitmap.height
        setScreenImage(screenBitmap, metadata)
    }

    override fun detectImage(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectImageNative(
                conditionBitmap,
                conditionWidth,
                conditionHeight,
                detectionArea.left,
                detectionArea.top,
                detectionArea.width(),
                detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithKeys(
                keys = mapOf(
                    "screenSize" to "${screenDimensions.x}x${screenDimensions.y}",
                    "originalConditionSize" to "${conditionBitmap.width}x${conditionBitmap.height}",
                    "conditionSize" to "${conditionWidth}x$conditionHeight",
                    "detectionArea" to detectionArea.toString(),
                    "threshold" to threshold.toString(),
                ),
            )
            DetectionResult()
        }
    }

    override fun detectColor(conditionColor: Int, detectionArea: Rect, threshold: Int): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectColorNative(
                conditionColor,
                detectionArea.left,
                detectionArea.top,
                detectionArea.width(),
                detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithKeys(
                keys = mapOf(
                    "screenSize" to "${screenDimensions.x}x${screenDimensions.y}",
                    "conditionColor" to conditionColor.toString(),
                    "detectionArea" to detectionArea.toString(),
                    "threshold" to threshold.toString(),
                ),
            )
            DetectionResult()
        }
    }

    override fun detectText(
        conditionText: String,
        recognitionModelId: String,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult {

        if (isClosed) return DetectionResult()

        return try {
            detectTextNative(
                conditionText = conditionText,
                recognitionModelId = recognitionModelId,
                x = detectionArea.left,
                y = detectionArea.top,
                width = detectionArea.width(),
                height = detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithKeys(
                keys = mapOf(
                    "screenSize" to "${screenDimensions.x}x${screenDimensions.y}",
                    "recognitionModelId" to recognitionModelId,
                    "conditionText" to conditionText,
                    "detectionArea" to detectionArea.toString(),
                    "threshold" to threshold.toString(),
                ),
            )
            DetectionResult()
        }
    }

    override fun detectNumber(detectionArea: Rect, threshold: Int, numberFormatType: NumberFormatType): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectNumberNative(
                x = detectionArea.left,
                y = detectionArea.top,
                width = detectionArea.width(),
                height = detectionArea.height(),
                threshold = threshold,
                numberFormat = numberFormatType.ordinal,
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithKeys(
                keys = mapOf(
                    "screenSize" to "${screenDimensions.x}x${screenDimensions.y}",
                    "detectionArea" to detectionArea.toString(),
                    "threshold" to threshold.toString(),
                ),
            )
            DetectionResult()
        }
    }

    override fun releaseScreenBitmap(screenBitmap: Bitmap) {
        if (isClosed) return
        releaseScreenImage(screenBitmap)
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
     *
     */
    private external fun loadDetectionModels(
        detectionModelPath: String,
        recognitionModelIds: Array<String>,
        recognitionModelsPaths: Array<String>,
    ): Boolean

    /**
     * Native method for detection setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    private external fun setScreenImage(screenBitmap: Bitmap, metricsTag: String)

    /**
     * Native method for detecting if the bitmap is at a specific position in the current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param conditionWidth the expected width of the condition at detection time.
     * @param conditionHeight the expected height of the condition at detection time.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectImageNative(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if the color is at a specific position in the current screen bitmap.
     *
     * @param conditionColor the condition to detect in the screen.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectColorNative(
        conditionColor: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if the text is at a specific position in the current screen bitmap.
     *
     * @param conditionText the condition to detect in the screen.
     * @param recognitionModelId the identifier of the recognition model specified with [init].
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectTextNative(
        conditionText: String,
        recognitionModelId: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if a number is at a specific position in the current screen bitmap.
     *
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectNumberNative(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        numberFormat: Int,
    ): DoubleArray?

    /** Native method for releasing the screen image resources set with [setScreenImage]. */
    private external fun releaseScreenImage(screenBitmap: Bitmap)
}