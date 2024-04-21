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
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV4Condition
import com.buzbuz.smartautoclicker.core.database.utils.getV5Conditions
import com.buzbuz.smartautoclicker.core.database.utils.*

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration4to5]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration4to5Tests {

    private companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrateConditions_nameColumn() {
        // Insert in V4 and close
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(getInsertV4Condition(24L, 1L, "", 0, 0, 0, 0, 0))
            close()
        }

        // Migrate
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5)

        // Verify
        val conditionsCursor = dbV5.query(getV5Conditions())
        Assert.assertEquals("Invalid conditions list size", 1, conditionsCursor.count)
        conditionsCursor.apply {
            moveToFirst()
            Assert.assertEquals("Invalid condition name", "Condition", getString(getColumnIndex("name")))
        }

        conditionsCursor.close()
        dbV5.close()
    }

    @Test
    fun migrateConditions_detectionTypeColumn() {
        // Insert in V4 and close
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(getInsertV4Condition(24L, 1L, "", 0, 0, 0, 0, 0))
            close()
        }

        // Migrate
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5)

        // Verify
        val conditionsCursor = dbV5.query(getV5Conditions())
        Assert.assertEquals("Invalid conditions list size", 1, conditionsCursor.count)
        conditionsCursor.apply {
            moveToFirst()
            Assert.assertEquals("Invalid condition name", 1, getInt(getColumnIndex("detection_type")))
        }

        conditionsCursor.close()
        dbV5.close()
    }

    @Test
    fun migrateConditions_thresholdIncrease() {
        val thresholdValue = 0

        // Insert in V4 and close
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(getInsertV4Condition(24L, 1L, "", 0, 0, 0, 0, thresholdValue))
            close()
        }

        // Migrate
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5)

        // Verify
        val conditionsCursor = dbV5.query(getV5Conditions())
        Assert.assertEquals("Invalid conditions list size", 1, conditionsCursor.count)
        conditionsCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid condition name",
                thresholdValue + Migration4to5.THRESHOLD_INCREASE,
                getInt(getColumnIndex("threshold"))
            )
        }

        conditionsCursor.close()
        dbV5.close()
    }

    @Test
    fun migrateConditions_thresholdIncrease_maxValue() {
        val thresholdValue =  Migration4to5.THRESHOLD_MAX_VALUE

        // Insert in V4 and close
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(getInsertV4Condition(24L, 1L, "", 0, 0, 0, 0, thresholdValue))
            close()
        }

        // Migrate
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5)

        // Verify
        val conditionsCursor = dbV5.query(getV5Conditions())
        Assert.assertEquals("Invalid conditions list size", 1, conditionsCursor.count)
        conditionsCursor.apply {
            moveToFirst()
            Assert.assertEquals(
                "Invalid condition name",
                thresholdValue,
                getInt(getColumnIndex("threshold"))
            )
        }

        conditionsCursor.close()
        dbV5.close()
    }
}