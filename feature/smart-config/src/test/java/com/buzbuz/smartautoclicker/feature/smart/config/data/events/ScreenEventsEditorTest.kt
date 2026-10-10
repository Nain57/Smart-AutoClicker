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

import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
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
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScreenEventsEditorTest {

    private val scenarioFlow = MutableStateFlow<Scenario?>(scenario())
    private val deletedEvents = mutableListOf<ScreenEvent>()

    private lateinit var editor: ScreenEventsEditor

    @Before
    fun setUp() {
        editor = ScreenEventsEditor(
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

    @Test
    fun `updateList normalizes event priorities`() {
        editor.startEdition(emptyList())

        editor.updateList(listOf(event(1, priority = 5), event(2, priority = 2)))

        assertEquals(listOf(0, 1), editor.editedList.value?.map { it.priority })
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

    // --- Actions editor propagation ---

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

    // --- Conditions editor propagation ---

    @Test
    fun `upsert in conditions editor updates edited event conditions`() {
        val event = event(1, actions = listOf(pause(1)))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)
        val condition = condition(1)

        editor.conditionsEditor.startItemEdition(condition)
        editor.conditionsEditor.upsertEditedItem()

        assertEquals(event.copy(conditions = listOf(condition)), editor.editedItem.value)
    }

    @Test
    fun `conditions editor update is ignored when no event is edited`() {
        editor.startEdition(listOf(event(1)))
        editor.conditionsEditor.startEdition(emptyList())

        editor.conditionsEditor.updateList(listOf(condition(1)))

        assertNull(editor.editedItem.value)
    }

    @Test
    fun `AND event condition removal deletes clicks referencing it`() {
        val condition1 = condition(1)
        val condition2 = condition(2)
        val clickOnCondition1 = click(1, position = Position.Condition(condition1.id))
        val clickOnCondition2 = click(2, position = Position.Condition(condition2.id))
        val clickOnCoordinates = click(3, position = coordinates())
        val pause = pause(4)
        val event = event(
            id = 1,
            conditionOperator = AND,
            conditions = listOf(condition1, condition2),
            actions = listOf(clickOnCondition1, clickOnCondition2, clickOnCoordinates, pause),
        )
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)

        editor.conditionsEditor.startItemEdition(condition1)
        editor.conditionsEditor.deleteEditedItem()

        val expectedActions = listOf(clickOnCondition2, clickOnCoordinates, pause)
        assertEquals(expectedActions, editor.actionsEditor.editedList.value)
        assertEquals(event.copy(conditions = listOf(condition2), actions = expectedActions), editor.editedItem.value)
    }

    @Test
    fun `AND event condition update deletes clicks on condition without reference`() {
        val condition = condition(1)
        val unreferencedClick = click(1, position = Position.Condition(null))
        val event = event(1, conditionOperator = AND, conditions = listOf(condition), actions = listOf(unreferencedClick))
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)

        val newCondition = condition(2)
        editor.conditionsEditor.startItemEdition(newCondition)
        editor.conditionsEditor.upsertEditedItem()

        assertEquals(emptyList<Action>(), editor.actionsEditor.editedList.value)
        assertEquals(event.copy(conditions = listOf(condition, newCondition), actions = emptyList()), editor.editedItem.value)
    }

    @Test
    fun `OR event condition update clears click condition references`() {
        val condition1 = condition(1)
        val condition2 = condition(2)
        val clickOnCondition = click(1, position = Position.Condition(condition1.id))
        val clickOnNoCondition = click(2, position = Position.Condition(null))
        val clickOnCoordinates = click(3, position = coordinates())
        val pause = pause(4)
        val event = event(
            id = 1,
            conditionOperator = OR,
            conditions = listOf(condition1),
            actions = listOf(clickOnCondition, clickOnNoCondition, clickOnCoordinates, pause),
        )
        editor.startEdition(listOf(event))
        editor.startItemEdition(event)

        editor.conditionsEditor.startItemEdition(condition2)
        editor.conditionsEditor.upsertEditedItem()

        val expectedActions = listOf(
            clickOnCondition.copy(position = Position.Condition(null)),
            clickOnNoCondition,
            clickOnCoordinates,
            pause,
        )
        assertEquals(expectedActions, editor.actionsEditor.editedList.value)
        assertEquals(
            event.copy(conditions = listOf(condition1, condition2), actions = expectedActions),
            editor.editedItem.value,
        )
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
            listOf(event2.copy(actions = listOf(expectedToggleEvent2)), event3.copy(priority = 1)),
            editor.editedList.value,
        )
    }

    @Test
    fun `deleteAllEventToggleReferencing does nothing when edition is not started`() {
        editor.deleteAllEventToggleReferencing(event(1))

        assertNull(editor.editedList.value)
    }

    private fun event(
        id: Long,
        conditionOperator: Int = AND,
        conditions: List<ScreenCondition> = emptyList(),
        actions: List<Action> = emptyList(),
        priority: Int = 0,
    ) = ScreenEvent(
        id = Identifier(databaseId = id),
        scenarioId = SCENARIO_ID,
        name = "event",
        conditionOperator = conditionOperator,
        conditions = conditions,
        actions = actions,
        priority = priority,
        keepDetecting = false,
        cooldownMs = 0L,
    )

    private fun condition(id: Long) = ScreenCondition.Color(
        id = Identifier(databaseId = id),
        eventId = EVENT_ID,
        name = "condition",
        threshold = 10,
        shouldBeDetected = true,
        priority = 0,
        color = 0xFF0000,
        detectionArea = Rect(0, 0, 10, 10),
    )

    private fun click(id: Long, position: Position) = Click(
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
