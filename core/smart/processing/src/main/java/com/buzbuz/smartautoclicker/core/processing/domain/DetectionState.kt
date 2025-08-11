
package com.buzbuz.smartautoclicker.core.processing.domain

import com.buzbuz.smartautoclicker.core.processing.data.DetectorState

/** The different states of the detection. */
enum class DetectionState {
    /** The detection is inactive. */
    INACTIVE,
    /** The screen is being recorded. */
    RECORDING,
    /** The screen is being recorded and the detection is running. */
    DETECTING,
    /** The native lib is not found and the detector can't work. */
    ERROR_NO_NATIVE_LIB,
}

internal fun DetectorState.toDetectionState(): DetectionState? = when (this) {
    DetectorState.CREATED -> DetectionState.INACTIVE
    DetectorState.RECORDING -> DetectionState.RECORDING
    DetectorState.DETECTING -> DetectionState.DETECTING
    DetectorState.DESTROYED -> DetectionState.INACTIVE
    DetectorState.TRANSITIONING -> null // Return null to avoid notifying state change when transitioning
    DetectorState.ERROR_NATIVE_DETECTOR_LIB_NOT_FOUND -> DetectionState.ERROR_NO_NATIVE_LIB
}