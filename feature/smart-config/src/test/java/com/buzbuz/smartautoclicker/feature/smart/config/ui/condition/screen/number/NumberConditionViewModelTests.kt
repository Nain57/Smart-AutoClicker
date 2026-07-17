/*
 * Copyright (C) 2026 Kevin Buzeau
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.number

import android.content.Context
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.condition.NumberFormatType
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiNumberFormatDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class NumberConditionViewModelTests {

    @Test
    fun changesKeepUnrelatedNumberConditionFieldsAndUpdateSelectedValues() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val event = ScreenEvent(
            id = Identifier(databaseId = 2L), scenarioId = scenario.id, name = "Event", conditionOperator = AND,
            priority = 0, keepDetecting = false, cooldownMs = 0,
        )
        val condition = ScreenCondition.Number(
            id = Identifier(databaseId = 3L), eventId = event.id, name = "Amount", threshold = 80, priority = 0,
            detectionArea = Rect(1, 2, 30, 40), comparisonOperation = ComparisonOperation.EQUALS,
            counterValue = CounterOperationValue.Number(12.0), numberFormatType = NumberFormatType.AUTO,
        )
        val editionRepository = createEditionRepository(scenario, screenEvents = listOf(event))
        editionRepository.startEventEdition(event)
        editionRepository.startConditionEdition(condition)
        val viewModel = NumberConditionViewModel(mockk<Context>(), editionRepository, mockk<MonitoredViewsManager>())

        viewModel.setNumberFormat(UiNumberFormatDropdownItem.CommaDecimal)
        viewModel.setComparisonOperator(UiCounterOperatorDropdownItem.Comparison.GreaterOrEqualsItem)
        viewModel.setThreshold(65)
        viewModel.setDetectionArea(Rect(5, 6, 70, 80))
        viewModel.setOperandType(UiOperandType.COUNTER)

        assertEquals(
            condition.copy(
                threshold = 65,
                detectionArea = Rect(5, 6, 70, 80),
                comparisonOperation = ComparisonOperation.GREATER_OR_EQUALS,
                counterValue = CounterOperationValue.Counter(""),
                numberFormatType = NumberFormatType.COMMA_DECIMAL,
            ),
            editionRepository.editionState.getEditedCondition<ScreenCondition.Number>(),
        )

        viewModel.setOperandType(UiOperandType.STATIC)

        assertEquals(
            CounterOperationValue.Number(0.0),
            editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.counterValue,
        )
    }
}
