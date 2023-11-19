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
    ERROR_NO_NATIVE_LIB
}

internal fun DetectorState.toDetectionState(): DetectionState? = when (this) {
    DetectorState.CREATED -> DetectionState.INACTIVE
    DetectorState.RECORDING -> DetectionState.RECORDING
    DetectorState.DETECTING -> DetectionState.DETECTING
    DetectorState.DESTROYED -> DetectionState.INACTIVE
    DetectorState.TRANSITIONING -> null // Return null to avoid notifying state change when transitioning
    DetectorState.ERROR_NATIVE_DETECTOR_LIB_NOT_FOUND -> DetectionState.ERROR_NO_NATIVE_LIB
}