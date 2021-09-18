/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.database.room

import android.os.Build
import androidx.room.Room

import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.database.old.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.old.room.ClickWithConditions
import com.buzbuz.smartautoclicker.database.old.room.ScenarioWithClicks

import com.buzbuz.smartautoclicker.database.utils.assertSameClickList
import com.buzbuz.smartautoclicker.database.utils.assertSameScenarioList
import com.buzbuz.smartautoclicker.database.utils.getOrAwaitValue
import com.buzbuz.smartautoclicker.database.utils.DatabaseExecutorRule
import com.buzbuz.smartautoclicker.database.utils.TestsData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
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

/** Tests for the [ClickDao].*/
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ClickDaoTests {

    /**
     * Rule to swaps the background executor used by the Architecture Components with a different one which executes
     * each IO task synchronously
     */
    @get:Rule val instantExecutorRule = DatabaseExecutorRule()

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
    fun addScenario() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        assertEquals(
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList()),
            database.clickDao().getClickScenarios().getOrAwaitValue()[0])
    }

    @Test
    fun addClick() = runBlocking {
        val expected = ClickWithConditions(TestsData.CLICK_ENTITY, emptyList())
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(expected)

        assertEquals(expected, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
    }

    @Test
    fun addCondition() = runBlocking {
        val expected = ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY))
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(expected)

        assertEquals(expected, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
    }

    @Test
    fun renameScenario() = runBlocking {
        val newName = TestsData.SCENARIO_NAME_2
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        database.clickDao().renameScenario(TestsData.SCENARIO_ENTITY.id, newName)

        assertEquals(
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY.copy(name = newName), emptyList()),
            database.clickDao().getClickScenarios().getOrAwaitValue()[0])
    }

    @Test
    fun updateClick() = runBlocking {
        val original = ClickWithConditions(TestsData.CLICK_ENTITY, emptyList())
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(original)

        val newValue = TestsData.CLICK_ENTITY_2.copy(clickId = original.click.clickId, scenarioId = original.click.scenarioId)
        database.clickDao().updateClicks(listOf(newValue))

        assertEquals(newValue, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0].click)
    }

    @Test
    fun updateClickWithConditions() = runBlocking {
        val original = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, 1, listOf(TestsData.CONDITION_ENTITY))
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(original)

        val newValue = TestsData.newClickWithConditionEntity2(original.click.scenarioId, original.click.clickId,
            listOf(TestsData.CONDITION_ENTITY_2))
        database.clickDao().updateClickWithConditions(newValue)

        assertEquals(newValue, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
    }

    @Test
    fun deleteScenario() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().deleteClickScenario(TestsData.SCENARIO_ENTITY)

        assertTrue(database.clickDao().getClickScenarios().getOrAwaitValue().isEmpty())
    }

    @Test
    fun deleteClick() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()))

        database.clickDao().deleteClick(TestsData.CLICK_ENTITY)

        assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
    }

    @Test
    fun deleteClickWithCondition() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val conditions = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        val clickWithConditions = TestsData.newClickWithConditionEntity(
            TestsData.SCENARIO_ENTITY.id,
            TestsData.CLICK_ID,
            conditions
        )
        database.clickDao().addClickWithConditions(clickWithConditions)
        database.clickDao().deleteClick(clickWithConditions.click)

        assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
        assertEquals(conditions, database.clickDao().getClicklessConditions().getOrAwaitValue())
    }

    @Test
    fun deleteScenarioWithClicks() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()))
        database.clickDao().deleteClickScenario(TestsData.SCENARIO_ENTITY)

        assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
    }

    @Test
    fun deleteScenarioWithClicksAndConditions() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val conditions = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        database.clickDao().addClickWithConditions(TestsData.newClickWithConditionEntity(
            TestsData.SCENARIO_ENTITY.id,
            TestsData.CLICK_ID,
            conditions
        ))
        database.clickDao().deleteClickScenario(TestsData.SCENARIO_ENTITY)

        assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
        assertEquals(conditions, database.clickDao().getClicklessConditions().getOrAwaitValue())
    }

    @Test
    fun deleteClicklessConditions() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao()
            .addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY)))
        database.clickDao().deleteClick(TestsData.CLICK_ENTITY)
        database.clickDao().deleteClicklessConditions()

        assertEquals(0, database.clickDao().getClicklessConditions().getOrAwaitValue().size)
    }

    @Test
    fun getClickCount() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickWithConditions(TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id))
        database.clickDao().addClickWithConditions(TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id))

        assertEquals(2, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
    }

    @Test
    fun getClickScenarios() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY_2)

        assertSameScenarioList(
            listOf(
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList()),
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, emptyList())
            ),
            database.clickDao().getClickScenarios().getOrAwaitValue()
        )
    }

    @Test
    fun getClickScenariosWithClicks() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY_2)

        val expectedClick1 = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ID, TestsData.CLICK_ID)
        database.clickDao().addClickWithConditions(expectedClick1)
        val expectedClick2 = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ID_2, TestsData.CLICK_ID_2)
        database.clickDao().addClickWithConditions(expectedClick2)

        assertSameScenarioList(
            listOf(
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY, listOf(expectedClick1.click)),
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, listOf(expectedClick2.click))
            ),
            database.clickDao().getClickScenarios().getOrAwaitValue()
        )
    }

    @Test
    fun getClicks() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val expectedClick1 = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID).apply {
            click.priority = 1
        }
        database.clickDao().addClickWithConditions(expectedClick1)
        val expectedClick2 = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_2).apply {
            click.priority = 0
        }
        database.clickDao().addClickWithConditions(expectedClick2)

        assertEquals(listOf(expectedClick2, expectedClick1),
            database.clickDao().getClicksWithConditions(TestsData.SCENARIO_ENTITY.id).getOrAwaitValue())
    }

    @Test
    fun getClicksList() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val expectedClick1 = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID)
            .apply { click.priority = 1 }
        database.clickDao().addClickWithConditions(expectedClick1)
        val expectedClick2 = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_2)
            .apply { click.priority = 0 }
        database.clickDao().addClickWithConditions(expectedClick2)

        assertEquals(listOf(expectedClick2, expectedClick1),
            database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id))
    }

    @Test
    fun getClicksWithConditions() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val expectedConditions1 = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        val expectedClick1 = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID,
            expectedConditions1).apply { click.priority = 1 }
        database.clickDao().addClickWithConditions(expectedClick1)
        val expectedConditions2 = listOf(TestsData.CONDITION_ENTITY)
        val expectedClick2 = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_2,
            expectedConditions2).apply { click.priority = 0 }
        database.clickDao().addClickWithConditions(expectedClick2)

        assertSameClickList(listOf(expectedClick2, expectedClick1),
            database.clickDao().getClicksWithConditions(TestsData.SCENARIO_ENTITY.id).getOrAwaitValue())
    }

    @Test
    fun getClicksWithConditionsList() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val expectedConditions1 = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        val expectedClick1 = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID,
            expectedConditions1).apply { click.priority = 1 }
        database.clickDao().addClickWithConditions(expectedClick1)
        val expectedConditions2 = listOf(TestsData.CONDITION_ENTITY)
        val expectedClick2 = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_2,
            expectedConditions2).apply { click.priority = 0 }
        database.clickDao().addClickWithConditions(expectedClick2)

        assertSameClickList(listOf(expectedClick2, expectedClick1),
            database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id))
    }

    @Test
    fun getClicksLessPrioritized() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val thresholdPriority = 1
        val thresholdClick = TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID)
            .apply { click.priority = thresholdPriority }
        database.clickDao().addClickWithConditions(thresholdClick)
        val highPriorityClick = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_2)
            .apply { click.priority = 0 }
        database.clickDao().addClickWithConditions(highPriorityClick)
        val expectedClick = TestsData.newClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id, TestsData.CLICK_ID_3)
            .apply { click.priority = 2 }
        database.clickDao().addClickWithConditions(expectedClick)

        assertEquals(listOf(expectedClick.click),
            database.clickDao().getClicksLessPrioritized(TestsData.SCENARIO_ENTITY.id, thresholdPriority))
    }

    @Test
    fun getClicklessConditions() = runBlocking {
        database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

        val expected = TestsData.CONDITION_ENTITY
        val deletedClick = TestsData.newClickWithConditionEntity(
            TestsData.SCENARIO_ENTITY.id,
            TestsData.CLICK_ID,
            listOf(expected, TestsData.CONDITION_ENTITY_2)
        )
        database.clickDao().addClickWithConditions(deletedClick)
        database.clickDao().addClickWithConditions(TestsData.newClickWithConditionEntity(
            TestsData.SCENARIO_ENTITY.id,
            TestsData.CLICK_ID_2,
            listOf(TestsData.CONDITION_ENTITY_2))
        )
        database.clickDao().deleteClick(deletedClick.click)

        assertEquals(listOf(expected), database.clickDao().getClicklessConditions().getOrAwaitValue())
    }
}