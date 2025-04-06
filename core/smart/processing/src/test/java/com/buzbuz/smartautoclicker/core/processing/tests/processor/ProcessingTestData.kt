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
import android.graphics.Rect
import android.util.Size
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.normalizePriorities
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
import org.mockito.Mockito
import org.mockito.Mockito.`when`

/** Use this object to provide valid scenarios data during [ScenarioProcessor] tests. */
internal object ProcessingTestData {

    private const val TEST_DATA_SCREEN_IMAGE_WIDTH = 800
    private const val TEST_DATA_SCREEN_IMAGE_HEIGHT = 600
    private val TEST_DATA_SCREEN_SIZE = Size(TEST_DATA_SCREEN_IMAGE_WIDTH, TEST_DATA_SCREEN_IMAGE_HEIGHT)

    private const val TEST_DATA_CONDITION_IMAGE_WIDTH = 100
    private const val TEST_DATA_CONDITION_IMAGE_HEIGHT = 50

    private const val TEST_DATA_ACTION_PAUSE_DURATION_MS_DEFAULT = 50L

    private var scenarioIdIndex: Long = 0L
    private var eventIdIndex: Long = 0L
    private var conditionIdIndex: Long = 0L
    private var actionIdIndex: Long = 0L
    private var eventToggleIdIndex: Long = 0L

    fun reset() {
        scenarioIdIndex = 0L
        eventIdIndex = 0L
        conditionIdIndex = 0L
        actionIdIndex = 0L
        eventToggleIdIndex = 0L
    }

    fun newMockedScreenBitmap(size: Size = TEST_DATA_SCREEN_SIZE): Bitmap {
        val mockScreenBitmap = Mockito.mock(Bitmap::class.java)
        `when`(mockScreenBitmap.width).thenReturn(size.width)
        `when`(mockScreenBitmap.height).thenReturn(size.height)
        `when`(mockScreenBitmap.config).thenReturn(Bitmap.Config.ARGB_8888)

        return mockScreenBitmap
    }

    fun newScenarioId(): Identifier {
        scenarioIdIndex++
        return Identifier(databaseId = scenarioIdIndex)
    }

    fun newEventId(): Identifier {
        eventIdIndex++
        return Identifier(databaseId = eventIdIndex)
    }

    private fun newConditionId(): Identifier {
        conditionIdIndex++
        return Identifier(databaseId = conditionIdIndex)
    }

    private fun newActionId(): Identifier {
        actionIdIndex++
        return Identifier(databaseId = actionIdIndex)
    }

    private fun newEventToggleId(): Identifier {
        eventToggleIdIndex++
        return Identifier(databaseId = eventToggleIdIndex)
    }

    fun newTestScenario(
        scenarioId: Identifier,
        imageEvents: List<ImageEvent> = emptyList(),
        triggerEvents: List<TriggerEvent> = emptyList(),
    ): TestScenario {

        // Setup correct priorities
        imageEvents.normalizePriorities()

        return TestScenario(
            scenario = Scenario(
                id = scenarioId,
                eventCount = imageEvents.size,
                name = "TestScenario",   // No impact on processor
                detectionQuality = 1000, // No impact with mocked detection
                randomize = false,       // Always keep false, we dont want randomness in tests
            ),
            imageEvents = imageEvents,
            triggerEvents = triggerEvents,
        )
    }

    fun newTestImageEvent(
        eventId: Identifier,
        scenarioId: Identifier,
        enabledOnStart: Boolean = true,
        keepDetecting: Boolean = false,
        @ConditionOperator conditionOperator: Int = AND,
        conditions: List<TestImageCondition>,
        actions: List<Action>,
    ) = ImageEvent(
        id = eventId,
        scenarioId = scenarioId,
        enabledOnStart = enabledOnStart,
        keepDetecting = keepDetecting,
        conditionOperator = conditionOperator,
        conditions = conditions.map { it.imageCondition },
        actions = actions,
        name = "TestImageEvent",    // No impact on processor
        priority = 0,               // Set correctly once added to a test scenario
    )

