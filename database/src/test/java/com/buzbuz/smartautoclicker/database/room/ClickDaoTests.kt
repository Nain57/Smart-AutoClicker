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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.TestsData

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ClickDaoTests {

    private lateinit var database: ClickDatabase
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.Main)
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            ClickDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        database.close()
        scope.cancel()
    }

    @Test
    fun addScenario() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

            assertEquals(ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList()),
                database.clickDao().getClickScenarios().value!![0])
        }
    }

    @Test
    fun renameScenario() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)

            assertEquals(ScenarioWithClicks(TestsData.SCENARIO_ENTITY.copy(name = TestsData.SCENARIO_NAME_2), emptyList()),
                database.clickDao().getClickScenarios().value!![0])
        }
    }

    @Test
    fun deleteScenario() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().deleteClickScenario(TestsData.SCENARIO_ENTITY)

            assertTrue(database.clickDao().getClickScenarios().value!!.isEmpty())
        }
    }

    @Test
    fun addClick() {
        scope.launch {
            val expected = ClickWithConditions(TestsData.CLICK_ENTITY, emptyList())
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(expected)

            assertEquals(expected, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
        }
    }

    @Test
    fun updateClick() {
        scope.launch {
            val original = ClickWithConditions(TestsData.CLICK_ENTITY, emptyList())
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(original)

            val newValue = TestsData.CLICK_ENTITY_2.copy(clickId = original.click.clickId)
            database.clickDao().updateClicks(listOf(newValue))

            assertEquals(newValue, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
        }
    }

    @Test
    fun deleteClick() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()))

            database.clickDao().deleteClick(TestsData.CLICK_ENTITY)

            assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
        }
    }

    @Test
    fun deleteScenarioWithClicks() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()))
            database.clickDao().deleteClickScenario(TestsData.SCENARIO_ENTITY)

            assertEquals(0, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
        }
    }

    @Test
    fun getClickCount() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(TestsData.newIdlessClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id))
            database.clickDao().addClickWithConditions(TestsData.newIdlessClickWithConditionEntity2(TestsData.SCENARIO_ENTITY.id))

            assertEquals(2, database.clickDao().getClickCount(TestsData.SCENARIO_ENTITY.id))
        }
    }

    @Test
    fun addCondition() {
        scope.launch {
            database.clickDao()
                .addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY)))
            database.clickDao().deleteClick(TestsData.CLICK_ENTITY)

            assertEquals(TestsData.CONDITION_ENTITY, database.clickDao().getClicklessConditions().value!![0])
        }
    }

    @Test
    fun deleteClicklessConditions() {
        scope.launch {
            database.clickDao()
                .addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY)))
            database.clickDao().deleteClick(TestsData.CLICK_ENTITY)
            database.clickDao().deleteClicklessConditions()

            assertEquals(0, database.clickDao().getClicklessConditions().value!!.size)
        }
    }

    @Test
    fun updateClickWithConditions() {
        scope.launch {
            val original = TestsData.newIdlessClickWithConditionEntity(TestsData.SCENARIO_ENTITY.id, 1, listOf(TestsData.CONDITION_ENTITY))
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickWithConditions(original)

            val newValue = TestsData.newIdlessClickWithConditionEntity2(original.click.scenarioId, original.click.clickId,
                listOf(TestsData.CONDITION_ENTITY_2))
            database.clickDao().updateClickWithConditions(newValue)

            assertEquals(newValue, database.clickDao().getClicksWithConditionsList(TestsData.SCENARIO_ENTITY.id)[0])
        }
    }

    @Test
    fun getClickScenarios() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY_2)

            val expected = listOf(
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList()),
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, emptyList())
            )

            assertEquals(expected, database.clickDao().getClickScenarios().value)
        }
    }

    @Test
    fun getClickScenariosWithClicks() {
        scope.launch {
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY)
            database.clickDao().addClickScenario(TestsData.SCENARIO_ENTITY_2)
            database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()))
            database.clickDao().addClickWithConditions(ClickWithConditions(TestsData.CLICK_ENTITY_2, emptyList()))

            val expected = listOf(
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY, listOf(TestsData.CLICK_ENTITY, TestsData.CLICK_ENTITY_2)),
                ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, listOf(TestsData.CLICK_ENTITY, TestsData.CLICK_ENTITY_2))
            )

            assertEquals(expected, database.clickDao().getClickScenarios().value)
        }
    }
}