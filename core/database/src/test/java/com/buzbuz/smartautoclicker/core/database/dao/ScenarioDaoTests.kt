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
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.utils.TestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [ScenarioDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioDaoTests {

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
    fun addScenario() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        assertEquals(
            CompleteScenario(scenarioEntity, emptyList(), emptyList()),
            database.scenarioDao().getCompleteScenario(scenarioEntity.id)
        )
    }

    @Test
    fun updateScenario() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        val updatedScenario = scenarioEntity.copy(name = "Toto")
        database.scenarioDao().update(updatedScenario)

        assertEquals(
            CompleteScenario(updatedScenario, emptyList(), emptyList()),
            database.scenarioDao().getCompleteScenario(scenarioEntity.id)
        )
    }

    @Test
    fun deleteScenario() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        database.scenarioDao().add(scenarioEntity)

        database.scenarioDao().delete(scenarioEntity.id)

        assertNull(database.scenarioDao().getCompleteScenario(scenarioEntity.id))
    }

    @Test
    fun getScenariosWithEvents() = runTest {
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