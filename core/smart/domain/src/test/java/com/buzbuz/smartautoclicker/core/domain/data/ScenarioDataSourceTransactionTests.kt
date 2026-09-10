/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.domain.data

import android.content.Context
import android.os.Build

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.domain.model.scenario.ScenarioTestsData

import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ensures external image cleanup only runs after a scenario transaction commits. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioDataSourceTransactionTests {

    private lateinit var database: ClickDatabase
    private lateinit var dataSource: ScenarioDataSource

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ClickDatabase::class.java,
        ).allowMainThreadQueries().build()
        dataSource = ScenarioDataSource(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addCompleteScenario_cleanupCallbackRunsAfterScenarioIsCommitted() = runTest {
        var callbackObservedCommittedScenario = false

        val insertedId = dataSource.addCompleteScenario(
            scenario = ScenarioTestsData.getNewScenario(id = 0),
            events = emptyList(),
            counters = emptyList(),
            onImageConditionsRemoved = {
                callbackObservedCommittedScenario = database.scenarioDao().getScenario(1L) != null
            },
        )

        assertEquals(1L, insertedId)
        assertNotNull(database.scenarioDao().getScenario(1L))
        assertEquals(true, callbackObservedCommittedScenario)
    }

    @Test
    fun updateScenario_cleanupCallbackRunsAfterScenarioIsCommitted() = runTest {
        val scenario = ScenarioTestsData.getNewScenario(id = 0)
        val scenarioId = dataSource.addCompleteScenario(
            scenario = scenario,
            events = emptyList(),
            counters = emptyList(),
            onImageConditionsRemoved = {},
        )!!
        var callbackObservedUpdatedScenario = false

        val result = dataSource.updateScenario(
            scenario = scenario.copy(id = Identifier(databaseId = scenarioId), name = "Updated scenario"),
            events = emptyList(),
            counters = emptyList(),
            onImageConditionsRemoved = {
                callbackObservedUpdatedScenario = database.scenarioDao().getScenario(scenarioId)?.scenario?.name == "Updated scenario"
            },
        )

        assertTrue(result)
        assertEquals("Updated scenario", database.scenarioDao().getScenario(scenarioId)?.scenario?.name)
        assertTrue(callbackObservedUpdatedScenario)
    }
}
