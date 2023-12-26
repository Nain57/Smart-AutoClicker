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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buzbuz.smartautoclicker.core.database.utils.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.utils.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.utils.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.utils.INTEGER
import com.buzbuz.smartautoclicker.core.database.utils.TEXT
import com.buzbuz.smartautoclicker.core.database.utils.execAddColumnStatement
import com.buzbuz.smartautoclicker.core.database.utils.execAddNullableColumnsStatement


/**
 * Migration from database v12 to v13.
 *
 *
 */
object Migration12to13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execAddColumnStatement(EVENT_TABLE, "type", "IMAGE_EVENT")

            execAddColumnStatement(CONDITION_TABLE, "type", "ON_IMAGE_DETECTED")
            execAddNullableColumnsStatement(CONDITION_TABLE, conditionsNewNullableColumns)

            execAddNullableColumnsStatement(ACTION_TABLE, actionsNewNullableColumns)
        }
    }

    /** Map of new condition columns to their sql database type. */
    private val conditionsNewNullableColumns = mapOf(
        "broadcast_action" to TEXT,
        "counter_name" to TEXT,
        "counter_comparison_operation" to TEXT,
        "counter_value" to INTEGER,
        "timer_value_ms" to INTEGER,
    )

    /** Map of new actions columns to their sql database type. */
    private val actionsNewNullableColumns = mapOf(
        "counter_name" to TEXT,
        "counter_operation" to TEXT,
        "counter_operation_value" to INTEGER,
    )
}