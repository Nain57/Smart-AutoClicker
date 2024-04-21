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
package com.buzbuz.smartautoclicker.core.database.migrations

import android.database.Cursor
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV6Event
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV6Scenario
import com.buzbuz.smartautoclicker.core.database.utils.getV7EndCondition
import com.buzbuz.smartautoclicker.core.database.utils.getV7Scenario

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration6to7]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration6to7Tests {

    private companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private fun Cursor.verifyCount(expectedCount: Int) =
        Assert.assertEquals("Invalid list size", expectedCount, count)

    private fun Cursor.verifyEndConditionRow(scenarioId: Long, eventId: Long, executions: Int) {
        Assert.assertEquals(
            "Invalid scenario_id value",
            scenarioId,
            getLong(getColumnIndex("scenario_id"))
        )
        Assert.assertEquals(
            "Invalid event_id value",
            eventId,
            getLong(getColumnIndex("event_id"))
        )
        Assert.assertEquals(
            "Invalid executions value",
            executions,
            getInt(getColumnIndex("executions"))
        )
    }

    @Test
    fun migrateScenario_detectionQuality() {
        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(24L, "TOTO"))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify
        val scenarioCursor = dbV7.query(getV7Scenario())
        scenarioCursor.apply {
            verifyCount(1)
            moveToFirst()
            Assert.assertEquals(
                "Invalid detection_quality value",
                600,
                getInt(getColumnIndex("detection_quality"))
            )
        }

        scenarioCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateScenario_endConditionOperator() {
        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(24L, "TOTO"))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify
        val scenarioCursor = dbV7.query(getV7Scenario())
        scenarioCursor.apply {
            verifyCount(1)
            moveToFirst()
            Assert.assertEquals(
                "Invalid end_condition_operator value",
                2,
                getInt(getColumnIndex("end_condition_operator"))
            )
        }

        scenarioCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateEndCondition_noEvents() {
        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(24L, "TOTO"))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify
        val endConditionCursor = dbV7.query(getV7EndCondition())
        endConditionCursor.verifyCount(0)

        endConditionCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateEndCondition_events_NoStopAfter() {
        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(24L, "TOTO"))
            execSQL(getInsertV6Event(1L,24L, "TUTU", 2, null, 1))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify
        val endConditionCursor = dbV7.query(getV7EndCondition())
        endConditionCursor.verifyCount(0)

        endConditionCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateEndCondition_events_OneStopAfter() {
        // Given
        val scenarioId = 24L
        val eventId = 2L
        val stopAfter = 3

        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV6Event(1L, scenarioId, "TUTU", 2, null, 1))
            execSQL(getInsertV6Event(eventId, scenarioId, "TATA", 2, stopAfter, 2))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify created end condition
        val endConditionCursor = dbV7.query(getV7EndCondition())
        endConditionCursor.apply {
            verifyCount(1)
            moveToFirst()
            verifyEndConditionRow(scenarioId, eventId, stopAfter)
        }

        endConditionCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateEndCondition_events_SeveralStopAfter() {
        // Given
        val scenarioId1 = 24L
        val eventId1 = 2L
        val stopAfter1 = 3
        val eventId2 = 98L
        val stopAfter2 = 8

        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(scenarioId1, "TOTO"))
            execSQL(getInsertV6Event(1L, scenarioId1, "TUTU", 2, null, 1))
            execSQL(getInsertV6Event(eventId1, scenarioId1, "TATA", 2, stopAfter1, 2))
            execSQL(getInsertV6Event(eventId2, scenarioId1, "TETE", 2, stopAfter2, 3))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify created end condition
        val endConditionCursor = dbV7.query(getV7EndCondition())
        endConditionCursor.apply {
            verifyCount(2)

            moveToFirst()
            verifyEndConditionRow(scenarioId1, eventId1, stopAfter1)

            moveToNext()
            verifyEndConditionRow(scenarioId1, eventId2, stopAfter2)
        }

        endConditionCursor.close()
        dbV7.close()
    }

    @Test
    fun migrateEndCondition_events_different_scenario() {
        // Given
        val scenarioId1 = 24L
        val eventId1 = 2L
        val stopAfter1 = 3
        val scenarioId2 = 42L
        val eventId2 = 98L
        val stopAfter2 = 8

        // Insert in V6 and close
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(getInsertV6Scenario(scenarioId1, "TOTO"))
            execSQL(getInsertV6Event(1L, scenarioId1, "TUTU", 2, null, 1))
            execSQL(getInsertV6Event(eventId1, scenarioId1, "TATA", 2, stopAfter1, 2))
            execSQL(getInsertV6Scenario(scenarioId2, "TITI"))
            execSQL(getInsertV6Event(eventId2, scenarioId2, "TETE", 2, stopAfter2, 1))
            close()
        }

        // Migrate
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)

        // Verify created end condition
        val endConditionCursor = dbV7.query(getV7EndCondition())
        endConditionCursor.apply {
            verifyCount(2)

            moveToFirst()
            verifyEndConditionRow(scenarioId1, eventId1, stopAfter1)

            moveToNext()
            verifyEndConditionRow(scenarioId2, eventId2, stopAfter2)
        }

        endConditionCursor.close()
        dbV7.close()
    }
}