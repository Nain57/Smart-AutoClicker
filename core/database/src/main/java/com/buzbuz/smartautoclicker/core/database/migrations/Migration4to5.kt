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

import androidx.annotation.VisibleForTesting
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.sqlite.getTable
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE

/**
 * Migration from database v4 to v5.
 *
 * Refactor of the condition table.
 *
 * Changes:
 * * add a name to the conditions.
 * * add the detection type to the conditions. Default must be EXACT (1), as it was the only behaviour previously.
 * * increase the threshold of existing condition by [THRESHOLD_INCREASE] in order to comply with the new algo.
 */
object Migration4to5 : Migration(4, 5) {

    /** Value to be added to current threshold in the database. */
    @VisibleForTesting
    internal const val THRESHOLD_INCREASE = 3
    /** Maximum value for the new threshold. */
    @VisibleForTesting
    internal const val THRESHOLD_MAX_VALUE = 20

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getTable(CONDITION_TABLE).apply {
            addConditionsColumns()
            forEachCondition { id, threshold ->
                updateThreshold(
                    id = id,
                    threshold = (threshold + THRESHOLD_INCREASE).coerceAtMost(THRESHOLD_MAX_VALUE),
                )
            }
        }
    }
}

private fun SQLiteTable.addConditionsColumns() = apply {
    alterTableAddColumn(SQLiteColumn.Default("name", String::class, defaultValue = "Condition"))
    alterTableAddColumn(SQLiteColumn.Default("detection_type", Int::class, defaultValue = "1"))
}

private fun SQLiteTable.forEachCondition(closure: (id: Long, threshold: Int) -> Unit) {
    select(setOf("id", "threshold")) { sqlRow ->
        closure(sqlRow.getLong("id"), sqlRow.getInt("threshold"))
    }
}

private fun SQLiteTable.updateThreshold(id: Long, threshold: Int) =
    update("WHERE `id` = $id", "threshold" to "$threshold")