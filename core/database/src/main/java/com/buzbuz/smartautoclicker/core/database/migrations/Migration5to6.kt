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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.sqlite.getTable
import com.buzbuz.smartautoclicker.core.database.entity.ActionType

/**
 * Migration from database v5 to v6.
 *
 * Changes:
 * * add clickOnCondition to the action click table.
 * * add shouldBeDetected to the condition table in order to allow condition negation.
 */
object Migration5to6 : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            getTable("condition_table").addConditionShouldBeDetectedColumn()

            getTable("action_table").apply {
                addActionClickOnConditionColumn()
                forEachActions { id, type ->
                    if (type == ActionType.CLICK) updateClickOnCondition(id, 0)
                }
            }
        }
    }
}


/** Add the should be detected column to the condition table. */
private fun SQLiteTable.addConditionShouldBeDetectedColumn() =
    alterTableAddColumn(SQLiteColumn.Default("shouldBeDetected", Boolean::class, defaultValue = "1"))

/** Add the click on condition to the action table. */
private fun SQLiteTable.addActionClickOnConditionColumn() =
    alterTableAddColumn(SQLiteColumn.Default("clickOnCondition", Int::class, isNotNull = false))


/** Update the click on condition value in the action table. */
private fun SQLiteTable.updateClickOnCondition(id: Long, clickOnCondition: Int) =
    update("WHERE `id` = $id", "clickOnCondition" to clickOnCondition.toString())

private fun SQLiteTable.forEachActions(closure: (id: Long, type: ActionType) -> Unit) {
    select(setOf("id", "type")) { sqlRow ->
        closure(sqlRow.getLong("id"), sqlRow.getEnumValue("type"))
    }
}