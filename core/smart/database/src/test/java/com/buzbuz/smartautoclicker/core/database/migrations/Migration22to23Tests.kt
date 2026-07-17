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
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the automatic migration that adds the number format type. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration22to23Tests {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private lateinit var dbPath: String

    @Before
    fun setUp() {
        dbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath("migration-22-to-23-test").path
    }

    @Test
    fun migrate_numberCondition_preservesValues_andKeepsFormatUnsetForLegacyData() {
        helper.createDatabase(dbPath, 22).use { db ->
            db.insertScenario()
            db.insertScreenEvent()
            db.insertNumberCondition()
        }

        helper.runMigrationsAndValidate(dbPath, 23, true).use { db ->
            db.query("SELECT number_format_type, number_counter_value FROM $CONDITION_TABLE WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(null, cursor.getString(0))
                assertEquals(42.5, cursor.getDouble(1), 0.0)
            }
        }
    }

    private fun SupportSQLiteDatabase.insertScenario() {
        insert(SCENARIO_TABLE, 0, ContentValues().apply {
            put("id", 1L)
            put("name", "Scenario")
            put("detection_quality", 1200)
        })
    }

    private fun SupportSQLiteDatabase.insertScreenEvent() {
        insert(EVENT_TABLE, 0, ContentValues().apply {
            put("id", 1L)
            put("scenario_id", 1L)
            put("name", "Event")
            put("operator", 0)
            put("priority", 0)
            put("type", "IMAGE_EVENT")
        })
    }

    private fun SupportSQLiteDatabase.insertNumberCondition() {
        insert(CONDITION_TABLE, 0, ContentValues().apply {
            put("id", 1L)
            put("eventId", 1L)
            put("name", "Number")
            put("type", "ON_NUMBER_DETECTED")
            put("number_counter_comparison_operation", "GREATER")
            put("number_counter_operation_value_type", "NUMBER")
            put("number_counter_value", 42.5)
        })
    }
}
