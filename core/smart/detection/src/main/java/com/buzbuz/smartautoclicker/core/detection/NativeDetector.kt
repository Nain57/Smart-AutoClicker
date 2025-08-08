
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
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from github.
 */
class NativeDetector private constructor() : ImageDetector {

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

    override fun setScreenBitmap(screenBitmap: Bitmap, metadata: String) {
        if (isClosed) return

        screenDimensions.x = screenBitmap.width
        screenDimensions.y = screenBitmap.height
        setScreenImage(screenBitmap, metadata)
    }

    override fun detectCondition(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult {
        if (isClosed) return detectionResult.copy()

        try {
            detect(conditionBitmap, conditionWidth, conditionHeight, detectionArea.left, detectionArea.top,
                detectionArea.width(), detectionArea.height(), threshold, detectionResult)
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
        }

        return detectionResult.copy()
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
    private external fun detect(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        result: DetectionResult,
    )

    /** Native method for releasing the screen image resources set with [setScreenImage]. */
    private external fun releaseScreenImage(screenBitmap: Bitmap)
}