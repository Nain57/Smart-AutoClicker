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

import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.sqlite.copyColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.forEachRow
import com.buzbuz.smartautoclicker.core.base.sqlite.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.END_CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TOGGLE_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.EventType

/**
 * Migration from database v12 to v13.
 *
 *
 */
object Migration12to13 : Migration(12, 13) {

    private val scenarioEndConditionOperatorColumn =
        SQLiteColumn.Default("end_condition_operator", Int::class)

    private val evtToggleToggleTypeColumn =
        SQLiteColumn.Default("toggle_type", String::class)
    private val evtToggleActionIdColumn = SQLiteColumn.ForeignKey(
        name = "action_id", type = Long::class,
        referencedTable = ACTION_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )
    private val evtToggleToggleEventIdColumn = SQLiteColumn.ForeignKey(
        name = "toggle_event_id", type = Long::class,
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    private val actionOldToggleEventIdColumn =
        SQLiteColumn.Default("toggle_event_id", Long::class, isNotNull = false)
    private val actionOldToggleTypeColumn =
        SQLiteColumn.Default("toggle_type", String::class, isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            val eventTable = migrateEventTable()
            val conditionTable = migrateConditionTable()
            val eventToggleTable = migrateEventToggleTable()
            val actionTable = migrateActionTable(
                postEventToggleTable = eventToggleTable,
            )

            migrateEndConditionsTable(
                preScenarioTable = getSQLiteTableReference(SCENARIO_TABLE),
                postEventTable = eventTable,
                postConditionTable = conditionTable,
                postActionTable = actionTable,
            )

            migrateScenarioTable()
        }
    }

    private fun SupportSQLiteDatabase.migrateScenarioTable() = getSQLiteTableReference(SCENARIO_TABLE).apply {
        alterTableDropColumn(setOf(scenarioEndConditionOperatorColumn.name))
    }

    private fun SupportSQLiteDatabase.migrateEventTable() = getSQLiteTableReference(EVENT_TABLE).apply {
        alterTableAddColumn(SQLiteColumn.Default("type", String::class, defaultValue = EventType.IMAGE_EVENT.name))
    }

    private fun SupportSQLiteDatabase.migrateConditionTable() = getSQLiteTableReference(CONDITION_TABLE).apply {
        val changedColumns = setOf(
            SQLiteColumn.Default("path", String::class, isNotNull = false),
            SQLiteColumn.Default("area_left", Int::class, isNotNull = false),
            SQLiteColumn.Default("area_top", Int::class, isNotNull = false),
            SQLiteColumn.Default("area_right", Int::class, isNotNull = false),
            SQLiteColumn.Default("area_bottom", Int::class, isNotNull = false),
            SQLiteColumn.Default("threshold", Int::class, isNotNull = false),
            SQLiteColumn.Default("detection_type", Int::class, isNotNull = false),
            SQLiteColumn.Default("shouldBeDetected", Boolean::class, isNotNull = false),
            SQLiteColumn.Default("detection_area_left", Int::class, isNotNull = false),
            SQLiteColumn.Default("detection_area_top", Int::class, isNotNull = false),
            SQLiteColumn.Default("detection_area_right", Int::class, isNotNull = false),
            SQLiteColumn.Default("detection_area_bottom", Int::class, isNotNull = false),
        )

        // Create temp condition table stripped from changed columns
        val (newConditionTable, indexes) = copyTable(droppedColumns = changedColumns.map { it.name })
        // Add new columns
        newConditionTable.alterTableAddColumns(
            setOf(
                SQLiteColumn.Default("type", String::class, defaultValue = ConditionType.ON_IMAGE_DETECTED.name),
                SQLiteColumn.Default("broadcast_action", String::class, isNotNull = false),
                SQLiteColumn.Default("counter_name", String::class, isNotNull = false),
                SQLiteColumn.Default("counter_comparison_operation", String::class, isNotNull = false),
                SQLiteColumn.Default("counter_value", Int::class, isNotNull = false),
                SQLiteColumn.Default("timer_value_ms", Long::class, isNotNull = false),
            )
        )
        // Add filtered old columns as nullable
        newConditionTable.alterTableAddColumns(changedColumns)
        // Copy transformed nullable columns content from table to temp_table
        newConditionTable.insertIntoSelect(
            fromTableName = CONDITION_TABLE,
            columnsToFromColumns = changedColumns.map { copyColumn(it.name) }.toTypedArray()
        )
        // Remove old condition table
        dropTable()
        // Recreate indexes
        indexes.forEach { execSQLite(it) }
        // Rename temp table to actual condition table name
        newConditionTable.alterTableRename(CONDITION_TABLE)
    }

    private fun SupportSQLiteDatabase.migrateEventToggleTable() = getSQLiteTableReference(EVENT_TOGGLE_TABLE).apply {
        createTable(
            columns = setOf(
                evtToggleActionIdColumn,
                evtToggleToggleTypeColumn,
                evtToggleToggleEventIdColumn,
            )
        )
        createIndex(evtToggleActionIdColumn)
        createIndex(evtToggleToggleEventIdColumn)
    }

