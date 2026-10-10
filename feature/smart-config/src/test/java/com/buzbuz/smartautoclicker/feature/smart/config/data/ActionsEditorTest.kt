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
package com.buzbuz.smartautoclicker.feature.smart.config.data

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionsEditorTest {

    private val parentFlow = MutableStateFlow<Event?>(null)
    private val listUpdates = mutableListOf<List<Action>>()

    private lateinit var editor: ActionsEditor<Event>

    @Before
    fun setUp() {
        editor = ActionsEditor(
            onListUpdated = { listUpdates.add(it) },
            parentItem = parentFlow,
        )
    }

    // --- Sub editors lifecycle ---

    @Test
    fun `startItemEdition with Intent starts intent extra editor with its extras`() {
        val extra = intentExtra(1)
        val intent = intent(1, extras = listOf(extra))
        editor.startEdition(listOf(intent))

        editor.startItemEdition(intent)

        assertEquals(listOf(extra), editor.intentExtraEditor.editedList.value)
        assertNull(editor.eventToggleEditor.editedList.value)
    }

    @Test
    fun `startItemEdition with Intent without extras starts intent extra editor with empty list`() {
        val intent = intent(1, extras = null)
        editor.startEdition(listOf(intent))

        editor.startItemEdition(intent)

        assertEquals(emptyList<IntentExtra<out Any>>(), editor.intentExtraEditor.editedList.value)
    }

    @Test
    fun `startItemEdition with ToggleEvent starts event toggle editor with its toggles`() {
        val toggle = eventToggle(1)
        val toggleEvent = toggleEvent(1, eventToggles = listOf(toggle))
        editor.startEdition(listOf(toggleEvent))

        editor.startItemEdition(toggleEvent)

        assertEquals(listOf(toggle), editor.eventToggleEditor.editedList.value)
        assertNull(editor.intentExtraEditor.editedList.value)
    }

    @Test
    fun `startItemEdition with other action does not start sub editors`() {
        val pause = pause(1)
        editor.startEdition(listOf(pause))

        editor.startItemEdition(pause)

        assertEquals(pause, editor.editedItem.value)
        assertNull(editor.intentExtraEditor.editedList.value)
        assertNull(editor.eventToggleEditor.editedList.value)
    }

    @Test
    fun `stopItemEdition stops intent extra editor`() {
        val intent = intent(1, extras = listOf(intentExtra(1)))
        editor.startEdition(listOf(intent))
        editor.startItemEdition(intent)

        editor.stopItemEdition()

        assertNull(editor.editedItem.value)
        assertNull(editor.intentExtraEditor.editedList.value)
    }

    @Test
    fun `stopItemEdition stops event toggle editor`() {
        val toggleEvent = toggleEvent(1, eventToggles = listOf(eventToggle(1)))
        editor.startEdition(listOf(toggleEvent))
        editor.startItemEdition(toggleEvent)

        editor.stopItemEdition()

        assertNull(editor.editedItem.value)
        assertNull(editor.eventToggleEditor.editedList.value)
    }

    @Test
    fun `switching from ToggleEvent to Intent edition resets event toggle editor`() {
        val toggleEvent = toggleEvent(1, eventToggles = listOf(eventToggle(1)))
        val intent = intent(2, extras = emptyList())
        editor.startEdition(listOf(toggleEvent, intent))
        editor.startItemEdition(toggleEvent)
        editor.stopItemEdition()

        editor.startItemEdition(intent)

        assertNull(editor.eventToggleEditor.editedList.value)
        assertEquals(emptyList<IntentExtra<out Any>>(), editor.intentExtraEditor.editedList.value)
    }

    // --- Sub editors propagation to edited action ---

    @Test
    fun `upsert in intent extra editor updates edited intent extras`() {
        val intent = intent(1, extras = emptyList())
        editor.startEdition(listOf(intent))
        editor.startItemEdition(intent)
        val extra = intentExtra(1)

        editor.intentExtraEditor.startItemEdition(extra)
        editor.intentExtraEditor.upsertEditedItem()

        assertEquals(intent.copy(extras = listOf(extra)), editor.editedItem.value)
        assertEquals(listOf(intent), editor.editedList.value)
    }

    @Test
    fun `delete in intent extra editor updates edited intent extras`() {
        val extra = intentExtra(1)
        val intent = intent(1, extras = listOf(extra))
        editor.startEdition(listOf(intent))
        editor.startItemEdition(intent)

        editor.intentExtraEditor.startItemEdition(extra)
        editor.intentExtraEditor.deleteEditedItem()

        assertEquals(intent.copy(extras = emptyList()), editor.editedItem.value)
    }

    @Test
    fun `upsert in event toggle editor updates edited toggle event toggles`() {
        val toggleEvent = toggleEvent(1, eventToggles = emptyList())
        editor.startEdition(listOf(toggleEvent))
        editor.startItemEdition(toggleEvent)
        val toggle = eventToggle(1)

        editor.eventToggleEditor.startItemEdition(toggle)
        editor.eventToggleEditor.upsertEditedItem()

        assertEquals(toggleEvent.copy(eventToggles = listOf(toggle)), editor.editedItem.value)
    }

    @Test
    fun `intent extra editor update is ignored when edited action is not an Intent`() {
        val pause = pause(1)
        editor.startEdition(listOf(pause))
        editor.startItemEdition(pause)
        editor.intentExtraEditor.startEdition(emptyList())

        editor.intentExtraEditor.updateList(listOf(intentExtra(1)))

        assertEquals(pause, editor.editedItem.value)
    }

    @Test
    fun `event toggle editor update is ignored when edited action is not a ToggleEvent`() {
        val pause = pause(1)
        editor.startEdition(listOf(pause))
        editor.startItemEdition(pause)
        editor.eventToggleEditor.startEdition(emptyList())

        editor.eventToggleEditor.updateList(listOf(eventToggle(1)))

        assertEquals(pause, editor.editedItem.value)
    }

    @Test
    fun `edited action changes are only committed to list on upsert`() = runTest {
        val intent = intent(1, extras = emptyList())
        editor.startEdition(listOf(intent))
        editor.startItemEdition(intent)
        val extra = intentExtra(1)
        editor.intentExtraEditor.startItemEdition(extra)
        editor.intentExtraEditor.upsertEditedItem()

        assertTrue(editor.editedItemState.first().hasChanged)
        assertTrue(listUpdates.isEmpty())

        editor.upsertEditedItem()

        assertEquals(listOf(intent.copy(extras = listOf(extra))), editor.editedList.value)
        assertEquals(1, listUpdates.size)
    }

    // --- itemCanBeSaved: non click actions ---

    @Test
    fun `null action can not be saved`() {
        assertFalse(editor.itemCanBeSaved(null, null))
    }

    @Test
    fun `complete non click action can be saved regardless of parent`() {
        val pause = pause(1)

        assertTrue(editor.itemCanBeSaved(pause, null))
        assertTrue(editor.itemCanBeSaved(pause, triggerEvent()))
        assertTrue(editor.itemCanBeSaved(pause, screenEvent(AND)))
    }

    @Test
    fun `incomplete non click action can not be saved`() {
        val pause = pause(1, name = null)

        assertFalse(editor.itemCanBeSaved(pause, null))
        assertFalse(editor.itemCanBeSaved(pause, triggerEvent()))
        assertFalse(editor.itemCanBeSaved(pause, screenEvent(AND)))
    }

    // --- itemCanBeSaved: click actions ---

    @Test
    fun `incomplete click can not be saved regardless of parent`() {
        val click = click(1, position = null)

        assertFalse(editor.itemCanBeSaved(click, null))
        assertFalse(editor.itemCanBeSaved(click, triggerEvent()))
        assertFalse(editor.itemCanBeSaved(click, screenEvent(AND)))
        assertFalse(editor.itemCanBeSaved(click, screenEvent(OR)))
    }

    @Test
    fun `click on coordinates can be saved regardless of parent`() {
        val click = click(1, position = coordinates())

        assertTrue(editor.itemCanBeSaved(click, null))
        assertTrue(editor.itemCanBeSaved(click, triggerEvent()))
        assertTrue(editor.itemCanBeSaved(click, screenEvent(AND)))
        assertTrue(editor.itemCanBeSaved(click, screenEvent(OR)))
    }

    @Test
    fun `click on condition can not be saved in trigger event`() {
        assertFalse(editor.itemCanBeSaved(click(1, position = Position.Condition(CONDITION_ID)), triggerEvent()))
        assertFalse(editor.itemCanBeSaved(click(1, position = Position.Condition(null)), triggerEvent()))
    }

    @Test
    fun `click on condition in AND screen event requires a condition id`() {
        assertTrue(editor.itemCanBeSaved(click(1, position = Position.Condition(CONDITION_ID)), screenEvent(AND)))
        assertFalse(editor.itemCanBeSaved(click(1, position = Position.Condition(null)), screenEvent(AND)))
    }

    @Test
    fun `click on condition in OR screen event can be saved without condition id`() {
        assertTrue(editor.itemCanBeSaved(click(1, position = Position.Condition(CONDITION_ID)), screenEvent(OR)))
        assertTrue(editor.itemCanBeSaved(click(1, position = Position.Condition(null)), screenEvent(OR)))
    }

    @Test
    fun `click on condition without parent can be saved`() {
        assertTrue(editor.itemCanBeSaved(click(1, position = Position.Condition(null)), null))
    }

    @Test
    fun `edited item state follows parent event changes`() = runTest {
        val click = click(1, position = Position.Condition(null))
        parentFlow.value = screenEvent(OR)
        editor.startEdition(listOf(click))
        editor.startItemEdition(click)

        assertTrue(editor.editedItemState.first().canBeSaved)
        assertTrue(editor.listState.first().canBeSaved)

        parentFlow.value = screenEvent(AND)

        assertFalse(editor.editedItemState.first().canBeSaved)
        val listState = editor.listState.first()
        assertFalse(listState.canBeSaved)
        assertEquals(listOf(false), listState.itemValidity)
    }

    private fun pause(id: Long, name: String? = "pause") = Pause(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = name,
        priority = 0,
        pauseDuration = 100L,
    )

    private fun click(id: Long, position: Position?) = Click(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "click",
        priority = 0,
        pressDuration = 100L,
        position = position,
    )

    private fun coordinates() = Position.Coordinates(
        x = CoordinatesValue.Literal(10),
        y = CoordinatesValue.Literal(20),
    )

    private fun intent(id: Long, extras: List<IntentExtra<out Any>>?) = Intent(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "intent",
        priority = 0,
        isAdvanced = false,
        isBroadcast = false,
        intentAction = "action",
        flags = 0,
        extras = extras,
    )

    private fun intentExtra(id: Long) = IntentExtra(
        id = Identifier(databaseId = id),
        actionId = ACTION_ID,
        key = "key",
        value = "value",
    )

    private fun toggleEvent(id: Long, eventToggles: List<EventToggle>) = ToggleEvent(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "toggle",
        priority = 0,
        toggleAll = false,
        eventToggles = eventToggles,
    )

    private fun eventToggle(id: Long) = EventToggle(
        id = Identifier(databaseId = id),
        actionId = ACTION_ID,
        targetEventId = EVENT_ID,
        toggleType = ToggleEvent.ToggleType.ENABLE,
    )

    private fun triggerEvent() = TriggerEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "trigger event",
        conditionOperator = AND,
    )

    private fun screenEvent(conditionOperator: Int) = ScreenEvent(
        id = EVENT_ID,
        scenarioId = SCENARIO_ID,
        name = "screen event",
        conditionOperator = conditionOperator,
        priority = 0,
        keepDetecting = false,
        cooldownMs = 0L,
    )

    private companion object {
        val SCENARIO_ID = Identifier(databaseId = 100L)
        val EVENT_ID = Identifier(databaseId = 10L)
        val ACTION_ID = Identifier(databaseId = 1L)
        val CONDITION_ID = Identifier(databaseId = 50L)
    }
}
