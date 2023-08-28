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
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.utils.TestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ConditionDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ConditionDaoTests {

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
    }

    @After
    fun tearDown() {
        database.clearAllTables()
        database.close()
    }

    @Test
    fun getConditions_none() = runTest {
        assertEquals(emptyList<ConditionEntity>(), database.conditionDao().getConditions(TestsData.EVENT_ID))
    }

    @Test
    fun getConditions() = runTest {
        val scenario = TestsData.getNewScenarioEntity()
        val event = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )
        val expectedConditions = mutableListOf(
            TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID),
            TestsData.getNewConditionEntity(id = 2, eventId = TestsData.EVENT_ID),
            TestsData.getNewConditionEntity(id = 3, eventId = TestsData.EVENT_ID),
        )

        database.apply {
            scenarioDao().add(scenario)
            eventDao().addEvent(event)
            conditionDao().addConditions(expectedConditions)
        }

        assertEquals(
            expectedConditions,
            database.conditionDao().getConditions(TestsData.EVENT_ID)
        )
    }

    @Test
    fun getConditionsPath_none() = runTest {
        assertEquals(emptyList<String>(), database.conditionDao().getConditionsPath(TestsData.EVENT_ID))
    }

    @Test
    fun getConditionsPath() = runTest {
        val scenario = TestsData.getNewScenarioEntity()
        val event = TestsData.getNewEventEntity(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )
        val conditions = mutableListOf(
            TestsData.getNewConditionEntity(id = 1, eventId = TestsData.EVENT_ID, path = "toto"),
            TestsData.getNewConditionEntity(id = 2, eventId = TestsData.EVENT_ID, path = "tata"),
            TestsData.getNewConditionEntity(id = 3, eventId = TestsData.EVENT_ID, path = "tutu"),
        )
        val expectedPaths = mutableListOf<String>()
        conditions.forEach { expectedPaths.add(it.path) }

        database.apply {
            scenarioDao().add(scenario)
            eventDao().addEvent(event)
            conditionDao().addConditions(conditions)
        }

        val resultPaths = database.conditionDao().getConditionsPath(TestsData.EVENT_ID)
        assertEquals(
            "Path lists should have the same size.",
            expectedPaths.size,
            resultPaths.size,
        )
        assertTrue("Path lists should have the same content", resultPaths.containsAll(expectedPaths))
    }

    @Test
    fun getValidPathCount_none() = runTest {
        assertEquals(0, database.conditionDao().getValidPathCount(TestsData.CONDITION_PATH))
    }

    @Test
    fun getValidPathCount() = runTest {
        val scenario = TestsData.getNewScenarioEntity()
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
        val conditions = mutableListOf(
            TestsData.getNewConditionEntity(id = 1, eventId = event1.id, path = "toto"),
            TestsData.getNewConditionEntity(id = 2, eventId = event2.id, path = "toto"),
            TestsData.getNewConditionEntity(id = 3, eventId = event2.id, path = "tutu"),
        )

        database.apply {
            scenarioDao().add(scenario)
            eventDao().addEvent(event1)
            eventDao().addEvent(event2)
            conditionDao().addConditions(conditions)
        }

        assertEquals(2, database.conditionDao().getValidPathCount("toto"))
    }
}