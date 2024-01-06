/*
 * Copyright (C) 2024 Kevin Buzeau
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
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.database.utils.V12Condition
import com.buzbuz.smartautoclicker.core.database.utils.V12EndCondition
import com.buzbuz.smartautoclicker.core.database.utils.V12Event
import com.buzbuz.smartautoclicker.core.database.utils.V12Scenario
import com.buzbuz.smartautoclicker.core.database.utils.V12ToggleEvent
import com.buzbuz.smartautoclicker.core.database.utils.getV13ChangeCounterActions
import com.buzbuz.smartautoclicker.core.database.utils.getV13Conditions
import com.buzbuz.smartautoclicker.core.database.utils.getV13CounterReachedConditions
import com.buzbuz.smartautoclicker.core.database.utils.getV13EventToggle
import com.buzbuz.smartautoclicker.core.database.utils.getV13Events
import com.buzbuz.smartautoclicker.core.database.utils.getV13ToggleEventsActions
import com.buzbuz.smartautoclicker.core.database.utils.getV13TriggerEvents
import com.buzbuz.smartautoclicker.core.database.utils.insertV12Condition
import com.buzbuz.smartautoclicker.core.database.utils.insertV12EndCondition
import com.buzbuz.smartautoclicker.core.database.utils.insertV12Event
import com.buzbuz.smartautoclicker.core.database.utils.insertV12Scenario
import com.buzbuz.smartautoclicker.core.database.utils.insertV12ToggleEvent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration12to13]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration12to13Tests {

    private companion object {
        private const val TEST_DB = "migration-test"

        private const val OLD_DB_VERSION = 12
        private const val NEW_DB_VERSION = 13
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrate_events() {
        // Given
        val event = V12Event(id = 12L, scenarioId = 2L)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Event(event)
        }

        // Migrate to v13 and verify
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            dbV13.query(getV13Events()).use { cursor ->
                cursor.moveToFirst()

                assertEquals(1, cursor.count)
                assertEquals(event.id, cursor.getLong(cursor.getColumnIndex("id")))
                assertEquals(event.scenarioId, cursor.getLong(cursor.getColumnIndex("scenario_id")))
                assertEquals(event.name, cursor.getString(cursor.getColumnIndex("name")))
                assertEquals(event.operator, cursor.getInt(cursor.getColumnIndex("operator")))
                assertEquals(event.enabledOnStart, cursor.getInt(cursor.getColumnIndex("enabled_on_start")) == 1)
                assertEquals(EventType.IMAGE_EVENT.name, cursor.getString(cursor.getColumnIndex("type")))
            }
        }
    }

    @Test
    fun migrate_conditions() {
        // Given
        val eventId = 2L
        val condition = V12Condition(id = 12L, eventId = eventId)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Event(V12Event(id = eventId, scenarioId = 1L))
            dbV12.insertV12Condition(condition)
        }

        // Migrate to v13 and verify
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            dbV13.query(getV13Conditions()).use { cursor ->
                cursor.moveToFirst()

                assertEquals(1, cursor.count)
                assertEquals(condition.id, cursor.getLong(cursor.getColumnIndex("id")))
                assertEquals(condition.eventId, cursor.getLong(cursor.getColumnIndex("eventId")))
                assertEquals(condition.name, cursor.getString(cursor.getColumnIndex("name")))
                assertEquals(condition.path, cursor.getString(cursor.getColumnIndex("path")))
                assertEquals(condition.area.left, cursor.getInt(cursor.getColumnIndex("area_left")))
                assertEquals(condition.area.top, cursor.getInt(cursor.getColumnIndex("area_top")))
                assertEquals(condition.area.right, cursor.getInt(cursor.getColumnIndex("area_right")))
                assertEquals(condition.area.bottom, cursor.getInt(cursor.getColumnIndex("area_bottom")))
                assertEquals(ConditionType.ON_IMAGE_DETECTED.name, cursor.getString(cursor.getColumnIndex("type")))
            }
        }
    }

    @Test
    fun migrate_toggle_event_actions_no_aggregation() {
        // Given
        val eventId = 2L
        val toggleEventAction = V12ToggleEvent(id = 12L, eventId = eventId)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Event(V12Event(id = eventId, scenarioId = 1L))
            dbV12.insertV12ToggleEvent(toggleEventAction)
        }

        // Migrate to v13 and verify
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            assertAggregation(dbV13, expectedAggregator = toggleEventAction, aggregated = listOf(toggleEventAction))
        }
    }

    @Test
    fun migrate_toggle_event_actions_aggregation_one_event() {
        // Given
        val eventId1 = 2L
        val eventId2 = 3L
        val eventId3 = 4L
        val toggleEventAction1 = V12ToggleEvent(id = 12L, eventId = eventId1, toggleEventId = eventId1, priority = 0)
        val toggleEventAction2 = V12ToggleEvent(id = 13L, eventId = eventId1, toggleEventId = eventId2, priority = 1)
        val toggleEventAction3 = V12ToggleEvent(id = 14L, eventId = eventId1, toggleEventId = eventId3, priority = 4)
        val expectedActions = listOf(toggleEventAction1, toggleEventAction2, toggleEventAction3)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Event(V12Event(id = eventId1, scenarioId = 1L))
            dbV12.insertV12Event(V12Event(id = eventId2, scenarioId = 1L))
            dbV12.insertV12Event(V12Event(id = eventId3, scenarioId = 1L))
            dbV12.insertV12ToggleEvent(toggleEventAction1)
            dbV12.insertV12ToggleEvent(toggleEventAction2)
            dbV12.insertV12ToggleEvent(toggleEventAction3)
        }

        // Migrate to v13 and verify
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            assertAggregation(dbV13, expectedAggregator = toggleEventAction1, aggregated = expectedActions)
        }
    }

    @Test
    fun migrate_toggle_event_actions_aggregation_multiple_events() {
        // Given
        val eventId1 = 2L
        val eventId2 = 3L
        val eventId3 = 4L
        val toggleEventAction11 = V12ToggleEvent(id = 12L, eventId = eventId1, toggleEventId = eventId1, priority = 0)
        val toggleEventAction12 = V12ToggleEvent(id = 13L, eventId = eventId1, toggleEventId = eventId2, priority = 1)
        val toggleEventAction13 = V12ToggleEvent(id = 14L, eventId = eventId1, toggleEventId = eventId3, priority = 4)
        val expectedActionsEvent1 = listOf(toggleEventAction11, toggleEventAction12, toggleEventAction13)
        val toggleEventAction21 = V12ToggleEvent(id = 21L, eventId = eventId2, toggleEventId = eventId3, priority = 2)
        val toggleEventAction22 = V12ToggleEvent(id = 31L, eventId = eventId2, toggleEventId = eventId2, priority = 4)
        val toggleEventAction23 = V12ToggleEvent(id = 41L, eventId = eventId2, toggleEventId = eventId1, priority = 8)
        val expectedActionsEvent2 = listOf(toggleEventAction21, toggleEventAction22, toggleEventAction23)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Event(V12Event(id = eventId1, scenarioId = 1L))
            dbV12.insertV12Event(V12Event(id = eventId2, scenarioId = 1L))
            dbV12.insertV12Event(V12Event(id = eventId3, scenarioId = 1L))
            expectedActionsEvent1.forEach(dbV12::insertV12ToggleEvent)
            expectedActionsEvent2.forEach(dbV12::insertV12ToggleEvent)
        }

        // Migrate to v13 and verify
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            assertAggregation(dbV13, eventId = eventId1, expectedAggregator = toggleEventAction11, aggregated = expectedActionsEvent1)
            assertAggregation(dbV13, eventId = eventId2, expectedAggregator = toggleEventAction21, aggregated = expectedActionsEvent2)
        }
    }

    @Test
    fun migrate_end_conditions_one() {
        // Given
        val scenarioId = 1L
        val eventId = 42L
        val scenario = V12Scenario(id = scenarioId)
        val event = V12Event(id = eventId, scenarioId = scenarioId)
        val endCondition = V12EndCondition(id = 11L, scenarioId = scenarioId, eventId = eventId)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Scenario(scenario)
            dbV12.insertV12Event(event)
            dbV12.insertV12EndCondition(endCondition)
        }

        // Migrate to v13
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            // Verify new trigger event with stop scenario action
            val endEventId = assertStopScenarioEvent(dbV13, scenario)
            // Verify counter to replacing end condition
            assertStopScenarioCounter(dbV13, endEventId, endCondition)
        }
    }

    @Test
    fun migrate_end_conditions_multiple() {
        // Given
        val scenarioId = 1L
        val eventId1 = 41L
        val eventId2 = 42L
        val eventId3 = 43L
        val scenario = V12Scenario(id = scenarioId)
        val event1 = V12Event(id = eventId1, scenarioId = scenarioId)
        val event2 = V12Event(id = eventId2, scenarioId = scenarioId)
        val event3 = V12Event(id = eventId3, scenarioId = scenarioId)
        val endCondition1 = V12EndCondition(id = 11L, scenarioId = scenarioId, eventId = eventId1, executions = 12)
        val endCondition2 = V12EndCondition(id = 12L, scenarioId = scenarioId, eventId = eventId2, executions = 1)
        val endCondition3 = V12EndCondition(id = 13L, scenarioId = scenarioId, eventId = eventId3, executions = 18)
        val endConditions = listOf(endCondition1, endCondition2, endCondition3)

        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->
            dbV12.insertV12Scenario(scenario)
            dbV12.insertV12Event(event1)
            dbV12.insertV12Event(event2)
            dbV12.insertV12Event(event3)
            dbV12.insertV12EndCondition(endCondition1)
            dbV12.insertV12EndCondition(endCondition2)
            dbV12.insertV12EndCondition(endCondition3)
        }

        // Migrate to v13
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->
            // Verify new trigger event with stop scenario action
            val endEventId = assertStopScenarioEvent(dbV13, scenario)

            // Verify counter to replacing end condition
            endConditions.forEach { endCondition ->
                assertStopScenarioCounter(dbV13, endEventId, endCondition)
            }
        }
    }

    private fun assertAggregation(
        database: SupportSQLiteDatabase,
        eventId: Long? = null,
        expectedAggregator: V12ToggleEvent,
        aggregated: List<V12ToggleEvent>,
    ) {
        // Verify aggregation, it should remain only one action. Verify copied values and new columns values
        val sqlQuery = if (eventId != null) getV13ToggleEventsActions(eventId) else getV13ToggleEventsActions()
        database.query(sqlQuery).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)

            assertEquals(expectedAggregator.id, cursor.getLong(cursor.getColumnIndex("id")))
            assertEquals(expectedAggregator.eventId, cursor.getLong(cursor.getColumnIndex("eventId")))
            assertEquals(expectedAggregator.name, cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(expectedAggregator.priority, cursor.getInt(cursor.getColumnIndex("priority")))
            assertEquals(expectedAggregator.type.name, cursor.getString(cursor.getColumnIndex("type")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndex("toggle_all")))
            assertTrue(cursor.isNull(cursor.getColumnIndex("toggle_all_type")))
        }

        // Verify created event toggle referencing the event and action. One event toggle per action
        database.query(getV13EventToggle(expectedAggregator.id)).use { cursor ->
            cursor.moveToFirst()
            assertEquals(aggregated.size, cursor.count)

            aggregated.forEach { expectedAction ->
                assertEquals(expectedAggregator.id, cursor.getLong(cursor.getColumnIndex("action_id")))
                assertEquals(expectedAction.toggleEventId, cursor.getLong(cursor.getColumnIndex("toggle_event_id")))
                assertEquals(expectedAction.toggleType.name, cursor.getString(cursor.getColumnIndex("toggle_type")))
                cursor.moveToNext()
            }
        }
    }

    private fun assertStopScenarioEvent(
        database: SupportSQLiteDatabase,
        scenario: V12Scenario,
    ): Long {
        database.query(getV13TriggerEvents()).use { cursor ->
            cursor.moveToFirst()

            assertEquals(1, cursor.count)

            assertEquals(scenario.id, cursor.getLong(cursor.getColumnIndex("scenario_id")))
            assertEquals("Stop scenario", cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(EventType.TRIGGER_EVENT.name, cursor.getString(cursor.getColumnIndex("type")))
            assertEquals(scenario.endConditionOperator, cursor.getInt(cursor.getColumnIndex("operator")))
            assertEquals(-1, cursor.getInt(cursor.getColumnIndex("priority")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndex("enabled_on_start")))

            val endEventId = cursor.getLong(cursor.getColumnIndex("id"))
            database.query(getV13ToggleEventsActions(endEventId)).use { actionsCursor ->
                actionsCursor.moveToFirst()

                assertEquals(1, actionsCursor.count)

                assertEquals(endEventId, actionsCursor.getLong(actionsCursor.getColumnIndex("eventId")))
                assertEquals("Stop scenario", actionsCursor.getString(actionsCursor.getColumnIndex("name")))
                assertEquals(0, actionsCursor.getInt(actionsCursor.getColumnIndex("priority")))
                assertEquals(ActionType.TOGGLE_EVENT.name, actionsCursor.getString(actionsCursor.getColumnIndex("type")))
                assertEquals(1, actionsCursor.getInt(actionsCursor.getColumnIndex("toggle_all")))
                assertEquals(EventToggleType.DISABLE.name, actionsCursor.getString(actionsCursor.getColumnIndex("toggle_all_type")))
            }

            return endEventId
        }
    }

    private fun assertStopScenarioCounter(
        database: SupportSQLiteDatabase,
        stopScenarioEventId: Long,
        expectedEndCondition: V12EndCondition,
    ) {

        val endConditionCounterName = "\"Stop Scenario ${expectedEndCondition.eventId}\""

        database.query(getV13ChangeCounterActions(expectedEndCondition.eventId)).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)

            assertEquals(expectedEndCondition.eventId, cursor.getLong(cursor.getColumnIndex("eventId")))
            assertEquals("Execution count", cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(10000, cursor.getInt(cursor.getColumnIndex("priority")))
            assertEquals(ActionType.CHANGE_COUNTER.name, cursor.getString(cursor.getColumnIndex("type")))
            assertEquals(endConditionCounterName, cursor.getString(cursor.getColumnIndex("counter_name")))
            assertEquals(ChangeCounterOperationType.ADD.name, cursor.getString(cursor.getColumnIndex("counter_operation")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndex("counter_operation_value")))

        }

        database.query(getV13CounterReachedConditions(stopScenarioEventId, expectedEndCondition.executions)).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)

            assertEquals(stopScenarioEventId, cursor.getLong(cursor.getColumnIndex("eventId")))
            assertEquals(endConditionCounterName, cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(ConditionType.ON_COUNTER_REACHED.name, cursor.getString(cursor.getColumnIndex("type")))
            assertEquals(endConditionCounterName, cursor.getString(cursor.getColumnIndex("counter_name")))
            assertEquals(CounterComparisonOperation.GREATER_OR_EQUALS.name, cursor.getString(cursor.getColumnIndex("counter_comparison_operation")))
            assertEquals(expectedEndCondition.executions, cursor.getInt(cursor.getColumnIndex("counter_value")))
        }
    }
}