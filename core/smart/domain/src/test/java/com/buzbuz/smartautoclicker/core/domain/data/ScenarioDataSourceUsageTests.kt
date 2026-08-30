/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.domain.data

import android.os.Build

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Database-backed tests for durable scenario usage accounting. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioDataSourceUsageTests {

    private lateinit var database: ClickDatabase
    private lateinit var dataSource: ScenarioDataSource
    private var scenarioId: Long = DATABASE_ID_INSERTION

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClickDatabase::class.java,
        ).allowMainThreadQueries().build()
        dataSource = ScenarioDataSource(database)
        runTest {
            scenarioId = database.scenarioDao().add(
                ScenarioEntity(
                    id = DATABASE_ID_INSERTION,
                    name = "Usage test",
                    detectionQuality = 1200,
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun markAsUsed_twice_persistsOneAccumulatedStatistic() = runTest {
        val startedAt = System.currentTimeMillis()

        dataSource.markAsUsed(scenarioId)
        dataSource.markAsUsed(scenarioId)

        val statistics = database.scenarioDao().getScenarioStats(scenarioId)
        assertEquals(1, statistics.size)
        assertEquals(2L, statistics.single().startCount)
        assertTrue(statistics.single().lastStartTimestampMs >= startedAt)
    }

    @Test
    fun markAsUsed_concurrently_preservesEveryIncrement() = runTest {
        coroutineScope {
            List(16) {
                async(Dispatchers.Default) { dataSource.markAsUsed(scenarioId) }
            }.awaitAll()
        }

        val statistics = database.scenarioDao().getScenarioStats(scenarioId)
        assertEquals(1, statistics.size)
        assertEquals(16L, statistics.single().startCount)
    }
}
