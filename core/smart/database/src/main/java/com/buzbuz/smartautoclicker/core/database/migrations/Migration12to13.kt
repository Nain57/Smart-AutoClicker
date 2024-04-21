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

import android.content.ContentValues
import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.copyColumn
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
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
 * - Add type column to event_table
 * - Add type column to condition_table. This comes with new nullable columns depending on it. Previous not nullable
 * columns related to image detection are now nullable.
 * - Create a new event_toggle_table
 * - Migrate action table from old toggle event system to new one
 * - Migrate the end condition table to its new equivalent (toggle off all events)
 * - Destroy end condition table.
 */
object Migration12to13 : Migration(12, 13) {

    private val scenarioEndConditionOperatorColumn = SQLiteColumn.Int("end_condition_operator")

    private val evtToggleToggleTypeColumn = SQLiteColumn.Text("toggle_type")
    private val evtToggleActionIdColumn = SQLiteColumn.ForeignKey(
        name = "action_id",
        referencedTable = ACTION_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )
    private val evtToggleToggleEventIdColumn = SQLiteColumn.ForeignKey(
        name = "toggle_event_id",
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    private val actionOldToggleEventIdColumn = SQLiteColumn.Long("toggle_event_id", isNotNull = false)
    private val actionOldToggleTypeColumn = SQLiteColumn.Text("toggle_type", isNotNull = false)

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
        alterTableAddColumn(SQLiteColumn.Text("type", defaultValue = EventType.IMAGE_EVENT.name))
    }

