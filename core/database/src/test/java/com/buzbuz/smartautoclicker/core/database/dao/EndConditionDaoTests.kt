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
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionWithEvent
import com.buzbuz.smartautoclicker.core.database.utils.TestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [EndConditionDao]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EndConditionDaoTests {

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
    fun addEndCondition() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        val eventEntity = TestsData.getNewEventEntity(scenarioId = scenarioEntity.id, priority = 1)
        val endConditionEntity = TestsData.getNewEndConditionEntity(
            scenarioId = scenarioEntity.id,
            eventId = eventEntity.id,
        )

        database.apply {
            scenarioDao().add(scenarioEntity)
            eventDao().addEvent(eventEntity)
            endConditionDao().addEndConditions(listOf(endConditionEntity))
        }

        Assert.assertEquals(
            EndConditionWithEvent(endConditionEntity, eventEntity),
            database.scenarioDao().getScenarioWithEndConditions(TestsData.END_SCENARIO_ID).first()!!.endConditions[0]
        )
    }

    @Test
    fun updateEndCondition_executions() = runTest {
        // Add end condition
        val scenarioEntity = TestsData.getNewScenarioEntity()
        val eventEntity = TestsData.getNewEventEntity(scenarioId = scenarioEntity.id, priority = 1)
        var endConditionEntity = TestsData.getNewEndConditionEntity(
            id = 18L,
            scenarioId = scenarioEntity.id,
            eventId = eventEntity.id,
            executions = 1
        )
        database.apply {
            scenarioDao().add(scenarioEntity)
            eventDao().addEvent(eventEntity)
            endConditionDao().addEndConditions(listOf(endConditionEntity))
        }

        // Update end condition executions
        endConditionEntity = endConditionEntity.copy(executions = 3)
        database.endConditionDao().updateEndConditions(listOf(endConditionEntity))

        // Verify
        Assert.assertEquals(
            EndConditionWithEvent(endConditionEntity, eventEntity),
            database.scenarioDao().getScenarioWithEndConditions(TestsData.END_SCENARIO_ID).first()!!.endConditions[0]
        )
    }

    @Test
    fun updateEndCondition_event() = runTest {
        // Add end condition
        val scenarioEntity = TestsData.getNewScenarioEntity()
        val eventEntity1 = TestsData.getNewEventEntity(id = 1, scenarioId = scenarioEntity.id, priority = 1)
        val eventEntity2 = TestsData.getNewEventEntity(id = 2, scenarioId = scenarioEntity.id, priority = 1)
        var endConditionEntity = TestsData.getNewEndConditionEntity(
            id = 18L,
            scenarioId = scenarioEntity.id,
            eventId = eventEntity1.id,
        )
        database.apply {
            scenarioDao().add(scenarioEntity)
            eventDao().addEvent(eventEntity1)
            eventDao().addEvent(eventEntity2)
            endConditionDao().addEndConditions(listOf(endConditionEntity))
        }

        // Update end condition event
        endConditionEntity = endConditionEntity.copy(eventId = eventEntity2.id)
        database.endConditionDao().updateEndConditions(listOf(endConditionEntity))

        // Verify
        Assert.assertEquals(
            EndConditionWithEvent(endConditionEntity, eventEntity2),
            database.scenarioDao().getScenarioWithEndConditions(TestsData.END_SCENARIO_ID).first()!!.endConditions[0]
        )
    }

    @Test
    fun deleteEndCondition() = runTest {
        // Add end condition
        val scenarioEntity = TestsData.getNewScenarioEntity()
        var eventEntity = TestsData.getNewEventEntity(scenarioId = scenarioEntity.id, priority = 1)
        val endConditionEntity  = TestsData.getNewEndConditionEntity(
            id = 18L,
            scenarioId = scenarioEntity.id,
            eventId = eventEntity.id,
        )
        val eventId: Long
        database.apply {
            scenarioDao().add(scenarioEntity)
            eventId = eventDao().addEvent(eventEntity)
            endConditionDao().addEndConditions(listOf(endConditionEntity))
        }

        // Update ids from insert methods
        eventEntity = eventEntity.copy(id = eventId)
        // Delete end condition
        database.endConditionDao().deleteEndConditions(listOf(endConditionEntity))

        // Verify scenario is OK and condition deleted
        Assert.assertTrue(
            "Invalid end condition size",
            database.scenarioDao().getScenarioWithEndConditions(endConditionEntity.scenarioId)
                .first()!!.endConditions.isEmpty()
        )
        // Verify associated event is OK
        Assert.assertEquals(
            "Invalid end condition size",
            eventEntity,
            database.eventDao().getEventsFlow(scenarioEntity.id).first().first(),
        )
    }
}