    private fun SupportSQLiteDatabase.migrateActionTable(postEventToggleTable: SQLiteTable) =
        getSQLiteTableReference(ACTION_TABLE).apply {
            alterTableAddColumns(
                setOf(
                    SQLiteColumn.Default("counter_name", String::class, isNotNull = false),
                    SQLiteColumn.Default("counter_operation", String::class, isNotNull = false),
                    SQLiteColumn.Default("counter_operation_value", Int::class, isNotNull = false),
                    SQLiteColumn.Default("toggle_all", Boolean::class, isNotNull = false),
                    SQLiteColumn.Default("toggle_all_type", String::class, isNotNull = false),
                )
            )

            val flaggedForDeletion = mutableSetOf<Long>() // All actions aggregated by another one will be deleted.
            var currentAggregatorId: Long // Id of the action used to regroup the other actions in the same event
            val aggregation = mutableMapOf<Long, Long>() // Key is the event id, Value the aggregator action id.
            forEachToggleEventAction { id, eventId, toggleEventId, toggleType ->

                // If that's the first action from the event, use it as aggregator
                // Otherwise, use the one available
                currentAggregatorId = aggregation.getOrPut(eventId) {
                    // As we will keep this action, set it as manual
                    updateWithNames(extraClause = "`id` = $id", "toggle_all" to "0",)
                    id
                }

                // For each toggle action, creates it's counterpart as a EventToggle referencing the aggregator
                postEventToggleTable.insertIntoValues(
                    evtToggleActionIdColumn.name to currentAggregatorId.toString(),
                    evtToggleToggleTypeColumn.name to toggleType,
                    evtToggleToggleEventIdColumn.name to toggleEventId.toString(),
                )

                // If this is not the aggregator, it is now aggregated and useless
                if (id != currentAggregatorId) flaggedForDeletion.add(id)
            }

            // Remove all aggregated actions
            if (flaggedForDeletion.isNotEmpty()) deleteFrom(flaggedForDeletion)
            // Remove old unused columns from actions
            alterTableDropColumn(
                setOf(actionOldToggleTypeColumn.name, actionOldToggleEventIdColumn.name)
            )
        }

    private fun SupportSQLiteDatabase.migrateEndConditionsTable(
        preScenarioTable: SQLiteTable,
        postEventTable: SQLiteTable,
        postActionTable: SQLiteTable,
        postConditionTable: SQLiteTable,
    ) = getSQLiteTableReference(END_CONDITION_TABLE).apply {

            preScenarioTable.forEachScenarioWithEndCondition { scenarioId, endConditionOperator ->
                // For each scenario with end conditions, create a trigger event that will stop the scenario
                val endConditionEventId = postEventTable.insertIntoValues(
                    "scenario_id" to scenarioId.toString(),
                    "name" to "\"Stop scenario\"",
                    "type" to "\"${EventType.TRIGGER_EVENT}\"",
                    "operator" to endConditionOperator.toString(),
                    "priority" to "-1",
                    "enabled_on_start" to "1",
                )

                // Add the action that end the scenario
                postActionTable.insertIntoValues(
                    "eventId" to endConditionEventId.toString(),
                    "priority" to "0",
                    "name" to "\"Stop scenario\"",
                    "type" to "\"${ActionType.TOGGLE_EVENT}\"",
                    "toggle_all" to "1",
                    "toggle_all_type" to "\"${EventToggleType.DISABLE}\"",
                )

                // For each end condition in this scenario, creates a counter reached condition and a change counter action
                forEachEndCondition(scenarioId) { targetEventId, executions ->
                    val endConditionCounterName = "\"Stop Scenario $targetEventId\""

                    // First, create a counter action for this event
                    postActionTable.insertIntoValues(
                        "eventId" to targetEventId.toString(),
                        "priority" to "10000",
                        "name" to "Execution count",
                        "type" to "\"${ActionType.CHANGE_COUNTER}\"",
                        "counter_name" to endConditionCounterName,
                        "counter_operation" to "\"${ChangeCounterOperationType.ADD}\"",
                        "counter_operation_value" to "1",
                    )

                    // Then, create a condition in our end condition trigger event
                    postConditionTable.insertIntoValues(
                        "eventId" to endConditionEventId.toString(),
                        "name" to endConditionCounterName,
                        "type" to "\"${ConditionType.ON_COUNTER_REACHED}\"",
                        "counter_name" to endConditionCounterName,
                        "counter_comparison_operation" to "\"${CounterComparisonOperation.GREATER_OR_EQUALS}\"",
                        "counter_value" to executions.toString(),
                    )
                }
            }

            dropTable()
        }

    private fun SQLiteTable.forEachToggleEventAction(closure: (Long, Long, Long, String) -> Unit) =
        forEachRow(
            extraClause = "WHERE `type` =\"${ActionType.TOGGLE_EVENT}\" ORDER BY `priority` ASC",
            SQLiteColumn.PrimaryKey("id"),
            SQLiteColumn.Default("eventId", type = Long::class),
            actionOldToggleEventIdColumn, actionOldToggleTypeColumn, closure,
        )

    private fun SQLiteTable.forEachScenarioWithEndCondition(closure: (Long, Int) -> Unit) =
        forEachRow(
            extraClause = "JOIN `${END_CONDITION_TABLE}` ON $tableName.id = ${END_CONDITION_TABLE}.scenario_id",
            SQLiteColumn.Default("$END_CONDITION_TABLE.id", Long::class),
            scenarioEndConditionOperatorColumn,
            closure,
        )

    private fun SQLiteTable.forEachEndCondition(scenarioId: Long, closure: (Long, Int) -> Unit) =
        forEachRow(
            extraClause = "WHERE `scenario_id` = $scenarioId",
            SQLiteColumn.Default("eventId", type = Long::class),
            SQLiteColumn.Default("executions", Int::class),
            closure,
        )
}
