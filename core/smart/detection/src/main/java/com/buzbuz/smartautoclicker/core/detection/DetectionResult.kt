
package com.buzbuz.smartautoclicker.core.detection

import android.graphics.Point
import androidx.annotation.Keep

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