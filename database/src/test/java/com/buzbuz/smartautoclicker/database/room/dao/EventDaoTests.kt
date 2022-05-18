/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room.dao

import android.os.Build

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.utils.TestsData
import com.buzbuz.smartautoclicker.database.utils.TestsData.cloneActions
import com.buzbuz.smartautoclicker.database.utils.TestsData.cloneEvent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [EventDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EventDaoTests {

    /** In memory database used to test the dao. */
    private lateinit var database: ClickDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, ClickDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Create a scenario to host all our test events
        runBlocking {
            database.scenarioDao().add(TestsData.getNewScenarioEntity(id = TestsData.SCENARIO_ID))
        }
    }

    @After
    fun tearDown() {
        database.clearAllTables()
        database.close()
    }

    @Test
    fun addEvent() = runTest {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(
                TestsData.getNewIntentEntity(
                    id = 1,
                    eventId = TestsData.EVENT_ID,
                    priority = 0,
                    intentExtras = mutableListOf(TestsData.getNewIntentExtraEntity(actionId = 1, value = "20"))
                )
            ),
            conditions = listOf(TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID))
        )
        database.eventDao().addCompleteEvent(completeEvent.cloneEvent())

        assertEquals(completeEvent, database.eventDao().getEvent(TestsData.EVENT_ID))
    }

    @Test
    fun addEvent_setIds() = runTest {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(
                TestsData.getNewIntentEntity(
                    eventId = 0L,
                    priority = 0,
                    intentExtras = mutableListOf(TestsData.getNewIntentExtraEntity(id = 0, actionId = 0, value = "20"))
                )
            ),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = 0L))
        )
        database.eventDao().addCompleteEvent(completeEvent)

        val databaseEvent = database.eventDao().getCompleteEvents(TestsData.SCENARIO_ID).first()[0]
        assertEquals(databaseEvent.event.id, databaseEvent.actions[0].action.eventId)
        assertEquals(databaseEvent.event.id, databaseEvent.conditions[0].eventId)
        assertEquals(databaseEvent.actions[0].action.id, databaseEvent.actions[0].intentExtras[0].actionId)
    }

    @Test
    fun addEvent_setActionPriority() = runTest {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(
                TestsData.getNewPauseEntity(id = 1, eventId = 0L, priority = 0),
                TestsData.getNewPauseEntity(id = 2, eventId = 0L, priority = 0),
                TestsData.getNewPauseEntity(id = 3, eventId = 0L, priority = 0),
            ),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = 0L))
        )
        database.eventDao().addCompleteEvent(completeEvent)

        val databaseEvent = database.eventDao().getEvent(TestsData.EVENT_ID)
        databaseEvent.actions.forEachIndexed { index, completeAction ->
            assertEquals("Invalid action position", index + 1, completeAction.action.id.toInt())
            assertEquals("Invalid action priority value", index, completeAction.action.priority)
        }
    }

    @Test
    fun deleteEvent() = runTest {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = 0L, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = 0L))
        )
        database.eventDao().addCompleteEvent(completeEvent)

        database.eventDao().deleteEvent(completeEvent.event)

        assertNull(database.eventDao().getEvent(TestsData.EVENT_ID))
    }

    @Test
    fun updateEvent() = runTest {
        // First add the old event
        val updatedId = 1L
        val deletedId = 2L
        val intentId = 3L
        val oldEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(
                id = TestsData.EVENT_ID,
                scenarioId = TestsData.SCENARIO_ID,
                priority = 0,
                name = "Event Old"
            ),
            actions = listOf(
                TestsData.getNewPauseEntity(id = updatedId, eventId = 0L, priority = 0, pauseDuration = 100L),
                TestsData.getNewPauseEntity(id = deletedId, eventId = 0L, priority = 1),
                TestsData.getNewIntentEntity(id = intentId, eventId = 0L, priority = 2, intentExtras = emptyList()),
            ),
            conditions = listOf(
                TestsData.getNewConditionEntity(id = updatedId, eventId = 0L, path="titi"),
                TestsData.getNewConditionEntity(id = deletedId, eventId = 0L),
            )
        )
        database.eventDao().addCompleteEvent(oldEvent.cloneEvent())

        // Then update with the new one.
        val updatedExtraList = mutableListOf(TestsData.getNewIntentExtraEntity(value = "20"))
        val updatedIntent = TestsData.getNewIntentEntity(id = intentId, eventId =  TestsData.EVENT_ID, priority = 2, intentExtras = updatedExtraList)
        val updatedAction = TestsData.getNewPauseEntity(id = updatedId, eventId = TestsData.EVENT_ID, priority = 1, pauseDuration = 1000L)
        val updatedCondition = TestsData.getNewConditionEntity(id = updatedId, eventId = TestsData.EVENT_ID, path="tutu")
        val newEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(
                id = TestsData.EVENT_ID,
                scenarioId = TestsData.SCENARIO_ID,
                priority = 0,
                name = "Event New"
            ),
            actions = listOf(
                TestsData.getNewPauseEntity(id = DEFAULT_PRIMARY_KEY, eventId = 0L, priority = 0),
                updatedAction,
                updatedIntent,
            ),
            conditions = listOf(
                TestsData.getNewConditionEntity(id = updatedId, eventId = TestsData.EVENT_ID, path="tutu"),
                TestsData.getNewConditionEntity(id = DEFAULT_PRIMARY_KEY, eventId = 0L),
            )
        )

        database.eventDao().updateCompleteEvent(newEvent)

        val databaseEvent = database.eventDao().getEvent(TestsData.EVENT_ID)
        assertEquals(newEvent.event.name, databaseEvent.event.name)
        assertEquals(newEvent.actions.size, databaseEvent.actions.size)
        databaseEvent.actions.forEach {
            if (it.action.id == updatedId) assertEquals(updatedAction.action.pauseDuration, it.action.pauseDuration)
        }
        assertEquals(newEvent.conditions.size, databaseEvent.conditions.size)
        databaseEvent.conditions.forEach {
            if (it.id == updatedId) assertEquals(updatedCondition.path, it.path)
        }
        assertEquals(newEvent.actions[0].intentExtras, databaseEvent.actions[0].intentExtras)
    }

    @Test
    fun getEventIds() = runTest {
        val eventId1 = TestsData.EVENT_ID
        val eventId2 = TestsData.EVENT_ID + 1
        val eventId3 = TestsData.EVENT_ID + 2
        database.eventDao().addCompleteEvent(CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId1, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId1, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId1))
        ))
        database.eventDao().addCompleteEvent(CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId2, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId2, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId2))
        ))
        database.eventDao().addCompleteEvent(CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId3, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId3, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId3))
        ))

        assertEquals(
            listOf(eventId1, eventId2, eventId3),
            database.eventDao().getEventsIds(TestsData.SCENARIO_ID)
        )
    }

    @Test
    fun getActions() = runTest {
        val actions = listOf(
            TestsData.getNewPauseEntity(id = 1, eventId = TestsData.EVENT_ID, priority = 0),
            TestsData.getNewSwipeEntity(id = 2, eventId = TestsData.EVENT_ID, priority = 1),
            TestsData.getNewClickEntity(id = 3, eventId = TestsData.EVENT_ID, priority = 2)
        )
        database.eventDao().addCompleteEvent(CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = actions.cloneActions(),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID))
        ))

        assertEquals(
            actions.map { it.action },
            database.eventDao().getActions(TestsData.EVENT_ID)
        )
    }

    @Test
    fun updateEventList() = runTest {
        val eventId1 = TestsData.EVENT_ID
        val eventId2 = TestsData.EVENT_ID + 1
        val eventId3 = TestsData.EVENT_ID + 2
        val event1 = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId1, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId1, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId1))
        )
        val event2 = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId2, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId2, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId2))
        )
        val event3 = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = eventId3, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = eventId3, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = eventId3))
        )
        database.eventDao().addCompleteEvent(event1)
        database.eventDao().addCompleteEvent(event2)
        database.eventDao().addCompleteEvent(event3)

        val events = database.eventDao().getEvents(TestsData.SCENARIO_ID).first()
        val expectedEvents = events.reversed()
        database.eventDao().updateEventList(expectedEvents)
        expectedEvents.forEachIndexed { index, event -> event.priority = index }

        assertEquals(
            expectedEvents,
            database.eventDao().getEvents(TestsData.SCENARIO_ID).first()
        )
    }
}

private const val DEFAULT_PRIMARY_KEY = 0L