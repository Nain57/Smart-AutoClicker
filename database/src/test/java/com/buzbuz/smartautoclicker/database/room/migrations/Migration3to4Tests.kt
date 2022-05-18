/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room.migrations

import android.database.Cursor
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.entity.ActionType
import com.buzbuz.smartautoclicker.database.utils.getInsertV3Click
import com.buzbuz.smartautoclicker.database.utils.getInsertV3Condition
import com.buzbuz.smartautoclicker.database.utils.getInsertV3ConditionCrossRef
import com.buzbuz.smartautoclicker.database.utils.getInsertV3Scenario
import com.buzbuz.smartautoclicker.database.utils.getV4Actions
import com.buzbuz.smartautoclicker.database.utils.getV4Conditions
import com.buzbuz.smartautoclicker.database.utils.getV4Events
import com.buzbuz.smartautoclicker.database.utils.getV4Scenarios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration3to4]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration3to4Tests {

    private companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrateScenarios_one() {
        val scenarioId = 1L
        val scenarioName = "TOTO"

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, scenarioName))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val scenarioCursor = dbV4.query(getV4Scenarios())
        assertEquals("Invalid scenario list size", 1, scenarioCursor.count)
        scenarioCursor.moveToFirst()
        assertDbScenario(scenarioCursor, scenarioId, scenarioName)

        scenarioCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateScenarios_multiple() {
        val scenarioId1 = 1L
        val scenarioName1 = "TOTO"
        val scenarioId2 = 2L
        val scenarioName2 = "TATA"
        val scenarioId3 = 3L
        val scenarioName3 = "TUTU"

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId1, scenarioName1))
            execSQL(getInsertV3Scenario(scenarioId2, scenarioName2))
            execSQL(getInsertV3Scenario(scenarioId3, scenarioName3))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val scenarioCursor = dbV4.query(getV4Scenarios())
        assertEquals("Invalid scenario list size", 3, scenarioCursor.count)
        scenarioCursor.moveToFirst()
        assertDbScenario(scenarioCursor, scenarioId1, scenarioName1)
        scenarioCursor.moveToNext()
        assertDbScenario(scenarioCursor, scenarioId2, scenarioName2)
        scenarioCursor.moveToNext()
        assertDbScenario(scenarioCursor, scenarioId3, scenarioName3)

        scenarioCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateConditions_one() {
        val scenarioId = 1L
        val clickId = 2L
        val conditionPath = "/TATA/TOTO"
        val left = 0
        val right = 10
        val top = 0
        val bottom = 5
        val width = right - left
        val height = top - bottom
        val threshold = 10

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(clickId, scenarioId, "TATA", 1, 1, 1, 1, 1,
                2, 1, 1, 1))
            execSQL(getInsertV3ConditionCrossRef(clickId, conditionPath))
            execSQL(getInsertV3Condition(conditionPath, left, top, right, bottom, width, height, threshold))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val conditionCursor = dbV4.query(getV4Conditions())
        assertEquals("Invalid condition list size", 1, conditionCursor.count)
        conditionCursor.moveToFirst()
        assertDbCondition(
            cursor = conditionCursor,
            eventId = clickId,
            path = conditionPath,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            threshold = threshold
        )

        conditionCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateConditions_multiple() {
        val scenarioId = 1L
        val clickId = 2L
        val conditionPath1 = "/TATA/TOTO"
        val left1 = 0
        val right1 = 10
        val top1 = 0
        val bottom1 = 5
        val width1 = right1 - left1
        val height1 = top1 - bottom1
        val threshold1 = 10
        val conditionPath2 = "/TUTU/TETE"
        val left2 = 10
        val right2 = 40
        val top2 = 4
        val bottom2 = 78
        val width2 = right2 - left2
        val height2 = top2 - bottom2
        val threshold2 = 2

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(clickId, scenarioId, "TATA", 1, 1, 1, 1, 1,
                2, 1, 1, 1))

            execSQL(getInsertV3ConditionCrossRef(clickId, conditionPath1))
            execSQL(getInsertV3Condition(conditionPath1, left1, top1, right1, bottom1, width1, height1, threshold1))

            execSQL(getInsertV3ConditionCrossRef(clickId, conditionPath2))
            execSQL(getInsertV3Condition(conditionPath2, left2, top2, right2, bottom2, width2, height2, threshold2))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val conditionCursor = dbV4.query(getV4Conditions())
        assertEquals("Invalid condition list size", 2, conditionCursor.count)
        conditionCursor.moveToFirst()
        assertDbCondition(
            cursor = conditionCursor,
            eventId = clickId,
            path = conditionPath1,
            left = left1,
            top = top1,
            right = right1,
            bottom = bottom1,
            threshold = threshold1,
        )
        conditionCursor.moveToNext()
        assertDbCondition(
            cursor = conditionCursor,
            eventId = clickId,
            path = conditionPath2,
            left = left2,
            top = top2,
            right = right2,
            bottom = bottom2,
            threshold = threshold2,
        )

        conditionCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateClickToEvent_one() {
        val scenarioId = 1L
        val clickId = 42L
        val clickName = "HELLO"
        val conditionOperator = 1
        val stopAfter = 5
        val priority = 1

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(clickId, scenarioId, clickName, 1, 1, 1, 1, 1, conditionOperator, 1, stopAfter, priority))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val eventCursor = dbV4.query(getV4Events())
        assertEquals("Invalid event list size", 1, eventCursor.count)
        eventCursor.moveToFirst()
        assertDbEvent(
            cursor = eventCursor,
            eventId = clickId,
            scenarioId = scenarioId,
            name = clickName,
            operator = conditionOperator,
            priority = priority,
            stopAfter = stopAfter,
        )

        eventCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateClickToEvent_multiple() {
        val scenarioId = 1L
        val clickId1 = 42L
        val clickName1 = "HELLO"
        val conditionOperator1 = 1
        val stopAfter1 = 5
        val priority1 = 1
        val clickId2 = 84L
        val clickName2 = "GUTEN TAG"
        val conditionOperator2 = 2
        val stopAfter2 = 1
        val priority2 = 2

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(clickId1, scenarioId, clickName1, 1, 1, 1, 1, 1,
                conditionOperator1, 1, stopAfter1, priority1))
            execSQL(getInsertV3Click(clickId2, scenarioId, clickName2, 1, 1, 1, 1, 1,
                conditionOperator2, 1, stopAfter2, priority2))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val eventCursor = dbV4.query(getV4Events())
        assertEquals("Invalid event list size", 2, eventCursor.count)
        eventCursor.moveToFirst()
        assertDbEvent(
            cursor = eventCursor,
            eventId = clickId1,
            scenarioId = scenarioId,
            name = clickName1,
            operator = conditionOperator1,
            priority = priority1,
            stopAfter = stopAfter1,
        )
        eventCursor.moveToNext()
        assertDbEvent(
            cursor = eventCursor,
            eventId = clickId2,
            scenarioId = scenarioId,
            name = clickName2,
            operator = conditionOperator2,
            priority = priority2,
            stopAfter = stopAfter2,
        )

        eventCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateClickToAction() {
        val scenarioId = 1L
        val clickId = 42L
        val clickName = "clickName !"
        val clickX = 10
        val clickY = 800
        val clickDelayAfter = 500L

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(clickId, scenarioId, clickName, 1, clickX, clickY, 1, 1,
                1, clickDelayAfter, 1, 1))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val actionCursor = dbV4.query(getV4Actions())
        assertEquals("Invalid action list size", 2, actionCursor.count)
        actionCursor.moveToFirst()
        assertDbClickAction(
            cursor = actionCursor,
            eventId = clickId,
            priority = 0,
            name = clickName,
            type = ActionType.CLICK.toString(),
            x = clickX,
            y = clickY,
            duration = 1
        )
        actionCursor.moveToNext()
        assertDbPauseAction(
            cursor = actionCursor,
            eventId = clickId,
            priority = 1,
            name = "Pause",
            type = ActionType.PAUSE.toString(),
            duration = clickDelayAfter
        )

        actionCursor.close()
        dbV4.close()
    }

    @Test
    fun migrateSwipeToAction() {
        val scenarioId = 1L
        val swipeId = 42L
        val swipeName = "swipeName !"
        val swipeX1 = 10
        val swipeY1 = 800
        val swipeX2 = 20
        val swipeY2 = 1600
        val swipeDelayAfter = 500L

        // Insert in V3 and close
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(getInsertV3Scenario(scenarioId, "TOTO"))
            execSQL(getInsertV3Click(swipeId, scenarioId, swipeName, 2, swipeX1, swipeY1, swipeX2, swipeY2,
                1, swipeDelayAfter, 1, 1))
            close()
        }

        // Migrate
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)

        // Verify
        val actionCursor = dbV4.query(getV4Actions())
        assertEquals("Invalid action list size", 2, actionCursor.count)
        actionCursor.moveToFirst()
        assertDbSwipeAction(
            cursor = actionCursor,
            eventId = swipeId,
            priority = 0,
            name = swipeName,
            type = ActionType.SWIPE.toString(),
            x1 = swipeX1,
            y1 = swipeY1,
            x2 = swipeX2,
            y2 = swipeY2,
            duration = 175
        )
        actionCursor.moveToNext()
        assertDbPauseAction(
            cursor = actionCursor,
            eventId = swipeId,
            priority = 1,
            name = "Pause",
            type = ActionType.PAUSE.toString(),
            duration = swipeDelayAfter
        )

        actionCursor.close()
        dbV4.close()
    }

    private fun assertDbScenario(cursor: Cursor, id: Long, name: String) {
        cursor.apply {
            assertEquals("Invalid scenario id", id, getLong(getColumnIndex("id")))
            assertEquals("Invalid scenario name", name, getString(getColumnIndex("name")))
        }
    }

    private fun assertDbEvent(cursor: Cursor, eventId: Long, scenarioId: Long, name: String,
                              operator: Int, priority: Int, stopAfter: Int) {
        cursor.apply {
            assertEquals("Invalid event id", eventId, getLong(getColumnIndex("id")))
            assertEquals("Invalid scenario id", scenarioId, getLong(getColumnIndex("scenario_id")))
            assertEquals("Invalid event name", name, getString(getColumnIndex("name")))
            assertEquals("Invalid event condition operator", operator, getInt(getColumnIndex("operator")))
            assertEquals("Invalid event priority", priority, getInt(getColumnIndex("priority")))
            assertEquals("Invalid event stop after", stopAfter, getInt(getColumnIndex("stop_after")))
        }
    }

    private fun assertDbCondition(cursor: Cursor, eventId: Long, path: String, left: Int, top: Int, right: Int,
                                  bottom: Int, threshold: Int) {
        cursor.apply {
            assertNotEquals("Invalid condition id", 0, getLong(getColumnIndex("id")))
            assertEquals("Invalid event id", eventId, getLong(getColumnIndex("eventId")))
            assertEquals("Invalid condition path", path, getString(getColumnIndex("path")))
            assertEquals("Invalid condition area left", left, getInt(getColumnIndex("area_left")))
            assertEquals("Invalid condition area top", top, getInt(getColumnIndex("area_top")))
            assertEquals("Invalid condition area right", right, getInt(getColumnIndex("area_right")))
            assertEquals("Invalid condition area bottom", bottom, getInt(getColumnIndex("area_bottom")))
            assertEquals("Invalid condition threshold", threshold, getInt(getColumnIndex("threshold")))
        }
    }

    private fun assertDbClickAction(cursor: Cursor, eventId: Long, priority: Int, name: String, type: String,
                                    x: Int, y: Int, duration: Long) {
        cursor.apply {
            assertNotEquals("Invalid action id", 0, getLong(getColumnIndex("id")))
            assertEquals("Invalid event id", eventId, getLong(getColumnIndex("eventId")))
            assertEquals("Invalid action priority", priority, getInt(getColumnIndex("priority")))
            assertEquals("Invalid action name", name, getString(getColumnIndex("name")))
            assertEquals("Invalid action type", type, getString(getColumnIndex("type")))
            assertEquals("Invalid click x", x, getInt(getColumnIndex("x")))
            assertEquals("Invalid click y", y, getInt(getColumnIndex("y")))
            assertEquals("Invalid click press duration", duration, getLong(getColumnIndex("pressDuration")))
        }
    }

    private fun assertDbSwipeAction(cursor: Cursor, eventId: Long, priority: Int, name: String, type: String,
                                    x1: Int, y1: Int, x2: Int, y2: Int, duration: Long) {
        cursor.apply {
            assertNotEquals("Invalid action id", 0, getLong(getColumnIndex("id")))
            assertEquals("Invalid event id", eventId, getLong(getColumnIndex("eventId")))
            assertEquals("Invalid action priority", priority, getInt(getColumnIndex("priority")))
            assertEquals("Invalid action name", name, getString(getColumnIndex("name")))
            assertEquals("Invalid action type", type, getString(getColumnIndex("type")))
            assertEquals("Invalid swipe start x", x1, getInt(getColumnIndex("fromX")))
            assertEquals("Invalid swipe start y", y1, getInt(getColumnIndex("fromY")))
            assertEquals("Invalid swipe end x", x2, getInt(getColumnIndex("toX")))
            assertEquals("Invalid swipe end y", y2, getInt(getColumnIndex("toY")))
            assertEquals("Invalid swipe duration", duration, getLong(getColumnIndex("swipeDuration")))
        }
    }

    private fun assertDbPauseAction(cursor: Cursor, eventId: Long, priority: Int, name: String, type: String, duration: Long) {
        cursor.apply {
            assertNotEquals("Invalid action id", 0, getLong(getColumnIndex("id")))
            assertEquals("Invalid event id", eventId, getLong(getColumnIndex("eventId")))
            assertEquals("Invalid action priority", priority, getInt(getColumnIndex("priority")))
            assertEquals("Invalid action name", name, getString(getColumnIndex("name")))
            assertEquals("Invalid action type", type, getString(getColumnIndex("type")))
            assertEquals("Invalid pause duration", duration, getLong(getColumnIndex("pauseDuration")))
        }
    }
}