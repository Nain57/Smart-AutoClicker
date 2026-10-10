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

import android.content.Context
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.utils.assertColumnEquals
import com.buzbuz.smartautoclicker.core.database.utils.assertColumnNull
import com.buzbuz.smartautoclicker.core.database.utils.assertCountEquals
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV23Click
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV23EmptyAction
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV23Swipe
import com.buzbuz.smartautoclicker.core.database.utils.getV24Actions
import com.buzbuz.smartautoclicker.core.database.utils.getV24GestureFingerMotionsForFinger
import com.buzbuz.smartautoclicker.core.database.utils.getV24GestureFingersForAction
import com.buzbuz.smartautoclicker.core.database.utils.getV24GestureFingers
import com.buzbuz.smartautoclicker.core.database.utils.getV24GestureFingerMotions

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration23to24]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration23to24Tests {

    private companion object {
        private const val OLD_DB_VERSION = 23
        private const val NEW_DB_VERSION = 24

        private const val ACTION_ID_COLUMN = "id"
        private const val ACTION_TYPE_COLUMN = "type"
        private const val FINGER_ACTION_ID_COLUMN = "action_id"
        private const val FINGER_INDEX_COLUMN = "index"
        private const val FINGER_START_TIME_MS_COLUMN = "start_time_ms"
        private const val MOTION_FINGER_ID_COLUMN = "finger_id"
        private const val MOTION_INDEX_COLUMN = "index"
        private const val MOTION_TYPE_COLUMN = "motion_type"
        private const val MOTION_DURATION_MS_COLUMN = "duration_ms"
        private const val MOTION_X_COORD_TYPE_COLUMN = "x_coordinate_type"
        private const val MOTION_Y_COORD_TYPE_COLUMN = "y_coordinate_type"
        private const val MOTION_X_LITERAL_COLUMN = "x_literal"
        private const val MOTION_Y_LITERAL_COLUMN = "y_literal"
        private const val MOTION_CONDITION_ID_COLUMN = "condition_id"
        private const val MOTION_X_OFFSET_COORD_TYPE_COLUMN = "x_offset_coordinate_type"
        private const val MOTION_Y_OFFSET_COORD_TYPE_COLUMN = "y_offset_coordinate_type"
        private const val MOTION_X_OFFSET_LITERAL_COLUMN = "x_offset_literal"
        private const val MOTION_Y_OFFSET_LITERAL_COLUMN = "y_offset_literal"
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
    fun migrate_noSwipes_gestureTablesAreEmpty() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23EmptyAction(1L, 1L, "click", ActionType.CLICK.name, 0))
            execSQL(getInsertV23EmptyAction(2L, 1L, "pause", ActionType.PAUSE.name, 1))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24GestureFingers()).use { cursor ->
            cursor.assertCountEquals(0)
        }
        dbV24.query(getV24GestureFingerMotions()).use { cursor ->
            cursor.assertCountEquals(0)
        }

        dbV24.close()
    }

    @Test
    fun migrate_noSwipes_nonSwipeActionTypesUnchanged() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23EmptyAction(1L, 1L, "click", ActionType.CLICK.name, 0))
            execSQL(getInsertV23EmptyAction(2L, 1L, "pause", ActionType.PAUSE.name, 1))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24Actions()).use { cursor ->
            cursor.assertCountEquals(2)
            cursor.moveToFirst()
            cursor.assertColumnEquals(ActionType.CLICK.name, ACTION_TYPE_COLUMN)
            cursor.moveToNext()
            cursor.assertColumnEquals(ActionType.PAUSE.name, ACTION_TYPE_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_oneSwipe_actionTypeBecomesGesture() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(1L, 1L, "swipe", 10, 20, 30, 40, 500L, 0))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(ActionType.GESTURE.name, ACTION_TYPE_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_oneSwipe_oneFingerCreated() {
        val actionId = 1L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(actionId, 1L, "swipe", 10, 20, 30, 40, 500L, 0))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(actionId, FINGER_ACTION_ID_COLUMN)
            cursor.assertColumnEquals(0, FINGER_INDEX_COLUMN)
            cursor.assertColumnEquals(0L, FINGER_START_TIME_MS_COLUMN)
        }

        dbV24.close()
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
            execSQL(getInsertV23Swipe(actionId, 1L, "swipe", fromX, fromY, toX, toY, swipeDuration, 0))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.assertCountEquals(3)

            // PRESS: finger down at (fromX, fromY)
            cursor.moveToFirst()
            cursor.assertColumnEquals(0, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.PRESS.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_Y_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(fromX, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(fromY, MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)

            // MOVE: drag to (toX, toY) over swipeDuration
            cursor.moveToNext()
            cursor.assertColumnEquals(1, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.MOVE.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_Y_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(toX, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(toY, MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnEquals(swipeDuration, MOTION_DURATION_MS_COLUMN)

            // RELEASE: finger up, no coordinates
            cursor.moveToNext()
            cursor.assertColumnEquals(2, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.RELEASE.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_Y_COORD_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_multipleSwipes_eachGetsOwnFingerAndMotions() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(1L, 1L, "swipe1", 10, 20, 30, 40, 100L, 0))
            execSQL(getInsertV23Swipe(2L, 1L, "swipe2", 50, 60, 70, 80, 200L, 1))
            execSQL(getInsertV23Swipe(3L, 1L, "swipe3", 90, 100, 110, 120, 300L, 2))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24Actions()).use { cursor ->
            cursor.assertCountEquals(3)
            cursor.moveToFirst(); cursor.assertColumnEquals(ActionType.GESTURE.name, ACTION_TYPE_COLUMN)
            cursor.moveToNext(); cursor.assertColumnEquals(ActionType.GESTURE.name, ACTION_TYPE_COLUMN)
            cursor.moveToNext(); cursor.assertColumnEquals(ActionType.GESTURE.name, ACTION_TYPE_COLUMN)
        }

        dbV24.query(getV24GestureFingers()).use { cursor ->
            cursor.assertCountEquals(3)
        }

        dbV24.query(getV24GestureFingerMotions()).use { cursor ->
            cursor.assertCountEquals(9)
        }

        dbV24.close()
    }

    @Test
    fun migrate_multipleSwipes_coordinatesAreCorrectPerSwipe() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(1L, 1L, "swipe1", 10, 20, 30, 40, 100L, 0))
            execSQL(getInsertV23Swipe(2L, 1L, "swipe2", 50, 60, 70, 80, 200L, 1))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        // Verify first swipe
        val finger1Id = dbV24.query(getV24GestureFingersForAction(1L)).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(1L, FINGER_ACTION_ID_COLUMN)
            cursor.getLong(cursor.getColumnIndex("id"))
        }
        dbV24.query(getV24GestureFingerMotionsForFinger(finger1Id)).use { cursor ->
            cursor.assertCountEquals(3)
            cursor.moveToFirst()
            cursor.assertColumnEquals(10, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(20, MOTION_Y_LITERAL_COLUMN)
            cursor.moveToNext()
            cursor.assertColumnEquals(30, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(40, MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnEquals(100L, MOTION_DURATION_MS_COLUMN)
        }

        // Verify second swipe
        val finger2Id = dbV24.query(getV24GestureFingersForAction(2L)).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(2L, FINGER_ACTION_ID_COLUMN)
            cursor.getLong(cursor.getColumnIndex("id"))
        }
        dbV24.query(getV24GestureFingerMotionsForFinger(finger2Id)).use { cursor ->
            cursor.assertCountEquals(3)
            cursor.moveToFirst()
            cursor.assertColumnEquals(50, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(60, MOTION_Y_LITERAL_COLUMN)
            cursor.moveToNext()
            cursor.assertColumnEquals(70, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(80, MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnEquals(200L, MOTION_DURATION_MS_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_userSelectedClick_actionTypeStaysClick() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = 1L, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.USER_SELECTED.name,
                    pressDuration = 250L, x = 10, y = 20,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(ActionType.CLICK.name, ACTION_TYPE_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_userSelectedClick_threeMotionsCreated() {
        val actionId = 1L
        val x = 10
        val y = 20
        val pressDuration = 250L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = actionId, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.USER_SELECTED.name,
                    pressDuration = pressDuration, x = x, y = y,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(0, FINGER_INDEX_COLUMN)
            cursor.assertColumnEquals(0L, FINGER_START_TIME_MS_COLUMN)
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.assertCountEquals(3)

            // PRESS: finger down on the click position
            cursor.moveToFirst()
            cursor.assertColumnEquals(0, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.PRESS.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_Y_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(x, MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnEquals(y, MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_CONDITION_ID_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)

            // WAIT: hold the press for the click duration
            cursor.moveToNext()
            cursor.assertColumnEquals(1, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.WAIT.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(pressDuration, MOTION_DURATION_MS_COLUMN)
            cursor.assertColumnNull(MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_Y_COORD_TYPE_COLUMN)

            // RELEASE: finger up, no coordinates
            cursor.moveToNext()
            cursor.assertColumnEquals(2, MOTION_INDEX_COLUMN)
            cursor.assertColumnEquals(GestureFingerMotionType.RELEASE.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_DURATION_MS_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_onConditionClick_pressMotionIsOnCondition() {
        val actionId = 1L
        val conditionId = 8L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = actionId, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    pressDuration = 250L, clickOnConditionId = conditionId,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.moveToFirst()
            cursor.assertColumnEquals(CoordinatesType.CONDITION.name, MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.CONDITION.name, MOTION_Y_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(conditionId, MOTION_CONDITION_ID_COLUMN)
            cursor.assertColumnNull(MOTION_X_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_Y_LITERAL_COLUMN)
            cursor.assertColumnNull(MOTION_X_OFFSET_COORD_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_Y_OFFSET_COORD_TYPE_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_onConditionClickWithOffset_pressMotionHasOffset() {
        val actionId = 1L
        val offsetX = -5
        val offsetY = 15

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = actionId, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    pressDuration = 250L, clickOnConditionId = 8L,
                    clickOffsetX = offsetX, clickOffsetY = offsetY,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.moveToFirst()
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_X_OFFSET_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(CoordinatesType.LITERAL.name, MOTION_Y_OFFSET_COORD_TYPE_COLUMN)
            cursor.assertColumnEquals(offsetX, MOTION_X_OFFSET_LITERAL_COLUMN)
            cursor.assertColumnEquals(offsetY, MOTION_Y_OFFSET_LITERAL_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_onConditionClickWithoutConditionId_pressMotionHasNoCondition() {
        val actionId = 1L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = actionId, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION.name,
                    pressDuration = 250L,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.moveToFirst()
            cursor.assertColumnEquals(CoordinatesType.CONDITION.name, MOTION_X_COORD_TYPE_COLUMN)
            cursor.assertColumnNull(MOTION_CONDITION_ID_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_clickWithoutPressDuration_waitMotionUsesDefault() {
        val actionId = 1L

        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV23Click(
                    id = actionId, eventId = 1L, name = "click", priority = 0,
                    clickPositionType = ClickPositionType.USER_SELECTED.name,
                    pressDuration = null, x = 10, y = 20,
                )
            )
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        val fingerId = dbV24.query(getV24GestureFingersForAction(actionId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndex("id"))
        }

        dbV24.query(getV24GestureFingerMotionsForFinger(fingerId)).use { cursor ->
            cursor.moveToFirst()
            cursor.moveToNext()
            cursor.assertColumnEquals(GestureFingerMotionType.WAIT.name, MOTION_TYPE_COLUMN)
            cursor.assertColumnEquals(1L, MOTION_DURATION_MS_COLUMN)
        }

        dbV24.close()
    }

    @Test
    fun migrate_clicksAndSwipes_eachGetsOwnFingerAndMotions() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(1L, 1L, "swipe", 10, 20, 30, 40, 100L, 0))
            execSQL(
                getInsertV23Click(
                    id = 2L, eventId = 1L, name = "click", priority = 1,
                    clickPositionType = ClickPositionType.USER_SELECTED.name,
                    pressDuration = 250L, x = 50, y = 60,
                )
            )
            execSQL(getInsertV23EmptyAction(3L, 1L, "pause", ActionType.PAUSE.name, 2))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24GestureFingers()).use { cursor ->
            cursor.assertCountEquals(2)
        }
        dbV24.query(getV24GestureFingerMotions()).use { cursor ->
            cursor.assertCountEquals(6)
        }
        dbV24.query(getV24GestureFingersForAction(3L)).use { cursor ->
            cursor.assertCountEquals(0)
        }

        dbV24.close()
    }

    @Test
    fun migrate_mixedActions_onlySwipesGetGestureData() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23Swipe(1L, 1L, "swipe", 10, 20, 30, 40, 500L, 0))
            execSQL(getInsertV23EmptyAction(2L, 1L, "click", ActionType.CLICK.name, 1))
            execSQL(getInsertV23EmptyAction(3L, 1L, "pause", ActionType.PAUSE.name, 2))
            close()
        }

        val dbV24 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration23to24)

        dbV24.query(getV24Actions()).use { cursor ->
            cursor.assertCountEquals(3)
        }
        dbV24.query(getV24GestureFingers()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(1L, FINGER_ACTION_ID_COLUMN)
        }
        dbV24.query(getV24GestureFingerMotions()).use { cursor ->
            cursor.assertCountEquals(3)
        }

        dbV24.close()
    }
}
