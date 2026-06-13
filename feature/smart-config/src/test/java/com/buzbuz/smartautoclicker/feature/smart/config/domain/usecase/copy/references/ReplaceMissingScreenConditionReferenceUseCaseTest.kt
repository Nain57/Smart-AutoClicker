/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import android.util.Log

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReplaceMissingScreenConditionReferenceUseCaseTest {

    private lateinit var useCase: ReplaceMissingScreenConditionReferenceUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        useCase = ReplaceMissingScreenConditionReferenceUseCase()
    }

    @Test
    fun `replace click condition id with replacement condition id`() {
        val oldConditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val click = click(clickOnConditionId = oldConditionId)
        val event = triggerEventWithActions(listOf(click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = oldConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        val updatedClick = result.actions[0] as Click
        assertEquals(replacementConditionId, updatedClick.clickOnConditionId)
    }

    @Test
    fun `non-click action returns unchanged event`() {
        val pause = mockk<Pause>(relaxed = true)
        val event = triggerEventWithActions(listOf(pause))
        val item = ItemWithMissingReferences.ActionItem(item = pause, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = Identifier(databaseId = 100L))
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `click with non-matching condition id returns unchanged event`() {
        val clickConditionId = Identifier(databaseId = 100L)
        val differentConditionId = Identifier(databaseId = 999L)
        val click = click(clickOnConditionId = clickConditionId)
        val event = triggerEventWithActions(listOf(click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Other Condition", conditionId = differentConditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true)

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `action not found in event returns unchanged event`() {
        val conditionId = Identifier(databaseId = 100L)
        val click = click(clickOnConditionId = conditionId)
        val otherClick = click(clickOnConditionId = Identifier(databaseId = 999L))
        val event = triggerEventWithActions(listOf(otherClick))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = conditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns Identifier(databaseId = 200L)
        }

        val result = useCase(event, item, missingRef, replacement)
        assertEquals(event, result)
    }

    @Test
    fun `action position in event is preserved after replacement`() {
        val pause = mockk<Pause>(relaxed = true)
        val conditionId = Identifier(databaseId = 100L)
        val replacementConditionId = Identifier(databaseId = 200L)
        val click = click(clickOnConditionId = conditionId)
        val event = triggerEventWithActions(listOf(pause, click))
        val item = ItemWithMissingReferences.ActionItem(item = click, missingReferences = emptyList())
        val missingRef = MissingCopyReference.ScreenConditionReference("Old Condition", conditionId = conditionId)
        val replacement = mockk<ScreenCondition.Image>(relaxed = true) {
            every { id } returns replacementConditionId
        }

        val result = useCase(event, item, missingRef, replacement) as TriggerEvent
        assertEquals(pause, result.actions[0])
        val updatedClick = result.actions[1] as Click
        assertEquals(replacementConditionId, updatedClick.clickOnConditionId)
    }

    private fun triggerEventWithActions(actions: List<com.buzbuz.smartautoclicker.core.domain.model.action.Action>) = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "test event",
        conditionOperator = AND,
        actions = actions,
        conditions = emptyList(),
    )

    private fun click(clickOnConditionId: Identifier?) = Click(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        positionType = Click.PositionType.ON_DETECTED_CONDITION,
        clickOnConditionId = clickOnConditionId,
        pressDuration = 100L,
    )

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val SCENARIO_ID = Identifier(databaseId = 20L)
    }
}
