
package com.buzbuz.smartautoclicker.core.processing.data.scaling

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition

internal data class ImageConditionScalingInfo(
    val imageCondition: ImageCondition,
    val imageArea: Rect,
    val detectionArea: Rect,
)