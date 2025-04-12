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
package com.buzbuz.smartautoclicker.core.processing.domain

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition

interface IConditionsResult {

    val fulfilled: Boolean?

    fun getScreenConditionResult(conditionId: Long): ScreenConditionResult?
    fun getFirstScreenDetectedResult(): ScreenConditionResult?
    fun getAllResults(): List<ConditionResult>
}

interface ConditionResult {
    val isFulfilled: Boolean
}

interface ScreenConditionResult : ConditionResult {
    val haveBeenDetected: Boolean
    val condition: ScreenCondition
    val position: Point
    val confidenceRate: Double
}
