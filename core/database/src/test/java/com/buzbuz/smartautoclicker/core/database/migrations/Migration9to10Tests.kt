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
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.utils.assertColumnEquals
import com.buzbuz.smartautoclicker.core.database.utils.assertCountEquals
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV9Click
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV9EmptyAction
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV9Swipe
import com.buzbuz.smartautoclicker.core.database.utils.getV10Actions
import com.buzbuz.smartautoclicker.core.database.utils.*

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration9to10]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration9to10Tests {

    private companion object {
        private const val TEST_DB = "migration-test"

        private const val OLD_DB_VERSION = 9
        private const val NEW_DB_VERSION = 10

        private const val PRESS_DURATION_COLUMN = "pressDuration"
        private const val SWIPE_DURATION_COLUMN = "swipeDuration"

        private fun getSqlCreateV9Click(id: Long, durationMs: Long): String =
            getInsertV9Click(
                id,
                1L,
                "toto",
                ActionType.CLICK.toString(),
                79, 97,
                0,
                durationMs,
                0,
            )

        private fun getSqlCreateV9Swipe(id: Long, durationMs: Long): String =
            getInsertV9Swipe(
                id,
                1L,
                "toto",
                ActionType.SWIPE.toString(),
                79, 97,
                12, 45,
                durationMs,
                0,
            )

        private fun getSqlCreateV9EmptyAction(id: Long, type: ActionType): String =
            getInsertV9EmptyAction(
                id,
                1L,
                "toto",
                type.toString(),
                0,
            )
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrate_click_durationOverLimit() {
        val oldDuration = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS + 500

        // Insert in V9 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getSqlCreateV9Click(24L, oldDuration))
            close()
        }

        // Migrate
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration9to10)

        // Verify
        val actionsCursor = dbV10.query(getV10Actions())
        actionsCursor.use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS, PRESS_DURATION_COLUMN)
        }

        dbV10.close()
    }

    @Test
    fun migrate_click_durationBelowLimit() {
        val oldDuration = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS - 500

        // Insert in V9 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getSqlCreateV9Click(24L, oldDuration))
            close()
        }

        // Migrate
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration9to10)

        // Verify
        val actionsCursor = dbV10.query(getV10Actions())
        actionsCursor.use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(oldDuration, PRESS_DURATION_COLUMN)
        }

        dbV10.close()
    }

    @Test
    fun migrate_swipe_durationOverLimit() {
        val oldDuration = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS + 500

        // Insert in V9 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getSqlCreateV9Swipe(24L, oldDuration))
            close()
        }

        // Migrate
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration9to10)

        // Verify
        val actionsCursor = dbV10.query(getV10Actions())
        actionsCursor.use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS, SWIPE_DURATION_COLUMN)
        }

        dbV10.close()
    }

    @Test
    fun migrate_swipe_durationBelowLimit() {
        val oldDuration = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS - 500

        // Insert in V9 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getSqlCreateV9Swipe(24L, oldDuration))
            close()
        }

        // Migrate
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration9to10)

        // Verify
        val actionsCursor = dbV10.query(getV10Actions())
        actionsCursor.use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(oldDuration, SWIPE_DURATION_COLUMN)
        }

        dbV10.close()
    }

    @Test
    fun migrate_oneOfEachCase() {
        val durationOver = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS + 500
        val durationBelow = Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS - 500

        // Insert in V9 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).apply {
            execSQL(getSqlCreateV9Click(1L, durationOver))
            execSQL(getSqlCreateV9Click(2L, durationBelow))
            execSQL(getSqlCreateV9Swipe(3L, durationOver))
            execSQL(getSqlCreateV9Swipe(4L, durationBelow))
            execSQL(getSqlCreateV9EmptyAction(5L, ActionType.PAUSE))
            execSQL(getSqlCreateV9EmptyAction(6L, ActionType.INTENT))
            execSQL(getSqlCreateV9EmptyAction(7L, ActionType.TOGGLE_EVENT))
            close()
        }

        // Migrate
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration9to10)

        // Verify
        val actionsCursor = dbV10.query(getV10Actions())
        actionsCursor.use { cursor ->
            cursor.apply {
                assertCountEquals(7)

                moveToFirst() // Click with duration over
                assertColumnEquals(Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS, PRESS_DURATION_COLUMN)
                assertColumnEquals(0L, SWIPE_DURATION_COLUMN)

                moveToNext() // Click with duration below
                assertColumnEquals(durationBelow, PRESS_DURATION_COLUMN)
                assertColumnEquals(0L, SWIPE_DURATION_COLUMN)

                moveToNext() // Swipe with duration over
                assertColumnEquals(Migration9to10.NEW_GESTURE_DURATION_LIMIT_MS, SWIPE_DURATION_COLUMN)
                assertColumnEquals(0L, PRESS_DURATION_COLUMN)

                moveToNext() // Swipe with duration below
                assertColumnEquals(durationBelow, SWIPE_DURATION_COLUMN)
                assertColumnEquals(0L, PRESS_DURATION_COLUMN)

                moveToNext() // Pause
                assertColumnEquals(0L, PRESS_DURATION_COLUMN)
                assertColumnEquals(0L, SWIPE_DURATION_COLUMN)

                moveToNext() // Intent
                assertColumnEquals(0L, PRESS_DURATION_COLUMN)
                assertColumnEquals(0L, SWIPE_DURATION_COLUMN)

                moveToNext() // Toggle
                assertColumnEquals(0L, PRESS_DURATION_COLUMN)
                assertColumnEquals(0L, SWIPE_DURATION_COLUMN)
            }
        }

        dbV10.close()
    }
}