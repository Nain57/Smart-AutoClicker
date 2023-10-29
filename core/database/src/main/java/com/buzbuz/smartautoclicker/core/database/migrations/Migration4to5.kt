/*
 * Copyright (C) 2022 Kevin Buzeau
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
        db.apply {
            execSQL(addConditionNameColumn)
            execSQL(addConditionDetectionTypeColumn)
            updateAllThreshold()
        }
    }

    private fun SupportSQLiteDatabase.updateAllThreshold() {
        query(getAllConditions).use { cursor ->
            if (cursor.count == 0) {
                return
            }

            cursor.moveToFirst()

            val idColumnIndex = cursor.getColumnIndex("id")
            val thresholdColumnIndex = cursor.getColumnIndex("threshold")
            if (idColumnIndex< 0 || thresholdColumnIndex < 0) throw IllegalStateException("Can't find columns")

            do {
                val newThreshold = (cursor.getInt(thresholdColumnIndex) + THRESHOLD_INCREASE)
                    .coerceAtMost(THRESHOLD_MAX_VALUE)
                execSQL(updateThreshold(cursor.getLong(idColumnIndex), newThreshold))
            } while (cursor.moveToNext())
        }
    }

    /** Update the current condition threshold to keep detecting old conditions. */
    private fun updateThreshold(id: Long, threshold: Int) = """
        UPDATE `condition_table` 
        SET `threshold` = $threshold
        WHERE `id` = $id
    """.trimIndent()

    /** Add the name column to the condition table. */
    private val addConditionNameColumn = """
        ALTER TABLE `condition_table` 
        ADD COLUMN `name` TEXT DEFAULT "Condition" NOT NULL
    """.trimIndent()

    /** Add the detection type column to the condition table. */
    private val addConditionDetectionTypeColumn = """
        ALTER TABLE `condition_table` 
        ADD COLUMN `detection_type` INTEGER DEFAULT 1 NOT NULL
    """.trimIndent()

    private val getAllConditions = """
        SELECT `id`, `threshold` 
        FROM `condition_table`
    """.trimIndent()
}