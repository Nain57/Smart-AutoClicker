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

import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV5Click
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV5Condition
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV5Pause
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV5Swipe
import com.buzbuz.smartautoclicker.core.database.utils.getV6Actions
import com.buzbuz.smartautoclicker.core.database.utils.getV6Conditions
import com.buzbuz.smartautoclicker.core.database.utils.*

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration5to6]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration5to6Tests {

    private companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrateConditions_shouldBeDetectedColumn() {
        // Insert in V5 and close
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(getInsertV5Condition(24L, 1L, "", "", 0, 0, 0, 0, 0, 1))
            close()
        }

        // Migrate
        val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5to6)

        // Verify
        val conditionsCursor = dbV6.query(getV6Conditions())
        Assert.assertEquals("Invalid conditions list size", 1, conditionsCursor.count)
        conditionsCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid shouldBeDetected value",
                1,
                getInt(getColumnIndex("shouldBeDetected"))
            )
        }

        conditionsCursor.close()
        dbV6.close()
    }

    @Test
    fun migrateActions_clickOnConditionColumn_click() {
        // Insert in V5 and close
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(getInsertV5Click(24L, 1L, "", ActionType.CLICK.toString(), 0, 0, 0, 0))
            close()
        }

        // Migrate
        val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5to6)

        // Verify
        val actionCursor = dbV6.query(getV6Actions())
        Assert.assertEquals("Invalid action list size", 1, actionCursor.count)
        actionCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid click on condition value",
                false,
                isNull(getColumnIndex("clickOnCondition"))
            )
            Assert.assertEquals(
                "Invalid click on condition value",
                0,
                getInt(getColumnIndex("clickOnCondition"))
            )
        }

        actionCursor.close()
        dbV6.close()
    }

    @Test
    fun migrateActions_clickOnConditionColumn_swipe() {
        // Insert in V5 and close
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(getInsertV5Swipe(24L, 1L, "", ActionType.SWIPE.toString(), 0, 0, 0, 0, 0, 0))
            close()
        }

        // Migrate
        val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5to6)

        // Verify
        val actionCursor = dbV6.query(getV6Actions())
        Assert.assertEquals("Invalid action list size", 1, actionCursor.count)
        actionCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid click on condition value",
                true,
                isNull(getColumnIndex("clickOnCondition"))
            )
        }

        actionCursor.close()
        dbV6.close()
    }

    @Test
    fun migrateActions_clickOnConditionColumn_pause() {
        // Insert in V5 and close
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(getInsertV5Pause(24L, 1L, "", ActionType.PAUSE.toString(), 0, 0))
            close()
        }

        // Migrate
        val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5to6)

        // Verify
        val actionCursor = dbV6.query(getV6Actions())
        Assert.assertEquals("Invalid action list size", 1, actionCursor.count)
        actionCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid click on condition value",
                true,
                isNull(getColumnIndex("clickOnCondition"))
            )
        }

        actionCursor.close()
        dbV6.close()
    }
}