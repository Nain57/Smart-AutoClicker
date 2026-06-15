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
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the AutoMigration from database v20 to v21. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration20to21Tests {

    private companion object {
        private const val OLD_DB_VERSION = 20
        private const val NEW_DB_VERSION = 21

        private const val COOLDOWN_COLUMN = "detecetion_cooldown_ms"
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
    fun migrate_existingEvent_cooldownIsNull() {
        // Given: a v20 event with no cooldown column
        val eventId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
            db.insertTestEvent(eventId, scenarioId = 1L)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            // Then: existing row has NULL for the new cooldown column
            db.query("SELECT $COOLDOWN_COLUMN FROM $EVENT_TABLE WHERE id = $eventId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertNull(cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate_multipleEvents_allCooldownsAreNull() {
        // Given: several v20 events
        val eventIds = listOf(1L, 2L, 3L)
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
            eventIds.forEach { id -> db.insertTestEvent(id, scenarioId = 1L) }
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            // Then: every pre-existing event has NULL cooldown
            db.query("SELECT id, $COOLDOWN_COLUMN FROM $EVENT_TABLE").use { cursor ->
                var rowCount = 0
                while (cursor.moveToNext()) {
                    rowCount++
                    assertNull(
                        "Event id=${cursor.getLong(0)} should have null cooldown",
                        cursor.getString(1),
                    )
                }
                assertEquals(eventIds.size, rowCount)
            }
        }
    }

    @Test
    fun migrate_existingEvent_otherColumnsPreserved() {
        // Given: a v20 event with specific field values
        val eventId = 42L
        val scenarioId = 7L
        val name = "MyEvent"
        val operator = 1
        val priority = 3
        val enabledOnStart = 0
        val keepDetecting = 1

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(
                id = eventId,
                scenarioId = scenarioId,
                name = name,
                operator = operator,
                priority = priority,
                enabledOnStart = enabledOnStart,
                keepDetecting = keepDetecting,
            )
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            // Then: original column values are unchanged
            db.query("SELECT scenario_id, name, operator, priority, enabled_on_start, keep_detecting FROM $EVENT_TABLE WHERE id = $eventId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(scenarioId, cursor.getLong(0))
                assertEquals(name, cursor.getString(1))
                assertEquals(operator, cursor.getInt(2))
                assertEquals(priority, cursor.getInt(3))
                assertEquals(enabledOnStart, cursor.getInt(4))
                assertEquals(keepDetecting, cursor.getInt(5))
            }
        }
    }

    @Test
    fun migrate_noEvents_succeeds() {
        // Given: a v20 database with a scenario but no events
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
        }

        // When / Then: migration must succeed and event_table must be empty
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT COUNT(*) FROM $EVENT_TABLE").use { cursor ->
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

    private fun SupportSQLiteDatabase.insertTestEvent(
        id: Long,
        scenarioId: Long,
        name: String = "Event $id",
        operator: Int = 0,
        priority: Int = 0,
        enabledOnStart: Int = 1,
        keepDetecting: Int? = null,
        type: String = "IMAGE_EVENT",
    ) {
        insert(EVENT_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("scenario_id", scenarioId)
            put("name", name)
            put("operator", operator)
            put("priority", priority)
            put("enabled_on_start", enabledOnStart)
            put("type", type)
            if (keepDetecting != null) put("keep_detecting", keepDetecting)
        })
    }
}
