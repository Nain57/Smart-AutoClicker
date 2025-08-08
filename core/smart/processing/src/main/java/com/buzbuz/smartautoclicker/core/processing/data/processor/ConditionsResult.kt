
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.graphics.Point

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.processing.domain.ConditionResult
import com.buzbuz.smartautoclicker.core.processing.domain.IConditionsResult
import com.buzbuz.smartautoclicker.core.processing.domain.ImageConditionResult


internal class ConditionsResult : IConditionsResult {

    private val _results: MutableMap<Long, ConditionResult> = mutableMapOf()

    override var fulfilled: Boolean? = null
        private set

    override fun getImageConditionResult(conditionId: Long): ImageConditionResult? =
        _results[conditionId]?.let { result ->
            if (result is ImageResult) result else null
        }

    override fun getFirstImageDetectedResult(): ImageResult? =
        _results.values.find { it is ImageResult && it.isFulfilled && it.condition.shouldBeDetected }
                as ImageResult?

    override fun getAllResults(): List<ConditionResult> = buildList {
        _results.forEach { (_, result) -> add(result) }
    }

    fun reset() {
        _results.clear()
        fulfilled = null
    }

    fun addResult(conditionId: Long, result: ConditionResult) {
        if (fulfilled != null) return
        _results[conditionId] = result
    }
    fun setFulfilledState(state: Boolean) {
        fulfilled = state
    }
}

internal data class DefaultResult(
    override val isFulfilled: Boolean,
) : ConditionResult

internal data class ImageResult(
    override val isFulfilled: Boolean,
    override val haveBeenDetected: Boolean,
    override val condition: ImageCondition,
    override val position: Point = Point(),
    override var confidenceRate: Double = 0.0,
) : ImageConditionResult


