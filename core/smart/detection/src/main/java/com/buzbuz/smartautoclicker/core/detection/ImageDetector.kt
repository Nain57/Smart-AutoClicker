
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Detects bitmaps within other bitmaps for conditions detection on the screen.
 * All calls should be made on the same thread.
 */
interface ImageDetector : AutoCloseable {

    /** Initialize the detector. Must be called on the same thread as the detection. */
    fun init()

    /**
     * Set the bitmap for the screen.
     * All following calls to [detectCondition] methods will be verified against this bitmap.
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
    fun detectCondition(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult

    /** Release the resources of the screen image set with [setScreenBitmap]. */
    fun releaseScreenBitmap(screenBitmap: Bitmap)
}

/** The minimum detection quality for the algorithm. */
const val DETECTION_QUALITY_MIN = 400L