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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Migration from database v18 to v19.
 *
 * Klick'r 4.0.0 release migration:
 * * Counter values are now stored as REAL (Double)
 * * Counter are now stored in database (table has been created during previous migration, but it is empty)
 * * Notification Action behave like set text now, counter columns have been deleted.
 * * Scenario frame limit is now a REAL and renamed compute_rate (FPS to FPMinutes)
 */
object Migration19to20 : Migration(19, 20) {

    private val conditionIdColumn = SQLiteColumn.PrimaryKey()
    private val conditionTypeColumn = SQLiteColumn.Text("type")
    private val conditionCounterValuesOldColumn = SQLiteColumn.Int("counter_value", isNotNull = false)
    private val conditionCounterValuesNewColumn = SQLiteColumn.Double("counter_value", isNotNull = false)

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionTypeColumn = SQLiteColumn.Text("type")
    private val actionCounterValueOldColumn = SQLiteColumn.Int("counter_operation_value", isNotNull = false)
    private val actionCounterValueNewColumn = SQLiteColumn.Double("counter_operation_value", isNotNull = false)

    private val notificationMessageTypeColumn = SQLiteColumn.Text("notification_message_type", isNotNull = false)
    private val notificationMessageCounterNameColumn = SQLiteColumn.Text("notification_message_counter_name", isNotNull = false)
    private val notificationMessageTextColumn = SQLiteColumn.Text("notification_message_text", isNotNull = false)

    private val scenarioIdColumn = SQLiteColumn.PrimaryKey()
    private val scenarioFrameLimitColumn = SQLiteColumn.Int("frame_limit")
    private val scenarioComputeRateColumn = SQLiteColumn.Double("compute_rate", defaultValue = "0.0")


    override fun migrate(db: SupportSQLiteDatabase) {
        // Migrate counters types from Int to Double
        db.migrateConditionsCounterValueType()
        db.migrateActionsCounterValueType()

        // Populate counters table with all existing counters
        db.populateCountersTable()

        // Rework notification actions to behave like set text
        db.migrateNotificationActions()

        // Migrate scenario frame limit
        db.migrateScenarioFrameLimit()
    }

    private fun SupportSQLiteDatabase.migrateConditionsCounterValueType() {
        getSQLiteTableReference(CONDITION_TABLE).apply {
            // Get the current counter reached condition counter values
            val counterReachedValues = buildMap {
                forEachCounterReachedCondition { id, type, counterValue ->
                    if (id == null || type == null || counterValue == null) return@forEachCounterReachedCondition
                    if (ConditionType.valueOf(type.uppercase()) == ConditionType.ON_COUNTER_REACHED) put(id, counterValue)
                }
            }

            // Remove the int version and recreate it with Double version
            alterTableDropColumn(setOf(conditionCounterValuesOldColumn.name))
            alterTableAddColumn(conditionCounterValuesNewColumn)

            // Restore the values
            counterReachedValues.forEach { (conditionId, oldCounterValue) ->
                restoreConditionCounterValue(conditionId, oldCounterValue.toDouble())
            }
        }
    }

    private fun SupportSQLiteDatabase.migrateActionsCounterValueType() {
        getSQLiteTableReference(ACTION_TABLE).apply {
            // Get the current counter action values
            val counterValues = buildMap {
                forEachChangeCounterAction { id, type, counterValue ->
                    if (id == null || type == null || counterValue == null) return@forEachChangeCounterAction
                    if (ActionType.valueOf(type.uppercase()) == ActionType.CHANGE_COUNTER) put(id, counterValue)
                }
            }

            // Remove the int version and recreate it with Double version
            alterTableDropColumn(setOf(actionCounterValueOldColumn.name))
            alterTableAddColumn(actionCounterValueNewColumn)

            // Restore the values
            counterValues.forEach { (conditionId, oldCounterValue) ->
                restoreActionCounterValue(conditionId, oldCounterValue.toDouble())
            }
        }
    }

    private fun SupportSQLiteDatabase.populateCountersTable() {
        val countersFound = mutableSetOf<Pair<String, Long>>()
        getSQLiteTableReference(ACTION_TABLE).apply {
            forEachActionCounterMention { sourceVal1, sourceVal2, sourceVal3, scenarioId ->
                if (scenarioId == null) return@forEachActionCounterMention
                sourceVal1?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
                sourceVal2?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
                sourceVal3?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
            }
        }
        getSQLiteTableReference(CONDITION_TABLE).apply {
            forEachConditionsCounterMention { sourceVal1, sourceVal2, sourceVal3, scenarioId ->
                if (scenarioId == null) return@forEachConditionsCounterMention
                sourceVal1?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
                sourceVal2?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
                sourceVal3?.takeIf { it.isNotBlank() }?.let { countersFound.add(it to scenarioId) }
            }
        }

        // Insert into counters_table
        getSQLiteTableReference(COUNTERS_TABLE).apply {
            countersFound.forEach { (name, scenarioId) ->
                insertIntoValues(
                    ContentValues().apply {
                        put("counterName", name)
                        put("scenarioId", scenarioId)
                        put("startingValue", 0)
                    }
                )
            }
        }
    }

