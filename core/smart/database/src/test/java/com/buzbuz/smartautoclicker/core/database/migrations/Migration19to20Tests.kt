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

import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration19to20]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration19to20Tests {

    private companion object {
        private const val OLD_DB_VERSION = 19
        private const val NEW_DB_VERSION = 20
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
    fun migrate_counters_new_type() {
        // Given
        val actionId = 1L
        val conditionId = 1L
        val actionValue = 10
        val conditionValue = 5

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
            db.insertTestEvent(1L, 1L)
            db.insertTestAction(actionId, 1L, type = ActionType.CHANGE_COUNTER, counterOperationValue = actionValue)
            db.insertTestCondition(conditionId, 1L, type = ConditionType.ON_COUNTER_REACHED, counterValue = conditionValue)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT counter_operation_value FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(actionValue.toDouble(), cursor.getDouble(0), 0.0)
            }
            db.query("SELECT counter_value FROM $CONDITION_TABLE WHERE id = $conditionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(conditionValue.toDouble(), cursor.getDouble(0), 0.0)
            }
        }
    }

    @Test
    fun migrate_counters_creation_no_counters() {
        // Given
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(1L)
            db.insertTestEvent(1L, 1L)
            db.insertTestAction(1L, 1L, type = ActionType.CLICK)
            db.insertTestCondition(1L, 1L, type = ConditionType.ON_COLOR_DETECTED)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate_counters_creation_action_only() {
        // Given
        val scenarioId = 1L
        val counterName1 = "CounterA"
        val counterName2 = "CounterB"
        val counterName3 = "CounterC"

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(1L, 1L, type = ActionType.CHANGE_COUNTER, counterName = counterName1)
            db.insertTestAction(2L, 1L, type = ActionType.CHANGE_COUNTER, counterOperationCounterName = counterName2)
            db.insertTestAction(3L, 1L, type = ActionType.NOTIFICATION, notificationMessageCounterName = counterName3)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.assertScenarioHasCounters(scenarioId, setOf(counterName1, counterName2, counterName3))
        }
    }

    @Test
    fun migrate_notification_counter_text_escapes_sql_value() {
        // Given
        val scenarioId = 1L
        val actionId = 860L
        val counterName = "ConnectionError"
        val expectedText = "ConnectionError = {ConnectionError}"

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(
                id = actionId,
                eventId = 1L,
                type = ActionType.NOTIFICATION,
                notificationMessageCounterName = counterName,
            )
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT notification_message_text FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(expectedText, cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate_counters_creation_condition_only() {
        // Given
        val scenarioId = 1L
        val counterName1 = "CounterD"
        val counterName2 = "CounterE"
        val counterName3 = "CounterF"

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestCondition(1L, 1L, type = ConditionType.ON_COUNTER_REACHED, counterName = counterName1)
            db.insertTestCondition(2L, 1L, type = ConditionType.ON_COUNTER_REACHED, counterValueCounterName = counterName2)
            db.insertTestCondition(3L, 1L, type = ConditionType.ON_NUMBER_DETECTED, numberCounterValueCounterName = counterName3)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.assertScenarioHasCounters(scenarioId, setOf(counterName1, counterName2, counterName3))
        }
    }

    // --- Blank counter regression tests (empty-string counter names must not create a blank counter) ---

    @Test
    fun migrate_blankCounter_notCreatedFromEmptyActionCounterName() {
        // Given: a CHANGE_COUNTER action whose counter_name is empty string (not null)
        // This simulates data produced by older app versions that stored "" instead of NULL.
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(1L, 1L, type = ActionType.CHANGE_COUNTER, counterName = "")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then: no blank counter must be created in the counters table
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    assertTrue("Blank counter name must not be created, found: '$name'", name.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun migrate_blankCounter_notCreatedFromEmptyActionOperationCounterName() {
        // Given: a CHANGE_COUNTER action whose counter_operation_counter_name is empty string
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(1L, 1L, type = ActionType.CHANGE_COUNTER, counterOperationCounterName = "")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    assertTrue("Blank counter name must not be created, found: '$name'", name.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun migrate_blankCounter_notCreatedFromEmptyNotificationCounterName() {
        // Given: a NOTIFICATION action whose notification_message_counter_name is empty string
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(
                id = 1L,
                eventId = 1L,
                type = ActionType.NOTIFICATION,
                notificationMessageCounterName = "",
            )
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    assertTrue("Blank counter name must not be created, found: '$name'", name.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun migrate_blankCounter_notCreatedFromEmptyConditionCounterName() {
        // Given: an ON_COUNTER_REACHED condition whose counter_name is empty string
        val scenarioId = 1L
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestCondition(1L, 1L, type = ConditionType.ON_COUNTER_REACHED, counterName = "")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.query("SELECT counterName FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    assertTrue("Blank counter name must not be created, found: '$name'", name.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun migrate_blankCounter_validCountersStillCreatedAlongsideEmptyNames() {
        // Given: a scenario with one real counter and one empty-string counter name
        val scenarioId = 1L
        val realCounter = "MyCounter"
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(1L, scenarioId)
            db.insertTestAction(1L, 1L, type = ActionType.CHANGE_COUNTER, counterName = realCounter)
            db.insertTestAction(2L, 1L, type = ActionType.CHANGE_COUNTER, counterName = "")
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then: only the real counter exists, no blank one
            db.assertScenarioHasCounters(scenarioId, setOf(realCounter))
        }
    }

    @Test
    fun migrate_counters_creation_mixed() {
        // Given
        val scenarioId1 = 1L
        val scenarioId2 = 2L
        val sharedCounter = "SharedCounter"
        val unique1 = "Counter1"
        val unique2 = "Counter2"

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId1)
            db.insertTestScenario(scenarioId2)
            db.insertTestEvent(1L, scenarioId1)
            db.insertTestEvent(2L, scenarioId2)

            // Scenario 1
            db.insertTestAction(1L, 1L, type = ActionType.CHANGE_COUNTER, counterName = sharedCounter)
            db.insertTestCondition(1L, 1L, type = ConditionType.ON_COUNTER_REACHED, counterName = unique1)

            // Scenario 2
            db.insertTestAction(2L, 2L, type = ActionType.CHANGE_COUNTER, counterName = sharedCounter)
            db.insertTestCondition(2L, 2L, type = ConditionType.ON_NUMBER_DETECTED, numberCounterValueCounterName = unique2)
        }

        // When
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true, Migration19to20).use { db ->
            // Then
            db.assertScenarioHasCounters(scenarioId1, setOf(sharedCounter, unique1))
            db.assertScenarioHasCounters(scenarioId2, setOf(sharedCounter, unique2))

            // Total counters should be 4
            db.query("SELECT COUNT(*) FROM $COUNTERS_TABLE").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(4, cursor.getInt(0))
            }
        }
    }

    private fun SupportSQLiteDatabase.insertTestScenario(id: Long) {
        insert(SCENARIO_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("name", "Scenario $id")
            put("detection_quality", 1)
            put("frame_limit", 0)
            put("randomize", 0)
            put("keep_screen_on", 0)
        })
    }

    private fun SupportSQLiteDatabase.insertTestEvent(id: Long, scenarioId: Long) {
        insert(EVENT_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("scenario_id", scenarioId)
            put("name", "Event $id")
            put("operator", 0)
            put("priority", 0)
            put("enabled_on_start", 1)
            put("type", "IMAGE_EVENT")
        })
    }

    private fun SupportSQLiteDatabase.insertTestAction(
        id: Long,
        eventId: Long,
        type: ActionType,
        counterName: String? = null,
        counterOperationValue: Int? = null,
        counterOperationCounterName: String? = null,
        notificationMessageCounterName: String? = null,
        notificationMessageType: NotificationMessageType? =
            if (notificationMessageCounterName != null) NotificationMessageType.COUNTER_VALUE else null,
    ) {
        insert(ACTION_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("eventId", eventId)
            put("priority", 0)
            put("name", "Action $id")
            put("type", type.name)
            put("counter_name", counterName)
            put("counter_operation_value", counterOperationValue)
            put("counter_operation_counter_name", counterOperationCounterName)
            put("notification_message_type", notificationMessageType?.name)
            put("notification_message_counter_name", notificationMessageCounterName)
        })
    }

    private fun SupportSQLiteDatabase.insertTestCondition(
        id: Long,
        eventId: Long,
        type: ConditionType? = null,
        counterName: String? = null,
        counterValue: Int? = null,
        counterValueCounterName: String? = null,
        numberCounterValueCounterName: String? = null,
    ) {
        insert(CONDITION_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("eventId", eventId)
            put("name", "Condition $id")
            put("type", type?.name ?: ConditionType.ON_IMAGE_DETECTED.name)
            put("priority", 0)
            put("counter_name", counterName)
            put("counter_value", counterValue)
            put("counter_value_counter_name", counterValueCounterName)
            put("number_counter_value_counter_name", numberCounterValueCounterName)
        })
    }

    private fun SupportSQLiteDatabase.assertScenarioHasCounters(scenarioId: Long, expectedCounters: Set<String>) {
        query("SELECT counterName, startingValue FROM $COUNTERS_TABLE WHERE scenarioId = $scenarioId").use { cursor ->
            val actualCounters = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                actualCounters.add(name)
                assertEquals(0.0, cursor.getDouble(1), 0.0)
            }
            assertEquals(expectedCounters, actualCounters)
        }
    }
}
