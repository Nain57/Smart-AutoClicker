
package com.buzbuz.smartautoclicker.core.processing.tests.processor

import android.graphics.Bitmap
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResult
import com.buzbuz.smartautoclicker.core.processing.data.processor.DefaultResult
import com.buzbuz.smartautoclicker.core.processing.data.processor.ImageResult
import com.buzbuz.smartautoclicker.core.processing.domain.ConditionResult


internal data class TestScenario(
    val scenario: Scenario,
    val imageEvents: List<ImageEvent>,
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

internal fun TestImageCondition.expectedResult(detected: Boolean) = ImageResult(
    isFulfilled = detected == imageCondition.shouldBeDetected,
    haveBeenDetected = detected,
    condition = imageCondition,
    position = Point(0, 0),
    confidenceRate = 0.0,
)

internal fun TriggerEvent.expectedResult(detected: Boolean): List<ConditionResult> = ConditionsResult().apply {
    addResult(conditionId = id.databaseId, DefaultResult(isFulfilled = detected))
}.getAllResults()