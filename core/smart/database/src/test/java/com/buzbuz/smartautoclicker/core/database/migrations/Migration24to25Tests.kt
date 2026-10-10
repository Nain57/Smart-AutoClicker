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
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.utils.assertColumnEquals
import com.buzbuz.smartautoclicker.core.database.utils.assertColumnNull
import com.buzbuz.smartautoclicker.core.database.utils.assertCountEquals
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV23EmptyAction
import com.buzbuz.smartautoclicker.core.database.utils.getInsertV24ChangeCounter
import com.buzbuz.smartautoclicker.core.database.utils.getV25Actions

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration24to25]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration24to25Tests {

    private companion object {
        private const val OLD_DB_VERSION = 24
        private const val NEW_DB_VERSION = 25

        private const val ACTION_TYPE_COLUMN = "type"
        private const val COUNTER_NAME_COLUMN = "counter_name"
        private const val COUNTER_OPERATION_COLUMN = "counter_operation"
        private const val COUNTER_OPERATION_VALUE_TYPE_COLUMN = "counter_operation_value_type"
        private const val COUNTER_OPERATION_VALUE_COLUMN = "counter_operation_value"
        private const val COUNTER_OPERATION_COUNTER_NAME_COLUMN = "counter_operation_counter_name"
        private const val COUNTER_OPERATION_CONDITION_ID_COLUMN = "counter_operation_condition_id"
        private const val COUNTER_OPERATION_CONDITION_POSITION_TYPE_COLUMN = "counter_operation_condition_position_type"
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
    fun migrate_changeCounters_valuesKept_conditionColumnsNull() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(
                getInsertV24ChangeCounter(
                    id = 1L, eventId = 1L, name = "number", priority = 0, counterName = "counterA",
                    counterOperation = ChangeCounterOperationType.ADD.name,
                    counterOperationValueType = CounterOperationValueType.NUMBER.name,
                    counterOperationValue = 3.5, counterOperationCounterName = null,
                )
            )
            execSQL(
                getInsertV24ChangeCounter(
                    id = 2L, eventId = 1L, name = "counter", priority = 1, counterName = "counterA",
                    counterOperation = ChangeCounterOperationType.SET.name,
                    counterOperationValueType = CounterOperationValueType.COUNTER.name,
                    counterOperationValue = null, counterOperationCounterName = "counterB",
                )
            )
            close()
        }

        val dbV25 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration24to25)

        dbV25.query(getV25Actions()).use { cursor ->
            cursor.assertCountEquals(2)

            cursor.moveToFirst()
            cursor.assertColumnEquals(ActionType.CHANGE_COUNTER.name, ACTION_TYPE_COLUMN)
            cursor.assertColumnEquals("counterA", COUNTER_NAME_COLUMN)
            cursor.assertColumnEquals(ChangeCounterOperationType.ADD.name, COUNTER_OPERATION_COLUMN)
            cursor.assertColumnEquals(CounterOperationValueType.NUMBER.name, COUNTER_OPERATION_VALUE_TYPE_COLUMN)
            assertEquals(3.5, cursor.getDouble(cursor.getColumnIndex(COUNTER_OPERATION_VALUE_COLUMN)), 0.0)
            cursor.assertColumnNull(COUNTER_OPERATION_COUNTER_NAME_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_ID_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_POSITION_TYPE_COLUMN)

            cursor.moveToNext()
            cursor.assertColumnEquals(ChangeCounterOperationType.SET.name, COUNTER_OPERATION_COLUMN)
            cursor.assertColumnEquals(CounterOperationValueType.COUNTER.name, COUNTER_OPERATION_VALUE_TYPE_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_VALUE_COLUMN)
            cursor.assertColumnEquals("counterB", COUNTER_OPERATION_COUNTER_NAME_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_ID_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_POSITION_TYPE_COLUMN)
        }

        dbV25.close()
    }

    @Test
    fun migrate_otherActions_conditionColumnsNull() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).apply {
            execSQL(getInsertV23EmptyAction(1L, 1L, "pause", ActionType.PAUSE.name, 0))
            close()
        }

        val dbV25 = helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration24to25)

        dbV25.query(getV25Actions()).use { cursor ->
            cursor.assertCountEquals(1)
            cursor.moveToFirst()
            cursor.assertColumnEquals(ActionType.PAUSE.name, ACTION_TYPE_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_ID_COLUMN)
            cursor.assertColumnNull(COUNTER_OPERATION_CONDITION_POSITION_TYPE_COLUMN)
        }

        dbV25.close()
    }
}
