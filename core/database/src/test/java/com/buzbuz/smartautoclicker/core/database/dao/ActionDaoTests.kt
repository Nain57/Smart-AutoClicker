/*
 * Copyright (C) 2023 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.core.database.utils.assertSameContent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ActionDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionDaoTests {

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
    fun addClickAction() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        val completeAction = TestsData.getNewClickEntity(eventId = event.id, priority = 0)

        database.apply {
            eventDao().addEvent(event)
            actionDao().addAction(completeAction.action)
        }

        assertEquals(completeAction, database.actionDao().getAllActions().first().first())
    }

    @Test
    fun addSwipeAction() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        val completeAction = TestsData.getNewSwipeEntity(eventId = event.id, priority = 0)

        database.apply {
            eventDao().addEvent(event)
            actionDao().addAction(completeAction.action)
        }

        assertEquals(completeAction, database.actionDao().getAllActions().first().first())
    }

    @Test
    fun addPauseAction() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        val completeAction = TestsData.getNewPauseEntity(eventId = event.id, priority = 0)

        database.apply {
            eventDao().addEvent(event)
            actionDao().addAction(completeAction.action)
        }

        assertEquals(completeAction, database.actionDao().getAllActions().first().first())
    }

    @Test
    fun addIntentAction() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        val completeAction = TestsData.getNewIntentEntity(eventId = event.id, priority = 0)

        database.apply {
            eventDao().addEvent(event)
            actionDao().addAction(completeAction.action)
        }

        assertEquals(completeAction, database.actionDao().getAllActions().first().first())
    }

    @Test
    fun addIntentActionWithExtra() = runTest {
        val event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0)
        val completeAction = TestsData.getNewIntentEntity(
            id = TestsData.INTENT_ID,
            eventId = event.id,
            priority = 0,
            intentExtras = listOf(TestsData.getNewIntentExtraEntity(actionId = TestsData.INTENT_ID, value = "toto"))
        )

        database.apply {
            eventDao().addEvent(event)
            actionDao().addAction(completeAction.action)
            actionDao().addIntentExtra(completeAction.intentExtras.first())
        }

        assertEquals(completeAction, database.actionDao().getAllActions().first().first())
    }

    @Test
    fun syncActions() = runTest {
        val event1 = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )
        val event2 = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID_2,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 1,
        )
        val action1 = TestsData.getNewPauseEntity(id = 1, eventId = event1.id, pauseDuration = 5L, priority = 0)
        val actionToBeUpdated = TestsData.getNewClickEntity(id = 2, eventId = event2.id, priority = 0, name = "titi")
        val actionToBeRemoved = TestsData.getNewSwipeEntity(id = 3, eventId = event2.id,  priority = 1, name = "tutu")
        database.apply {
            eventDao().addEvent(event1)
            eventDao().addEvent(event2)
            actionDao().addAction(action1.action)
            actionDao().addAction(actionToBeUpdated.action)
            actionDao().addAction(actionToBeRemoved.action)
        }

        val added = TestsData.getNewPauseEntity(id = 4, eventId = event1.id, pauseDuration = 50L, priority = 1)
        val updated = actionToBeUpdated.copy(action = actionToBeUpdated.action.copy(name = "tata"))
        database.actionDao().syncActions(listOf(added), listOf(updated), listOf(actionToBeRemoved))

        val expectedActions = listOf(action1, updated, added)
        assertSameContent(expectedActions, database.actionDao().getAllActions().first()) { completeAction ->
            completeAction.action.id
        }
    }

    @Test
    fun syncIntentActions() = runTest {
        val event = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )
        val extra1 = TestsData.getNewIntentExtraEntity(id = 1, actionId = TestsData.INTENT_ID, value = "toto")
        val extraToBeUpdated = TestsData.getNewIntentExtraEntity(id = 2, actionId = TestsData.INTENT_ID, value = "tutu")
        val extraToBeRemoved = TestsData.getNewIntentExtraEntity(id = 3, actionId = TestsData.INTENT_ID, value = "tata")
        val completeAction = TestsData.getNewIntentEntity(
            id = TestsData.INTENT_ID,
            eventId = event.id,
            priority = 0,
            intentExtras = listOf(extra1, extraToBeUpdated, extraToBeRemoved)
        )

        database.apply {
            eventDao().addEvent(event)
            actionDao().syncActions(listOf(completeAction), emptyList(), emptyList())
        }

        val added = TestsData.getNewIntentExtraEntity(id = 4, actionId = TestsData.INTENT_ID, value = "toto")
        val updated = extraToBeUpdated.copy(value = "tyty")
        val expectedExtras = listOf(extra1, added, updated)
        val updatedAction = completeAction.copy(intentExtras = expectedExtras)
        database.actionDao().syncActions(emptyList(), listOf(updatedAction), emptyList())

        assertSameContent(expectedExtras, database.actionDao().getAllActions().first().first().intentExtras) { it.id }
    }
}