    private fun SupportSQLiteDatabase.migrateNotificationActions() {
        getSQLiteTableReference(ACTION_TABLE).apply {

            // First, move the counter name into the text using counter placeholders
            forEachNotificationActionWithCounter { id, _, _, counterName ->
                if (id == null || counterName == null) return@forEachNotificationActionWithCounter
                updateNotificationCounterText(
                    actionId = id,
                    text = "$counterName = ".appendCounterReference(counterName)
                )
            }

            // Then, remove counter related columns
            alterTableDropColumn(
                setOf(
                    notificationMessageTypeColumn.name,
                    notificationMessageCounterNameColumn.name,
                )
            )
        }
    }

    private fun SupportSQLiteDatabase.migrateScenarioFrameLimit() {
        getSQLiteTableReference(SCENARIO_TABLE).apply {
            // Get the current frame limit values
            val frameLimits = buildMap {
                forEachScenario { id, frameLimit ->
                    if (id == null || frameLimit == null) return@forEachScenario
                    put(id, frameLimit)
                }
            }

            // Remove the int version and recreate it with Double version
            alterTableDropColumn(setOf(scenarioFrameLimitColumn.name))
            alterTableAddColumn(scenarioComputeRateColumn)

            // Restore the values
            frameLimits.forEach { (scenarioId, frameLimit) ->
                restoreFrameLimitValue(scenarioId, frameLimit.toDouble())
            }
        }
    }

    private fun SQLiteTable.forEachCounterReachedCondition(closure: (Long?, String?, Int?) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ConditionType.ON_COUNTER_REACHED}\"",
            columnA = conditionIdColumn,
            columnB = conditionTypeColumn,
            columnC = conditionCounterValuesOldColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.forEachChangeCounterAction(closure: (Long?, String?, Int?) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ActionType.CHANGE_COUNTER}\"",
            columnA = actionIdColumn,
            columnB = actionTypeColumn,
            columnC = actionCounterValueOldColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.forEachActionCounterMention(closure: (String?, String?, String?, Long?) -> Unit) {
        forEachRow(
            distinct = true,
            extraClause = """
                AS actions JOIN $EVENT_TABLE AS events
                ON actions.eventId = events.id
                WHERE (actions.counter_name IS NOT NULL AND length(trim(actions.counter_name)) > 0)
                OR (actions.counter_operation_counter_name IS NOT NULL AND length(trim(actions.counter_operation_counter_name)) > 0)
                OR (actions.notification_message_counter_name IS NOT NULL AND length(trim(actions.notification_message_counter_name)) > 0)
            """.trimIndent(),
            columnA = SQLiteColumn.Text("actions.counter_name"),
            columnB = SQLiteColumn.Text("actions.counter_operation_counter_name"),
            columnC = SQLiteColumn.Text("actions.notification_message_counter_name"),
            columnD = SQLiteColumn.Long("events.scenario_id"),
            closure = closure,
        )
    }

    private fun SQLiteTable.forEachConditionsCounterMention(closure: (String?, String?, String?, Long?) -> Unit) {
        forEachRow(
            distinct = true,
            extraClause = """
                AS conditions JOIN $EVENT_TABLE AS events
                ON conditions.eventId = events.id
                WHERE (conditions.counter_name IS NOT NULL AND length(trim(conditions.counter_name)) > 0)
                OR (conditions.counter_value_counter_name IS NOT NULL AND length(trim(conditions.counter_value_counter_name)) > 0)
                OR (conditions.number_counter_value_counter_name IS NOT NULL AND length(trim(conditions.number_counter_value_counter_name)) > 0)
            """.trimIndent(),
            columnA = SQLiteColumn.Text("conditions.counter_name"),
            columnB = SQLiteColumn.Text("conditions.counter_value_counter_name"),
            columnC = SQLiteColumn.Text("conditions.number_counter_value_counter_name"),
            columnD = SQLiteColumn.Long("events.scenario_id"),
            closure = closure,
        )
    }

    private fun SQLiteTable.forEachNotificationActionWithCounter(closure: (Long?, String?, String?, String?) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ActionType.NOTIFICATION}\" AND `${notificationMessageTypeColumn.name}` = \"${NotificationMessageType.COUNTER_VALUE}\"",
            columnA = actionIdColumn,
            columnB = actionTypeColumn,
            columnC = notificationMessageTypeColumn,
            columnD = notificationMessageCounterNameColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.forEachScenario(closure: (Long?, Int?) -> Unit) {
        forEachRow(
            columnA = scenarioIdColumn,
            columnB = scenarioFrameLimitColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.restoreConditionCounterValue(conditionId: Long, counterValue: Double) = update(
        extraClause = "WHERE `id` = $conditionId",
        contentValues = ContentValues().apply {
            put(conditionCounterValuesNewColumn.name, counterValue)
        },
    )

    private fun SQLiteTable.restoreActionCounterValue(actionId: Long, counterValue: Double) = update(
        extraClause = "WHERE `id` = $actionId",
        contentValues = ContentValues().apply {
            put(actionCounterValueNewColumn.name, counterValue)
        },
    )

    private fun SQLiteTable.updateNotificationCounterText(actionId: Long, text: String) = update(
        extraClause = "WHERE `id` = $actionId",
        contentValues = ContentValues().apply {
            put(notificationMessageTextColumn.name, text)
        },
    )

    private fun SQLiteTable.restoreFrameLimitValue(scenarioId: Long, computeRate: Double) = update(
        extraClause = "WHERE `id` = $scenarioId",
        contentValues = ContentValues().apply {
            put(scenarioComputeRateColumn.name, computeRate)
        },
    )
}
