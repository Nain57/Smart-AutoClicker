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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType

/**
 * Migration from database v5 to v6.
 *
 * Changes:
 * * add clickOnCondition to the action click table.
 * * add shouldBeDetected to the condition table in order to allow condition negation.
 */
object Migration5to6 : Migration(5, 6) {

    private val conditionShouldBeDetectedColumn =
        SQLiteColumn.Boolean("shouldBeDetected", defaultValue = "1")

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionTypeColumn = SQLiteColumn.Text("type")
    private val actionClickOnConditionColumn = SQLiteColumn.Int("clickOnCondition", isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            getSQLiteTableReference(CONDITION_TABLE).alterTableAddColumn(conditionShouldBeDetectedColumn)

            getSQLiteTableReference(ACTION_TABLE).apply {
                alterTableAddColumn(actionClickOnConditionColumn)

                forEachRow(null, actionIdColumn, actionTypeColumn) { actionId, actionType ->
                    if (ActionType.valueOf(actionType) == ActionType.CLICK)
                        updateClickOnCondition(actionId, 0)
                }
            }
        }
    }

    /** Update the click on condition value in the action table. */
    private fun SQLiteTable.updateClickOnCondition(id: Long, clickOnCondition: Int): Unit =
        update(
            extraClause = "WHERE `id` = $id",
            contentValues = ContentValues().apply {
                put(actionClickOnConditionColumn.name, clickOnCondition)
            },
        )
}
