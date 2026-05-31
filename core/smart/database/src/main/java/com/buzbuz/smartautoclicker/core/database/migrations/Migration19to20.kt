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
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType

/**
 * Migration from database v18 to v19.
 *
 * Klick'r 4.0.0 release migration:
 * * Counter values are now stored as REAL (Double)
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

    override fun migrate(db: SupportSQLiteDatabase) {
        db.migrateConditions()
        db.migrateActions()
    }

    private fun SupportSQLiteDatabase.migrateConditions() {
        getSQLiteTableReference(CONDITION_TABLE).apply {
            // Get the current counter reached condition counter values
            val counterReachedValues = buildMap {
                forEachCounterReachedCondition { id, type, counterValue ->
                    if (ConditionType.valueOf(type) == ConditionType.ON_COUNTER_REACHED) put(id, counterValue)
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

    private fun SQLiteTable.forEachCounterReachedCondition(closure: (Long, String, Int) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ConditionType.ON_COUNTER_REACHED}\"",
            columnA = conditionIdColumn,
            columnB = conditionTypeColumn,
            columnC = conditionCounterValuesOldColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.restoreConditionCounterValue(conditionId: Long, counterValue: Double) = update(
        extraClause = "WHERE `id` = $conditionId",
        contentValues = ContentValues().apply {
            put(conditionCounterValuesNewColumn.name, counterValue)
        },
    )

    private fun SupportSQLiteDatabase.migrateActions() {
        getSQLiteTableReference(ACTION_TABLE).apply {
            // Get the current counter action values
            val counterValues = buildMap {
                forEachChangeCounterAction { id, type, counterValue ->
                    if (ActionType.valueOf(type) == ActionType.CHANGE_COUNTER) put(id, counterValue)
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

    private fun SQLiteTable.forEachChangeCounterAction(closure: (Long, String, Int) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ActionType.CHANGE_COUNTER}\"",
            columnA = actionIdColumn,
            columnB = actionTypeColumn,
            columnC = actionCounterValueOldColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.restoreActionCounterValue(actionId: Long, counterValue: Double) = update(
        extraClause = "WHERE `id` = $actionId",
        contentValues = ContentValues().apply {
            put(actionCounterValueNewColumn.name, counterValue)
        },
    )
}
