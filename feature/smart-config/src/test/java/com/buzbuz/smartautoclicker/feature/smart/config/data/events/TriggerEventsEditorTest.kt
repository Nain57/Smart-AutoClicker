/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.data.events

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TriggerEventsEditorTest {

    private val scenarioFlow = MutableStateFlow<Scenario?>(scenario())
    private val deletedEvents = mutableListOf<TriggerEvent>()

    private lateinit var editor: TriggerEventsEditor

    @Before
    fun setUp() {
        editor = TriggerEventsEditor(
            onDeleteEvent = { deletedEvents.add(it) },
            parentItem = scenarioFlow,
        )
    }

    // --- Sub editors lifecycle ---

    @Test
    fun `startItemEdition starts conditions and actions editors with event children`() {
        val condition = condition(1)
        val action = pause(1)
        val event = event(1, conditions = listOf(condition), actions = listOf(action))
        editor.startEdition(listOf(event))

        editor.startItemEdition(event)

        assertEquals(event, editor.editedItem.value)
        assertEquals(listOf(condition), editor.conditionsEditor.editedList.value)
        assertEquals(listOf(action), editor.actionsEditor.editedList.value)
    }

    @Test
    fun `stopItemEdition stops conditions and actions editors`() {
        val event = event(1, conditions = listOf(condition(1)), actions = listOf(pause(1)))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)

        editor.stopItemEdition()

        assertNull(editor.editedItem.value)
        assertNull(editor.conditionsEditor.editedList.value)
        assertNull(editor.actionsEditor.editedList.value)
        assertEquals(listOf(event), editor.editedList.value)
    }

    @Test
    fun `empty event list can be saved`() = runTest {
        editor.startEdition(emptyList())

        assertTrue(editor.listState.first().canBeSaved)
    }

    // --- Delete ---

    @Test
    fun `deleteEditedItem notifies onDeleteEvent then removes event from list`() {
        val event1 = event(1)
        val event2 = event(2)
        editor.startEdition(listOf(event1, event2))
        editor.startItemEdition(event1)

        editor.deleteEditedItem()

        assertEquals(listOf(event1), deletedEvents)
        assertEquals(listOf(event2), editor.editedList.value)
        assertFalse(editor.isItemEditionStarted())
    }

    @Test
    fun `deleteEditedItem without edited event does nothing`() {
        val events = listOf(event(1))
        editor.startEdition(events)

        editor.deleteEditedItem()

        assertTrue(deletedEvents.isEmpty())
        assertEquals(events, editor.editedList.value)
    }

    // --- Sub editors propagation to edited event ---

    @Test
    fun `upsert in actions editor updates edited event actions`() {
        val event = event(1, conditions = listOf(condition(1)))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)
        val action = pause(1)

        editor.actionsEditor.startItemEdition(action)
        editor.actionsEditor.upsertEditedItem()

        assertEquals(event.copy(actions = listOf(action)), editor.editedItem.value)
        assertEquals(listOf(event), editor.editedList.value)
    }

    @Test
    fun `upsert in conditions editor updates edited event conditions`() {
        val event = event(1, actions = listOf(pause(1)))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)
        val condition = condition(1)

        editor.conditionsEditor.startItemEdition(condition)
        editor.conditionsEditor.upsertEditedItem()

        assertEquals(event.copy(conditions = listOf(condition)), editor.editedItem.value)
        assertEquals(listOf(event), editor.editedList.value)
    }

    @Test
    fun `delete in conditions editor updates edited event conditions`() {
        val condition = condition(1)
        val event = event(1, conditions = listOf(condition))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)

        editor.conditionsEditor.startItemEdition(condition)
        editor.conditionsEditor.deleteEditedItem()

        assertEquals(event.copy(conditions = emptyList()), editor.editedItem.value)
    }

    @Test
    fun `conditions editor update is ignored when no event is edited`() {
        editor.startEdition(listOf(event(1)))
        editor.conditionsEditor.startEdition(emptyList())

        editor.conditionsEditor.updateList(listOf(condition(1)))

        assertNull(editor.editedItem.value)
    }

    @Test
    fun `edited event changes are only committed to list on upsert`() = runTest {
        val event = event(1, conditions = listOf(condition(1)))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)
        val action = pause(1)
        editor.actionsEditor.startItemEdition(action)
        editor.actionsEditor.upsertEditedItem()

        assertTrue(editor.editedItemState.first().hasChanged)
        assertFalse(editor.listState.first().hasChanged)

        editor.upsertEditedItem()

        assertEquals(listOf(event.copy(actions = listOf(action))), editor.editedList.value)
        assertTrue(editor.listState.first().hasChanged)
    }

    // --- deleteAllEventToggleReferencing ---

    @Test
    fun `deleteAllEventToggleReferencing removes event and toggles targeting it`() {
        val event1 = event(1)
        val toggleEvent2 = toggleEvent(2, targets = listOf(event1.id, Identifier(databaseId = 3)))
        val event2 = event(2, actions = listOf(toggleEvent2))
        val event3 = event(3, actions = listOf(toggleEvent(3, targets = listOf(event2.id)), pause(4)))
        editor.startEdition(listOf(event1, event2, event3))

        editor.deleteAllEventToggleReferencing(event1)

        val expectedToggleEvent2 = toggleEvent2.copy(
            eventToggles = toggleEvent2.eventToggles.filter { it.targetEventId != event1.id },
        )
        assertEquals(
            listOf(event2.copy(actions = listOf(expectedToggleEvent2)), event3),
            editor.editedList.value,
        )
    }

    @Test
    fun `deleteAllEventToggleReferencing keeps other events untouched`() {
        val event1 = event(1)
        val event2 = event(2, actions = listOf(pause(2)))
        editor.startEdition(listOf(event1, event2))

        editor.deleteAllEventToggleReferencing(event1)

        assertEquals(listOf(event2), editor.editedList.value)
    }

    @Test
    fun `deleteAllEventToggleReferencing does nothing when edition is not started`() {
        editor.deleteAllEventToggleReferencing(event(1))

        assertNull(editor.editedList.value)
    }

    private fun event(
        id: Long,
        conditions: List<TriggerCondition> = emptyList(),
        actions: List<Action> = emptyList(),
    ) = TriggerEvent(
        id = Identifier(databaseId = id),
        scenarioId = SCENARIO_ID,
        name = "event",
        conditionOperator = AND,
        conditions = conditions,
        actions = actions,
    )

    private fun condition(id: Long) = TriggerCondition.OnTimerReached(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "condition",
        durationMs = 1000L,
        restartWhenReached = false,
    )

    private fun pause(id: Long) = Pause(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "pause",
        priority = 0,
        pauseDuration = 100L,
    )

    private fun toggleEvent(id: Long, targets: List<Identifier>) = ToggleEvent(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "toggle",
        priority = 0,
        eventToggles = targets.mapIndexed { index, target ->
            EventToggle(
                id = Identifier(databaseId = id * 100 + index),
                actionId = Identifier(databaseId = id),
                targetEventId = target,
                toggleType = ToggleEvent.ToggleType.ENABLE,
            )
        },
    )

    private fun scenario() = Scenario(
        id = SCENARIO_ID,
        name = "scenario",
        detectionQuality = 600,
    )

    private companion object {
        val SCENARIO_ID = Identifier(databaseId = 100L)
        val EVENT_ID = Identifier(databaseId = 10L)
    }
}
