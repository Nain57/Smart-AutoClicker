/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.database.migrations

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_USAGE_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the usage-statistics repair performed by [Migration23to24]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration23to24Tests {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private lateinit var dbPath: String

    @Before
    fun setUp() {
        dbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath("migration-test").path
    }

    @Test
    fun migrate_duplicateStatistics_mergesCountAndLatestTimestamp_thenEnforcesUniqueness() {
        helper.createDatabase(dbPath, 23).use { db ->
            db.insertTestScenario(1L)
            db.insertTestStats(id = 10L, scenarioId = 1L, timestampMs = 100L, startCount = 2L)
            db.insertTestStats(id = 11L, scenarioId = 1L, timestampMs = 300L, startCount = 4L)
        }

        helper.runMigrationsAndValidate(dbPath, 24, true, Migration23to24).use { db ->
            db.query("SELECT id, last_start_timestamp_ms, start_count FROM $SCENARIO_USAGE_TABLE WHERE scenario_id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(10L, cursor.getLong(0))
                assertEquals(300L, cursor.getLong(1))
                assertEquals(6L, cursor.getLong(2))
            }

            val insertResult = db.insert(
                SCENARIO_USAGE_TABLE,
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("scenario_id", 1L)
                    put("last_start_timestamp_ms", 400L)
                    put("start_count", 1L)
                },
            )
            assertEquals(-1L, insertResult)
        }
    }

    @Test
    fun migrate_v15Database_throughTheRegisteredUpgradeChain() {
        val lowerVersionDbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath("migration-v15-to-v24-test").path

        helper.createDatabase(lowerVersionDbPath, 15).use { db ->
            db.insertV15Scenario(2L)
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClickDatabase::class.java,
            lowerVersionDbPath,
        ).addMigrations(
            Migration19to20,
            Migration21to22,
            Migration23to24,
        ).build()
        try {
            database.openHelper.writableDatabase.query(
                "SELECT name FROM $SCENARIO_TABLE WHERE id = 2",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Scenario 2", cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    private fun SupportSQLiteDatabase.insertV15Scenario(id: Long) {
        insert(SCENARIO_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("name", "Scenario $id")
            put("detection_quality", 1200)
            put("randomize", 0)
        })
    }

    private fun SupportSQLiteDatabase.insertTestScenario(id: Long) {
        insert(SCENARIO_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("name", "Scenario $id")
            put("detection_quality", 1200)
            put("compute_rate", 0.0)
            put("randomize", 0)
            put("keep_screen_on", 0)
        })
    }

    private fun SupportSQLiteDatabase.insertTestStats(
        id: Long,
        scenarioId: Long,
        timestampMs: Long,
        startCount: Long,
    ) {
        insert(SCENARIO_USAGE_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("scenario_id", scenarioId)
            put("last_start_timestamp_ms", timestampMs)
            put("start_count", startCount)
        })
    }
}
