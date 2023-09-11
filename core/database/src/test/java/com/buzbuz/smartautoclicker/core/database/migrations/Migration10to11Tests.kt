/*
 * Copyright (C) 2023 Kevin Buzeau
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

import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventType
import com.buzbuz.smartautoclicker.core.database.utils.*

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration10to11]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration10to11Tests {

    private companion object {
        private const val TEST_DB = "migration-test"

        private const val OLD_DB_VERSION = 10
        private const val NEW_DB_VERSION = 11
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrate_quality() {
        val id = 1L
        val quality = 1200

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Scenario(id, quality))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Scenarios()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()

            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(quality + 600, "detection_quality")
        }
    }

    @Test
    fun migrate_swipe() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val fromX = 0
        val fromY = 1
        val toX = 10
        val toY = 11
        val duration = 500L
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Swipe(id, evtId, name, fromX, fromY, toX,toY, duration, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnEquals(fromX, "fromX")
            cursor.assertColumnEquals(fromY, "fromY")
            cursor.assertColumnEquals(toX, "toX")
            cursor.assertColumnEquals(toY, "toY")
            cursor.assertColumnEquals(duration, "swipeDuration")
        }
        dbV11.close()
    }

    @Test
    fun migrate_pause() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val duration = 500L
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Pause(id, evtId, name, duration, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnEquals(duration, "pauseDuration")
        }
        dbV11.close()
    }

    @Test
    fun migrate_intent() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val isAdvanced = true
        val isBroadcast = false
        val action = "com.toto"
        val comp = "com.toto/TOTO"
        val flags = 17
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Intent(id, evtId, name, isAdvanced, isBroadcast, action, comp, flags, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnEquals(isAdvanced, "isAdvanced")
            cursor.assertColumnEquals(isBroadcast, "isBroadcast")
            cursor.assertColumnEquals(action, "intent_action")
            cursor.assertColumnEquals(comp, "component_name")
            cursor.assertColumnEquals(flags, "flags")
        }
        dbV11.close()
    }

    @Test
    fun migrate_toggle_event() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val toggleEventId = 12L
        val toggleType = ToggleEventType.TOGGLE
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10ToggleEvent(id, evtId, name, toggleEventId, toggleType, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnEquals(toggleEventId, "toggle_event_id")
            cursor.assertColumnEquals(toggleType, "toggle_type")
        }
        dbV11.close()
    }

    @Test
    fun migrate_click_on_position() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val x = 0
        val y = 1
        val clickOnCondition = false
        val duration = 500L
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Click(id, evtId, name, x, y, clickOnCondition, duration, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnEquals(x, "x")
            cursor.assertColumnEquals(y, "y")
            cursor.assertColumnEquals(ClickPositionType.USER_SELECTED, "clickPositionType")
            cursor.assertColumnNull("clickOnConditionId")
            cursor.assertColumnEquals(duration, "pressDuration")
        }
        dbV11.close()
    }

    @Test
    fun migrate_click_on_condition_no_conditions() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val clickOnCondition = true
        val duration = 500L
        val priority = 1

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Click(id, evtId, name, null, null, clickOnCondition, duration, priority))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnNull("x")
            cursor.assertColumnNull("y")
            cursor.assertColumnEquals(ClickPositionType.ON_DETECTED_CONDITION, "clickPositionType")
            cursor.assertColumnNull("clickOnConditionId")
            cursor.assertColumnEquals(duration, "pressDuration")
        }
        dbV11.close()
    }

    @Test
    fun migrate_click_on_condition_invalid_condition() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val clickOnCondition = true
        val duration = 500L
        val priority = 1
        val conditionId = 25L
        val shouldBeDetected = false

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Click(id, evtId, name, null, null, clickOnCondition, duration, priority))
            execSQL(getInsertV10Condition(conditionId, evtId, "TOTO", "/toto", 0, 1, 2, 3, 10, 0, shouldBeDetected))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnNull("x")
            cursor.assertColumnNull("y")
            cursor.assertColumnEquals(ClickPositionType.ON_DETECTED_CONDITION, "clickPositionType")
            cursor.assertColumnNull("clickOnConditionId")
            cursor.assertColumnEquals(duration, "pressDuration")
        }
        dbV11.close()
    }

    @Test
    fun migrate_click_on_condition_valid_condition() {
        val id = 1L
        val evtId = 2L
        val name = "TOTO"
        val clickOnCondition = true
        val duration = 500L
        val priority = 1
        val conditionId = 25L
        val shouldBeDetected = true

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getInsertV10Click(id, evtId, name, null, null, clickOnCondition, duration, priority))
            execSQL(getInsertV10Condition(conditionId, evtId, "TOTO", "/toto", 0, 1, 2, 3, 10, 0, shouldBeDetected))
            close()
        }

        // Migrate
        val dbV11 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11)

        // Verify
        dbV11.query(getV11Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(id, "id")
            cursor.assertColumnEquals(evtId, "eventId")
            cursor.assertColumnEquals(name, "name")
            cursor.assertColumnEquals(priority, "priority")
            cursor.assertColumnNull("x")
            cursor.assertColumnNull("y")
            cursor.assertColumnEquals(ClickPositionType.ON_DETECTED_CONDITION, "clickPositionType")
            cursor.assertColumnEquals(conditionId, "clickOnConditionId")
            cursor.assertColumnEquals(duration, "pressDuration")
        }
        dbV11.close()
    }
}