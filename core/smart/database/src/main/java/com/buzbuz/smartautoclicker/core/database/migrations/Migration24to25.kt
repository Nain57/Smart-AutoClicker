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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE

/**
 * Migration from database v24 to v25.
 *
 * Introduces the condition counter operation value, allowing a change counter action to use the position of a
 * condition detected in its event as operand.
 */
object Migration24to25 : Migration(24, 25) {

    private val actionCounterOperationConditionIdColumn =
        SQLiteColumn.Long("counter_operation_condition_id", isNotNull = false)
    private val actionCounterOperationConditionPositionTypeColumn =
        SQLiteColumn.Text("counter_operation_condition_position_type", isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(ACTION_TABLE).alterTableAddColumns(
            setOf(actionCounterOperationConditionIdColumn, actionCounterOperationConditionPositionTypeColumn)
        )
    }
}
