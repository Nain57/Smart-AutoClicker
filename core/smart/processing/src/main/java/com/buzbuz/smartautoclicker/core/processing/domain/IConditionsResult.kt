
package com.buzbuz.smartautoclicker.core.processing.domain

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition

interface IConditionsResult {

    val fulfilled: Boolean?

    fun getImageConditionResult(conditionId: Long): ImageConditionResult?
    fun getFirstImageDetectedResult(): ImageConditionResult?
    fun getAllResults(): List<ConditionResult>
}

interface ConditionResult {
    val isFulfilled: Boolean
}

interface ImageConditionResult : ConditionResult {
    val haveBeenDetected: Boolean
    val condition: ImageCondition
    val position: Point
    val confidenceRate: Double
}
