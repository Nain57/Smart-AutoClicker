/*
 * Copyright (C) 2021 Nain57
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
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.utils.DatabaseExecutorRule
import com.buzbuz.smartautoclicker.database.utils.TestsData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ConditionDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ConditionDaoTests {

    /**
     * Rule to swaps the background executor used by the Architecture Components with a different one which executes
     * each IO task synchronously
     */
    @get:Rule
    val instantExecutorRule = DatabaseExecutorRule()

    /** Coroutine dispatcher for the tests. */
    private val testDispatcher = TestCoroutineDispatcher()
    /** Coroutine scope for the tests. */
    private val testScope = TestCoroutineScope(testDispatcher)

    /** In memory database used to test the dao. */
    private lateinit var database: ClickDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, ClickDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.clearAllTables()
        database.close()
        testScope.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    @Test
    fun getConditions_none() = runBlocking {
        assertEquals(emptyList<ConditionEntity>(), database.conditionDao().getConditions(TestsData.EVENT_ID))
    }

    @Test
    fun getConditions() = runBlocking {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = mutableListOf(TestsData.getNewClickEntity(eventId = TestsData.EVENT_ID, priority = 0)),
            conditions = mutableListOf(
                TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID),
                TestsData.getNewConditionEntity(id = 2, eventId = TestsData.EVENT_ID),
                TestsData.getNewConditionEntity(id = 3, eventId = TestsData.EVENT_ID),
            ),
        )
        database.scenarioDao().add(TestsData.getNewScenarioEntity())
        database.eventDao().addEvent(completeEvent)

        assertEquals(
            completeEvent.conditions,
            database.conditionDao().getConditions(TestsData.EVENT_ID)
        )
    }

    @Test
    fun getConditionsPath_none() = runBlocking {
        assertEquals(emptyList<String>(), database.conditionDao().getConditionsPath(TestsData.EVENT_ID))
    }

    @Test
    fun getConditionsPath() = runBlocking {
        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = mutableListOf(TestsData.getNewClickEntity(eventId = TestsData.EVENT_ID, priority = 0)),
            conditions = mutableListOf(
                TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID, path = "toto"),
                TestsData.getNewConditionEntity(id = 2, eventId = TestsData.EVENT_ID, path = "tata"),
                TestsData.getNewConditionEntity(id = 3, eventId = TestsData.EVENT_ID, path = "tutu"),
            ),
        )
        database.scenarioDao().add(TestsData.getNewScenarioEntity())
        database.eventDao().addEvent(completeEvent)

        val expectedPaths = mutableListOf<String>()
        completeEvent.conditions.forEach {
            expectedPaths.add(it.path)
        }
        val resultPaths = database.conditionDao().getConditionsPath(TestsData.EVENT_ID)

        assertEquals(
            "Path lists should have the same size.",
            expectedPaths.size,
            resultPaths.size
        )
        assertTrue("Path lists should have the same content", resultPaths.containsAll(expectedPaths))
    }

    @Test
    fun getValidPathCount_none() = runBlocking {
        assertEquals(0, database.conditionDao().getValidPathCount(TestsData.CONDITION_PATH))
    }

    @Test
    fun getValidPathCount() = runBlocking {
        val completeEvent1 = CompleteEventEntity(
            event = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = mutableListOf(TestsData.getNewClickEntity(eventId = TestsData.EVENT_ID, priority = 0)),
            conditions = mutableListOf(TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID, path = "toto")),
        )
        val event2Id = 123456L
        val completeEvent2 = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = event2Id, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = mutableListOf(TestsData.getNewClickEntity(eventId = event2Id, priority = 0)),
            conditions = mutableListOf(
                TestsData.getNewConditionEntity(id = 2, eventId = event2Id, path = "toto"),
                TestsData.getNewConditionEntity(id = 3, eventId = event2Id, path = "tutu")
            ),
        )
        database.scenarioDao().add(TestsData.getNewScenarioEntity())
        database.eventDao().addEvent(completeEvent1)
        database.eventDao().addEvent(completeEvent2)

        assertEquals(2, database.conditionDao().getValidPathCount("toto"))
    }
}