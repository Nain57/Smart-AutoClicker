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
package com.buzbuz.smartautoclicker.core.processing.tests.processor

import android.graphics.Bitmap
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResult
import com.buzbuz.smartautoclicker.core.processing.data.processor.DefaultResult
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScreenResult
import com.buzbuz.smartautoclicker.core.processing.domain.ConditionResult


internal data class TestScenario(
    val scenario: Scenario,
    val screenEvents: List<ScreenEvent>,
    val triggerEvents: List<TriggerEvent>,
)

internal data class TestImageCondition(
    val imageCondition: ImageCondition,
    val mockedBitmap: Bitmap,
)

internal data class TestEventToggle(
    val targetId: Identifier,
    val toggleType: ToggleEvent.ToggleType,
)

internal fun TestImageCondition.expectedResult(detected: Boolean) = ScreenResult(
    isFulfilled = detected == imageCondition.shouldBeDetected,
    haveBeenDetected = detected,
    condition = imageCondition,
    position = Point(0, 0),
    confidenceRate = 0.0,
)

internal fun TriggerEvent.expectedResult(detected: Boolean): List<ConditionResult> = ConditionsResult().apply {
    addResult(conditionId = id.databaseId, DefaultResult(isFulfilled = detected))
}.getAllResults()