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
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.database.utils.DatabaseExecutorRule
import com.buzbuz.smartautoclicker.database.utils.TestsData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ScenarioDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioDaoTests {

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
    fun addScenario() = runBlocking {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        assertEquals(
            ScenarioWithEvents(scenarioEntity, emptyList()),
            database.scenarioDao().getScenarioWithEvents(scenarioEntity.id).first()
        )
    }

    @Test
    fun updateScenario() = runBlocking {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        val updatedScenario = scenarioEntity.copy(name = "Toto")
        database.scenarioDao().update(updatedScenario)

        assertEquals(
            ScenarioWithEvents(updatedScenario, emptyList()),
            database.scenarioDao().getScenarioWithEvents(scenarioEntity.id).first()
        )
    }

    @Test
    fun deleteScenario() = runBlocking {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        database.scenarioDao().delete(scenarioEntity)

        assertEquals(
            null,
            database.scenarioDao().getScenarioWithEvents(scenarioEntity.id).first()
        )
    }

    @Test
    fun getScenariosWithEvents() = runBlocking {
        val scenarioCount = 10
        val scenarioList = mutableListOf<ScenarioEntity>()
        repeat(scenarioCount) { index ->
            val scenario = TestsData.getNewScenarioEntity(id = index.toLong() + 1)
            database.scenarioDao().add(scenario)
            scenarioList.add(scenario)
        }

        database.scenarioDao().getScenariosWithEvents().first().apply {
            assertEquals("Invalid scenario count", scenarioCount, size)
            filter { scenarioList.contains(it.scenario) }
            assertEquals("Invalid scenarios", scenarioCount, size)
        }
        Unit
    }
}