    private fun SupportSQLiteDatabase.migrateConditionTable() = getSQLiteTableReference(CONDITION_TABLE).apply {
        val changedColumns = setOf(
            SQLiteColumn.Text("path", isNotNull = false),
            SQLiteColumn.Int("area_left", isNotNull = false),
            SQLiteColumn.Int("area_top", isNotNull = false),
            SQLiteColumn.Int("area_right", isNotNull = false),
            SQLiteColumn.Int("area_bottom", isNotNull = false),
            SQLiteColumn.Int("threshold", isNotNull = false),
            SQLiteColumn.Int("detection_type", isNotNull = false),
            SQLiteColumn.Boolean("shouldBeDetected", isNotNull = false),
            SQLiteColumn.Int("detection_area_left", isNotNull = false),
            SQLiteColumn.Int("detection_area_top", isNotNull = false),
            SQLiteColumn.Int("detection_area_right", isNotNull = false),
            SQLiteColumn.Int("detection_area_bottom", isNotNull = false),
        )

        // Create temp condition table stripped from changed columns
        val (newConditionTable, indexes) = copyTable(droppedColumns = changedColumns.map { it.name }, withValues = false)
        // Add new columns and old columns that became nullable
        newConditionTable.alterTableAddColumns(
            setOf(
                SQLiteColumn.Text("type", defaultValue = ConditionType.ON_IMAGE_DETECTED.name),
                SQLiteColumn.Text("broadcast_action", isNotNull = false),
                SQLiteColumn.Text("counter_name", isNotNull = false),
                SQLiteColumn.Text("counter_comparison_operation", isNotNull = false),
                SQLiteColumn.Int("counter_value", isNotNull = false),
                SQLiteColumn.Long("timer_value_ms", isNotNull = false),
                SQLiteColumn.Long("timer_restart_when_reached", isNotNull = false),
            ) + changedColumns
        )
        // Copy columns content from table to temp_table
        val copyColumns = buildSet {
            addAll(changedColumns.map { copyColumn(it.name) })
            add(copyColumn("id"))
            add(copyColumn("eventId"))
            add(copyColumn("name"))
        }
        newConditionTable.insertIntoSelect(
            fromTableName = CONDITION_TABLE,
            columnsToFromColumns = copyColumns.toTypedArray()
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
                    SQLiteColumn.Text("counter_name", isNotNull = false),
                    SQLiteColumn.Text("counter_operation", isNotNull = false),
                    SQLiteColumn.Int("counter_operation_value", isNotNull = false),
                    SQLiteColumn.Boolean("toggle_all", isNotNull = false),
                    SQLiteColumn.Text("toggle_all_type", isNotNull = false),
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
                    update(extraClause = "WHERE `id` = $id", ContentValues().apply { put("toggle_all", "0") })
                    id
                }

                // For each toggle action, creates it's counterpart as a EventToggle referencing the aggregator
                postEventToggleTable.insertIntoValues(
                    ContentValues().apply {
                        put(evtToggleActionIdColumn.name, currentAggregatorId)
                        put(evtToggleToggleTypeColumn.name, toggleType)
                        put(evtToggleToggleEventIdColumn.name, toggleEventId)
                    }
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
                    ContentValues().apply {
                        put("scenario_id", scenarioId)
                        put("name", "Stop scenario")
                        put("type", EventType.TRIGGER_EVENT.name)
                        put("operator", endConditionOperator.toString())
                        put("priority", "-1")
                        put("enabled_on_start", "1")
                    }
                )

                // Add the action that end the scenario
                postActionTable.insertIntoValues(
                    ContentValues().apply {
                        put("eventId", endConditionEventId)
                        put("priority", 0)
                        put("name", "Stop scenario")
                        put("type", ActionType.TOGGLE_EVENT.name)
                        put("toggle_all", "1")
                        put("toggle_all_type", EventToggleType.DISABLE.name)
                    }
                )

                // For each end condition in this scenario, creates a counter reached condition and a change counter action
                forEachEndCondition(scenarioId) { targetEventId, executions ->
                    val endConditionCounterName = "\"Stop Scenario $targetEventId\""

                    // First, create a counter action for this event
                    postActionTable.insertIntoValues(
                        ContentValues().apply {
                            put("eventId", targetEventId)
                            put("priority", 10000)
                            put("name", "Execution count")
                            put("type", ActionType.CHANGE_COUNTER.name)
                            put("counter_name", endConditionCounterName)
                            put("counter_operation", ChangeCounterOperationType.ADD.name)
                            put("counter_operation_value", 1)
                        }
                    )

                    // Then, create a condition in our end condition trigger event
                    postConditionTable.insertIntoValues(
                        ContentValues().apply {
                            put("eventId", endConditionEventId)
                            put("name", endConditionCounterName)
                            put("type", ConditionType.ON_COUNTER_REACHED.name)
                            put("counter_name", endConditionCounterName)
                            put("counter_comparison_operation", CounterComparisonOperation.GREATER_OR_EQUALS.name)
                            put("counter_value", executions)
                        }
                    )
                }
            }

            dropTable()
        }

    private fun SQLiteTable.forEachToggleEventAction(closure: (Long, Long, Long, String) -> Unit) =
        forEachRow(
            extraClause = "WHERE `type` =\"${ActionType.TOGGLE_EVENT}\" ORDER BY `priority` ASC",
            SQLiteColumn.PrimaryKey("id"),
            SQLiteColumn.Long("eventId"),
            actionOldToggleEventIdColumn, actionOldToggleTypeColumn, closure,
        )

    private fun SQLiteTable.forEachScenarioWithEndCondition(closure: (Long, Int) -> Unit) =
        forEachRow(
            extraClause = """
                JOIN `${END_CONDITION_TABLE}` ON $tableName.id = ${END_CONDITION_TABLE}.scenario_id
                GROUP BY $END_CONDITION_TABLE.scenario_id
            """.trimIndent(),
            SQLiteColumn.Long("$END_CONDITION_TABLE.scenario_id"),
            scenarioEndConditionOperatorColumn,
            closure,
        )

    private fun SQLiteTable.forEachEndCondition(scenarioId: Long, closure: (Long, Int) -> Unit) =
        forEachRow(
            extraClause = "WHERE `scenario_id` = $scenarioId",
            SQLiteColumn.Long("event_id"),
            SQLiteColumn.Int("executions"),
            closure,
        )
}