    fun newTestImageCondition(
        eventId: Identifier,
        shouldBeDetected: Boolean = true,
        @DetectionType detectionType: Int = EXACT,
    ) : TestImageCondition {

        val conditionId = newConditionId()

        val condition = ImageCondition(
            id = conditionId,
            eventId = eventId,
            shouldBeDetected = shouldBeDetected,
            path = conditionId.databaseId.toString(),
            detectionType = detectionType,
            captureArea = Rect(0, 0, TEST_DATA_CONDITION_IMAGE_WIDTH, TEST_DATA_CONDITION_IMAGE_HEIGHT),
            detectionArea = Rect(0, 0, TEST_DATA_SCREEN_IMAGE_WIDTH, TEST_DATA_SCREEN_IMAGE_HEIGHT),
            threshold = 10,              // No impact with mocked detection
            name = "TestImageCondition", // No impact on processor
            priority = 0,
        )

        val mockScreenBitmap = Mockito.mock(Bitmap::class.java)
        `when`(mockScreenBitmap.width).thenReturn(TEST_DATA_CONDITION_IMAGE_WIDTH)
        `when`(mockScreenBitmap.height).thenReturn(TEST_DATA_CONDITION_IMAGE_HEIGHT)
        `when`(mockScreenBitmap.config).thenReturn(Bitmap.Config.ARGB_8888)

        return TestImageCondition(condition, mockScreenBitmap)
    }

    fun newTestTriggerEvent(
        eventId: Identifier,
        scenarioId: Identifier,
        enabledOnStart: Boolean = true,
        @ConditionOperator conditionOperator: Int = AND,
        conditions: List<TriggerCondition>,
        actions: List<Action>,
    ) = TriggerEvent(
        id = eventId,
        scenarioId = scenarioId,
        enabledOnStart = enabledOnStart,
        conditionOperator = conditionOperator,
        conditions = conditions,
        actions = actions,
        name = "TestImageEvent",    // No impact on processor
    )

    fun newTestCounterTriggerCondition(
        eventId: Identifier,
        counterName: String,
        operator: ComparisonOperation,
        value: Int = 0,
    ) = TriggerCondition.OnCounterCountReached(
            id = newConditionId(),
            eventId = eventId,
            counterName = counterName,
            comparisonOperation = operator,
            counterValue = CounterOperationValue.Number(value),
            name = "TestCounterCondition", // No impact on processor
        )

    fun newPauseAction(eventId: Identifier, durationMs: Long = TEST_DATA_ACTION_PAUSE_DURATION_MS_DEFAULT) =
        Pause(
            id = newActionId(),
            eventId = eventId,
            name = "TestToggleEventAction",
            priority = 0,
            pauseDuration = durationMs,
        )

    fun newCounterAction(
        eventId: Identifier,
        counterName: String,
        operator: ChangeCounter.OperationType,
        value: Int = 0,
    ) = ChangeCounter(
            id = newActionId(),
            eventId = eventId,
            counterName = counterName,
            operation = operator,
            operationValue = CounterOperationValue.Number(value),
            name = "TestToggleEventAction",
            priority = 0,
        )

    fun newToggleEventAction(eventId: Identifier, toggles: List<TestEventToggle>): ToggleEvent {
        val actionId = newActionId()

        return ToggleEvent(
            id = actionId,
            eventId = eventId,
            name = "TestToggleEventAction",
            priority = 0,
            toggleAll = false,
            toggleAllType = null,
            eventToggles = toggles.map { toggle ->
                EventToggle(
                    id = newEventToggleId(),
                    actionId = actionId,
                    targetEventId = toggle.targetId,
                    toggleType = toggle.toggleType,
                )
            }
        )
    }
}

