
package com.buzbuz.smartautoclicker.core.base

import android.accessibilityservice.GestureDescription

/** Execute the actions related to Android. */
interface AndroidExecutor {

    /** Execute the provided gesture. */
    suspend fun executeGesture(gestureDescription: GestureDescription)
}

/** The maximum supported duration for a gesture. This limitation comes from Android GestureStroke API.  */
const val GESTURE_DURATION_MAX_VALUE = 59_999L