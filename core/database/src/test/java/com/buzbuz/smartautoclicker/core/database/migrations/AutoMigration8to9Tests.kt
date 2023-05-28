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
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV8Event
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV8Scenario
import com.buzbuz.smartautoclicker.core.database.utils.getV9Events
import com.buzbuz.smartautoclicker.core.database.utils.getV9Scenario
import com.buzbuz.smartautoclicker.core.database.utils.*
import com.buzbuz.smartautoclicker.core.database.utils.TestsData
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [AutoMigration8to9]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AutoMigration8to9Tests {

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

    private fun Cursor.verifyV9Event(id: Long, scenarioId: Long, name: String, operator: Int, priority: Int) {
        Assert.assertEquals("Invalid event id", id, getLong(getColumnIndex("id")))
        Assert.assertEquals("Invalid event scenario id", scenarioId, getLong(getColumnIndex("scenario_id")))
        Assert.assertEquals("Invalid event name", name, getString(getColumnIndex("name")))
        Assert.assertEquals("Invalid event condition operator", operator, getInt(getColumnIndex("operator")))
        Assert.assertEquals("Invalid event priority", priority, getInt(getColumnIndex("priority")))
    }

    @Test
    fun migrate_scenario_randomize() {
        // Insert in V8 and close
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                getInsertV8Scenario(
                TestsData.SCENARIO_ID,
                TestsData.SCENARIO_NAME,
                TestsData.SCENARIO_DETECTION_QUALITY,
                TestsData.SCENARIO_END_CONDITION_OPERATOR,
            )
            )
            close()
        }

        // Migrate
        val dbV9 = helper.runMigrationsAndValidate(TEST_DB, 9, true)

        // Verify
        val scenarioCursor = dbV9.query(getV9Scenario())
        scenarioCursor.apply {
            verifyCount(1)
            moveToFirst()
            Assert.assertEquals(
                "Invalid randomize value",
                false,
                getInt(getColumnIndex("randomize")) == 1,
            )
        }

        scenarioCursor.close()
        dbV9.close()
    }

    @Test
    fun migrate_event_remove_stopAfter() {
        val priority = 9

        // Insert in V8 and close
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                getInsertV8Scenario(
                TestsData.SCENARIO_ID,
                TestsData.SCENARIO_NAME,
                TestsData.SCENARIO_DETECTION_QUALITY,
                TestsData.SCENARIO_END_CONDITION_OPERATOR,
            )
            )

            execSQL(
                getInsertV8Event(
                TestsData.EVENT_ID,
                TestsData.SCENARIO_ID,
                TestsData.EVENT_NAME,
                TestsData.EVENT_CONDITION_OPERATOR,
                stopAfter = 5,
                priority,
            )
            )
            close()
        }

        // Migrate
        val dbV9 = helper.runMigrationsAndValidate(TEST_DB, 9, true)

        // Verify
        val scenarioCursor = dbV9.query(getV9Events())
        scenarioCursor.apply {
            verifyCount(1)
            moveToFirst()
            verifyV9Event(
                TestsData.EVENT_ID,
                TestsData.SCENARIO_ID,
                TestsData.EVENT_NAME,
                TestsData.EVENT_CONDITION_OPERATOR,
                priority,
            )
        }

        scenarioCursor.close()
        dbV9.close()
    }

    @Test
    fun migrate_event_enabledOnStart() {
        // Insert in V8 and close
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                getInsertV8Scenario(
                TestsData.SCENARIO_ID,
                TestsData.SCENARIO_NAME,
                TestsData.SCENARIO_DETECTION_QUALITY,
                TestsData.SCENARIO_END_CONDITION_OPERATOR,
            )
            )

            execSQL(
                getInsertV8Event(
                TestsData.EVENT_ID,
                TestsData.SCENARIO_ID,
                TestsData.EVENT_NAME,
                TestsData.EVENT_CONDITION_OPERATOR,
                TestsData.EVENT_STOP_AFTER,
                0,
            )
            )
            close()
        }

        // Migrate
        val dbV9 = helper.runMigrationsAndValidate(TEST_DB, 9, true)

        // Verify
        val scenarioCursor = dbV9.query(getV9Events())
        scenarioCursor.apply {
            verifyCount(1)
            moveToFirst()
            Assert.assertEquals(
                "Invalid enabled on start value",
                true,
                getInt(getColumnIndex("enabled_on_start")) == 1,
            )
        }

        scenarioCursor.close()
        dbV9.close()
    }
}