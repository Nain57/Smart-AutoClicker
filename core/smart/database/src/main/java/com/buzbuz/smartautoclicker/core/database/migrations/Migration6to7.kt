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

import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.copyColumn
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.END_CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE

/**
 * Migration from database v6 to v7.
 *
 * Changes:
 * * creates end condition table
 * * add detection quality to scenario
 * * add end condition operator to scenario
 */
object Migration6to7 : Migration(6, 7) {

    private val endConditionScenarioIdColumn = SQLiteColumn.ForeignKey(
        name = "scenario_id",
        referencedTable = "scenario_table", referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    private val endConditionEventIdColumn = SQLiteColumn.ForeignKey(
        name = "event_id",
        referencedTable = "event_table", referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(END_CONDITION_TABLE).apply {
            createEndConditionTable()
            createIndex(endConditionScenarioIdColumn)
            createIndex(endConditionEventIdColumn)

            initializeEndConditions()
        }

        db.getSQLiteTableReference(SCENARIO_TABLE)
            .addScenarioNewColumns()
    }

    /** Create the table for the end conditions. */
    private fun SQLiteTable.createEndConditionTable(): Unit = createTable(
        columns = setOf(
            endConditionScenarioIdColumn,
            endConditionEventIdColumn,
            SQLiteColumn.Int("executions"),
        )
    )

    /** Insert a new end condition for each event with a stop after. */
    private fun SQLiteTable.initializeEndConditions() = insertIntoSelect(
        fromTableName = "event_table",
        extraClause = "WHERE `stop_after` IS NOT NULL",
        columnsToFromColumns = arrayOf(
            copyColumn("scenario_id"),
            "event_id" to "id",
            "executions" to "stop_after",
        )
    )

    /**
     * Add the detection quality & operator column to the scenario table.
     * Quality default value is the new algorithm default value, operator default value is OR.
     */
    private fun SQLiteTable.addScenarioNewColumns() = apply {
        alterTableAddColumn(SQLiteColumn.Int("detection_quality", defaultValue = "600"))
        alterTableAddColumn(SQLiteColumn.Int("end_condition_operator", defaultValue = "2"))
    }
}
