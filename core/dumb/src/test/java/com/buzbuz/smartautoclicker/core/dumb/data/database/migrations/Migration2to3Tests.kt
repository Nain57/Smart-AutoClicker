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
package com.buzbuz.smartautoclicker.core.dumb.data.database.migrations

import android.content.Context
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerMotionType
import com.buzbuz.smartautoclicker.core.dumb.utils.assertColumnEquals
import com.buzbuz.smartautoclicker.core.dumb.utils.assertColumnMissing
import com.buzbuz.smartautoclicker.core.dumb.utils.assertColumnNull
import com.buzbuz.smartautoclicker.core.dumb.utils.assertCountEquals
import com.buzbuz.smartautoclicker.core.dumb.utils.getInsertV2Click
import com.buzbuz.smartautoclicker.core.dumb.utils.getInsertV2Pause
import com.buzbuz.smartautoclicker.core.dumb.utils.getInsertV2Scenario
import com.buzbuz.smartautoclicker.core.dumb.utils.getInsertV2Swipe
import com.buzbuz.smartautoclicker.core.dumb.utils.getV3Actions
import com.buzbuz.smartautoclicker.core.dumb.utils.getV3GestureFingerMotions
import com.buzbuz.smartautoclicker.core.dumb.utils.getV3GestureFingerMotionsForFinger
import com.buzbuz.smartautoclicker.core.dumb.utils.getV3GestureFingers
import com.buzbuz.smartautoclicker.core.dumb.utils.getV3GestureFingersForAction

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration2to3]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration2to3Tests {

    private companion object {
        private const val OLD_DB_VERSION = 2
        private const val NEW_DB_VERSION = 3

        private const val SCENARIO_ID = 1L

        private const val ACTION_ID_COLUMN = "id"
        private const val ACTION_TYPE_COLUMN = "type"
        private const val ACTION_NAME_COLUMN = "name"
        private const val ACTION_PRIORITY_COLUMN = "priority"
        private const val ACTION_REPEAT_COUNT_COLUMN = "repeat_count"
        private const val ACTION_X_COLUMN = "x"
        private const val ACTION_Y_COLUMN = "y"
        private const val ACTION_PRESS_DURATION_COLUMN = "press_duration"
        private const val ACTION_PAUSE_DURATION_COLUMN = "pause_duration"
        private const val ACTION_SWIPE_DURATION_COLUMN = "swipe_duration"
        private const val ACTION_FROM_X_COLUMN = "fromX"
        private const val ACTION_FROM_Y_COLUMN = "fromY"
        private const val ACTION_TO_X_COLUMN = "toX"
        private const val ACTION_TO_Y_COLUMN = "toY"
        private const val FINGER_ACTION_ID_COLUMN = "dumb_action_id"
        private const val FINGER_INDEX_COLUMN = "index"
        private const val FINGER_START_TIME_MS_COLUMN = "start_time_ms"
        private const val MOTION_INDEX_COLUMN = "index"
        private const val MOTION_TYPE_COLUMN = "motion_type"
        private const val MOTION_DURATION_MS_COLUMN = "duration_ms"
        private const val MOTION_X_COLUMN = "x"
        private const val MOTION_Y_COLUMN = "y"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DumbDatabase::class.java,
    )

    private lateinit var dbPath: String

    @Before
    fun setUp() {
        dbPath = ApplicationProvider
            .getApplicationContext<Context>()
            .getDatabasePath("migration-test").path
    }

    @Test
    fun migrate_noSwipes_gestureTablesAreEmpty() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Click(1L, SCENARIO_ID, "click", 0, 10, 20, 1L))
            execSQL(getInsertV2Pause(2L, SCENARIO_ID, "pause", 1, 50L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3GestureFingers()).use { cursor -> cursor.assertCountEquals(0) }
        dbV3.query(getV3GestureFingerMotions()).use { cursor -> cursor.assertCountEquals(0) }

        dbV3.close()
    }

    @Test
    fun migrate_noSwipes_otherActionsUnchanged() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Click(1L, SCENARIO_ID, "click", 0, 10, 20, 100L))
            execSQL(getInsertV2Pause(2L, SCENARIO_ID, "pause", 1, 50L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3Actions()).use { cursor ->
            cursor.assertCountEquals(2)

            cursor.moveToFirst()
            cursor.assertColumnEquals(1L, ACTION_ID_COLUMN)
            cursor.assertColumnEquals(DumbActionType.CLICK.name, ACTION_TYPE_COLUMN)
            cursor.assertColumnEquals("click", ACTION_NAME_COLUMN)
            cursor.assertColumnEquals(0, ACTION_PRIORITY_COLUMN)
            cursor.assertColumnEquals(10, ACTION_X_COLUMN)
            cursor.assertColumnEquals(20, ACTION_Y_COLUMN)
            cursor.assertColumnEquals(100L, ACTION_PRESS_DURATION_COLUMN)

            cursor.moveToNext()
            cursor.assertColumnEquals(2L, ACTION_ID_COLUMN)
            cursor.assertColumnEquals(DumbActionType.PAUSE.name, ACTION_TYPE_COLUMN)
            cursor.assertColumnEquals("pause", ACTION_NAME_COLUMN)
            cursor.assertColumnEquals(1, ACTION_PRIORITY_COLUMN)
            cursor.assertColumnEquals(50L, ACTION_PAUSE_DURATION_COLUMN)
        }

        dbV3.close()
    }

    @Test
    fun migrate_swipeColumnsRemoved() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Swipe(1L, SCENARIO_ID, "swipe", 0, 10, 20, 30, 40, 500L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3Actions()).use { cursor ->
            cursor.assertColumnMissing(ACTION_SWIPE_DURATION_COLUMN)
            cursor.assertColumnMissing(ACTION_FROM_X_COLUMN)
            cursor.assertColumnMissing(ACTION_FROM_Y_COLUMN)
            cursor.assertColumnMissing(ACTION_TO_X_COLUMN)
            cursor.assertColumnMissing(ACTION_TO_Y_COLUMN)
        }

        dbV3.close()
    }

    @Test
    fun migrate_oneSwipe_actionBecomesGestureAndKeepsCommonValues() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Swipe(1L, SCENARIO_ID, "swipe", 3, 10, 20, 30, 40, 500L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(1L, ACTION_ID_COLUMN)
            cursor.assertColumnEquals(DumbActionType.GESTURE.name, ACTION_TYPE_COLUMN)
            cursor.assertColumnEquals("swipe", ACTION_NAME_COLUMN)
            cursor.assertColumnEquals(3, ACTION_PRIORITY_COLUMN)
            cursor.assertColumnEquals(1, ACTION_REPEAT_COUNT_COLUMN)
        }

        dbV3.close()
    }

    @Test
    fun migrate_oneSwipe_oneFingerCreated() {
        val actionId = 1L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Swipe(actionId, SCENARIO_ID, "swipe", 0, 10, 20, 30, 40, 500L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3GestureFingersForAction(actionId)).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(actionId, FINGER_ACTION_ID_COLUMN)
            cursor.assertColumnEquals(0, FINGER_INDEX_COLUMN)
            cursor.assertColumnEquals(0L, FINGER_START_TIME_MS_COLUMN)
        }

        dbV3.close()
    }

    @Test
    fun migrate_oneSwipe_threeMotionsCreated() {
        val actionId = 1L
        val fromX = 10
        val fromY = 20
        val toX = 30
        val toY = 40
        val swipeDuration = 500L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Swipe(actionId, SCENARIO_ID, "swipe", 0, fromX, fromY, toX, toY, swipeDuration))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        val fingerId = dbV3.query(getV3GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV3.query(getV3GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.assertCountEquals(3)

            // PRESS: finger down at (fromX, fromY)
            cursor.moveToFirst()
            cursor.assertColumnEquals(0, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(DumbGestureFingerMotionType.PRESS.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(fromX, MOTION_X_COLUMN)
            cursor.assertColumnEquals(fromY, MOTION_Y_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)

            // MOVE: drag to (toX, toY) over swipeDuration
            cursor.moveToNext()
            cursor.assertColumnEquals(1, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(DumbGestureFingerMotionType.MOVE.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(toX, MOTION_X_COLUMN)
            cursor.assertColumnEquals(toY, MOTION_Y_COLUMN)
            cursor.assertColumnEquals(swipeDuration, MOTION_DURATION_MS_COLUMN)

            // RELEASE: finger up, no coordinates
            cursor.moveToNext()
            cursor.assertColumnEquals(2, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(DumbGestureFingerMotionType.RELEASE.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_X_COLUMN)
            cursor.assertColumnNull(MOTION_Y_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)
        }

        dbV3.close()
    }

    @Test
    fun migrate_multipleSwipes_eachGetsOwnFingerAndMotions() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV2Scenario(SCENARIO_ID, "scenario"))
            execSQL(getInsertV2Swipe(1L, SCENARIO_ID, "swipe1", 0, 10, 20, 30, 40, 100L))
            execSQL(getInsertV2Click(2L, SCENARIO_ID, "click", 1, 10, 20, 1L))
            execSQL(getInsertV2Swipe(3L, SCENARIO_ID, "swipe2", 2, 50, 60, 70, 80, 200L))
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration2to3)

        dbV3.query(getV3Actions()).use { cursor ->
            cursor.assertCountEquals(3)
            cursor.moveToFirst(); cursor.assertColumnEquals(DumbActionType.GESTURE.name, ACTION_TYPE_COLUMN)
            cursor.moveToNext(); cursor.assertColumnEquals(DumbActionType.CLICK.name, ACTION_TYPE_COLUMN)
            cursor.moveToNext(); cursor.assertColumnEquals(DumbActionType.GESTURE.name, ACTION_TYPE_COLUMN)
        }

        dbV3.query(getV3GestureFingersForAction(1L)).use { cursor -> cursor.assertCountEquals(1) }
        dbV3.query(getV3GestureFingersForAction(2L)).use { cursor -> cursor.assertCountEquals(0) }
        dbV3.query(getV3GestureFingersForAction(3L)).use { cursor -> cursor.assertCountEquals(1) }
        dbV3.query(getV3GestureFingerMotions()).use { cursor -> cursor.assertCountEquals(6) }

        dbV3.close()
    }
}
