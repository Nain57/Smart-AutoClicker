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
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
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

class ReplaceMissingEventToggleReferenceUseCaseTest {

    private lateinit var useCase: ReplaceMissingEventToggleReferenceUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        useCase = ReplaceMissingEventToggleReferenceUseCase()
    }

    @Test
    fun `replace event toggles in toggle event action`() {
        val originalToggle = eventToggle(targetEventId = OLD_EVENT_ID)
        val action = toggleEvent(toggles = listOf(originalToggle))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.EventToggleReference("Missing Event")
        val replacementToggle = eventToggle(targetEventId = NEW_EVENT_ID)

        val result = useCase(event, item, missingRef, listOf(replacementToggle)) as TriggerEvent
        val updatedAction = result.actions[0] as ToggleEvent
        assertEquals(listOf(replacementToggle), updatedAction.eventToggles)
    }

    @Test
    fun `replacement toggles replaces full list not just one toggle`() {
        val toggle1 = eventToggle(targetEventId = OLD_EVENT_ID)
        val toggle2 = eventToggle(targetEventId = Identifier(databaseId = 99L))
        val action = toggleEvent(toggles = listOf(toggle1, toggle2))
        val event = triggerEventWithActions(listOf(action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.EventToggleReference("Missing Event")
        val newToggle = eventToggle(targetEventId = NEW_EVENT_ID)

        val result = useCase(event, item, missingRef, listOf(newToggle)) as TriggerEvent
        val updatedAction = result.actions[0] as ToggleEvent
        assertEquals(listOf(newToggle), updatedAction.eventToggles)
    }

    @Test
    fun `non-toggle-event action returns unchanged event`() {
        val pause = mockk<Pause>(relaxed = true)
        val event = triggerEventWithActions(listOf(pause))
        val item = ItemWithMissingReferences.ActionItem(item = pause, missingReferences = emptyList())
        val missingRef = MissingCopyReference.EventToggleReference("Missing Event")

        val result = useCase(event, item, missingRef, emptyList())
        assertEquals(event, result)
    }

    @Test
    fun `action not found in event returns unchanged event`() {
        val action = toggleEvent(toggles = listOf(eventToggle(targetEventId = OLD_EVENT_ID)))
        val otherAction = toggleEvent(toggles = listOf(eventToggle(targetEventId = NEW_EVENT_ID)))
        val event = triggerEventWithActions(listOf(otherAction))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.EventToggleReference("Missing Event")

        val result = useCase(event, item, missingRef, emptyList())
        assertEquals(event, result)
    }

    @Test
    fun `action position in event is preserved after replacement`() {
        val pause = mockk<Pause>(relaxed = true)
        val action = toggleEvent(toggles = listOf(eventToggle(targetEventId = OLD_EVENT_ID)))
        val event = triggerEventWithActions(listOf(pause, action))
        val item = ItemWithMissingReferences.ActionItem(item = action, missingReferences = emptyList())
        val missingRef = MissingCopyReference.EventToggleReference("Missing Event")
        val newToggle = eventToggle(targetEventId = NEW_EVENT_ID)

        val result = useCase(event, item, missingRef, listOf(newToggle)) as TriggerEvent
        assertEquals(pause, result.actions[0])
        val updatedAction = result.actions[1] as ToggleEvent
        assertEquals(listOf(newToggle), updatedAction.eventToggles)
    }

    private fun triggerEventWithActions(actions: List<com.buzbuz.smartautoclicker.core.domain.model.action.Action>) = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "test event",
        conditionOperator = AND,
        actions = actions,
        conditions = emptyList(),
    )

    private fun eventToggle(targetEventId: Identifier?) = EventToggle(
        id = Identifier(databaseId = 1L),
        actionId = ACTION_ID,
        targetEventId = targetEventId,
        toggleType = ToggleEvent.ToggleType.ENABLE,
    )

    private fun toggleEvent(toggles: List<EventToggle>) = ToggleEvent(
        id = ACTION_ID,
        eventId = EVENT_ID,
        priority = 0,
        toggleAll = false,
        eventToggles = toggles,
    )

    private companion object {
        val ACTION_ID = Identifier(databaseId = 1L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val SCENARIO_ID = Identifier(databaseId = 20L)
        val OLD_EVENT_ID = Identifier(databaseId = 30L)
        val NEW_EVENT_ID = Identifier(databaseId = 31L)
    }
}
