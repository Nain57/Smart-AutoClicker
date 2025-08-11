
package com.buzbuz.smartautoclicker.feature.smart.debugging.domain

import android.graphics.Point
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent

/** Debug information for the scenario processing */
data class DebugInfo(
    val event: ImageEvent,
    val condition: ImageCondition,
    val isDetected: Boolean,
    val position: Point = Point(),
    val confidenceRate: Double,
    val conditionArea: Rect,
)