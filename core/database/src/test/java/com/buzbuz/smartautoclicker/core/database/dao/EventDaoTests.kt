/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.dao

import android.os.Build

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.utils.TestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        database = Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ClickDatabase::class.java,
            )
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
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)

        database.eventDao().addEvent(event)

        assertEquals(event, database.eventDao().getEventsFlow(TestsData.SCENARIO_ID).first().first())
    }

    @Test
    fun updateEvent() = runTest {
        // First add the old event
        val event = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
            name = "toto"
        )
        database.eventDao().addEvent(event)

        // Then, update the event
        val expectedName = "tutu"
        val updatedEvent = event.copy(name = expectedName)
        database.eventDao().updateEvent(updatedEvent)

        // Check update
        assertEquals(updatedEvent, database.eventDao().getEventsFlow(TestsData.SCENARIO_ID).first().first())
    }

    @Test
    fun deleteEvent() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        database.eventDao().addEvent(event)

        database.eventDao().deleteEvents(listOf(event))

        assertTrue(database.eventDao().getEventsFlow(TestsData.SCENARIO_ID).first().isEmpty())
    }

    @Test
    fun getEventIds() = runTest {
        val eventId1 = TestsData.EVENT_ID
        val eventId2 = TestsData.EVENT_ID + 1
        val eventId3 = TestsData.EVENT_ID + 2

        database.eventDao().apply {
            addEvent(TestsData.getNewEventEntity(id = eventId1, scenarioId = TestsData.SCENARIO_ID, priority = 0))
            addEvent(TestsData.getNewEventEntity(id = eventId2, scenarioId = TestsData.SCENARIO_ID, priority = 0))
            addEvent(TestsData.getNewEventEntity(id = eventId3, scenarioId = TestsData.SCENARIO_ID, priority = 0))
        }

        assertEquals(
            listOf(eventId1, eventId2, eventId3),
            database.eventDao().getEventsIds(TestsData.SCENARIO_ID),
        )
    }
}