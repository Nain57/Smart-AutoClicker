/*
 * Copyright (C) 2026 Kevin Buzeau
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

import android.content.ContentValues
import android.content.Context
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration21to22]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration21to22Tests {

    private companion object {
        private const val OLD_DB_VERSION = 21
        private const val NEW_DB_VERSION = 22
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private lateinit var dbPath: String

    @Before
    fun setUp() {
        dbPath = ApplicationProvider
            .getApplicationContext<Context>()
            .getDatabasePath("migration-test").path
    }

    @Test
    fun migrate_blankCounter_isRemoved() {
        // Given: a v21 database that contains a blank counter (empty string name) left by the
        // Migration19to20 bug.
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestCounter(scenarioId, counterName = "")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            // Then: the blank counter must no longer exist
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate_whitespaceOnlyCounter_isRemoved() {
        // Given: a counter whose name is whitespace only
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestCounter(scenarioId, counterName = "   ")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            // Then
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate_validCounter_isPreserved() {
        // Given: a v21 database with a normally-named counter
        val scenarioId = 1L
        val counterName = "MyCounter"
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestCounter(scenarioId, counterName = counterName)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            // Then: the valid counter is untouched
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(counterName, cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate_mixedCounters_onlyBlankRemoved() {
        // Given: one blank counter and one valid counter in the same scenario
        val scenarioId = 1L
        val validName = "RealCounter"
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestCounter(scenarioId, counterName = "")
            db.insertTestCounter(scenarioId, counterName = validName)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            // Then: only the valid counter remains
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(validName, cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate_multipleScenarios_blankCountersRemovedFromAll() {
        // Given: two scenarios each containing a blank and a valid counter
        val scenarioId1 = 1L
        val scenarioId2 = 2L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId1)
            db.insertTestScenario(scenarioId2)
            db.insertTestCounter(scenarioId1, counterName = "")
            db.insertTestCounter(scenarioId1, counterName = "CounterA")
            db.insertTestCounter(scenarioId2, counterName = "")
            db.insertTestCounter(scenarioId2, counterName = "CounterB")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            // Then: each scenario retains only its valid counter
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE WHERE length(trim(counterName)) = 0").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate_noCounters_succeeds() {
        // Given: a v21 database with no counters at all
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
        }

        // When / Then: migration must succeed and counters_table must stay empty
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration21to22).use { db ->
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    // ---- Helpers ----

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

    private fun SupportSQLiteDatabase.insertTestCounter(
        scenarioId: Long,
        counterName: String,
        startingValue: Double = 0.0,
    ) {
        insert(COUNTERS_TABLE, 0, ContentValues().apply {
            put("counterName", counterName)
            put("scenarioId", scenarioId)
            put("startingValue", startingValue)
        })
    }
}
