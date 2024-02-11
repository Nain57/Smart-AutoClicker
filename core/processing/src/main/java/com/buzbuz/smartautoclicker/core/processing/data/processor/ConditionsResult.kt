/*
 * Copyright (C) 2024 Kevin Buzeau
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


