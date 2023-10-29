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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database v6 to v7.
 *
 * Changes:
 * * creates end condition table
 * * add detection quality to scenario
 * * add end condition operator to scenario
 */
object Migration6to7 : Migration(6, 7) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL(createEndConditionTable)
            execSQL(endConditionToScenarioIndex)
            execSQL(endConditionToEventIndex)
            execSQL(insertEndConditions)
            execSQL(addDetectionQualityColumn)
            execSQL(addEndConditionOperatorColumn)
        }
    }

    /** Create the table for the end conditions. */
    private val createEndConditionTable = """
        CREATE TABLE IF NOT EXISTS `end_condition_table` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `scenario_id` INTEGER NOT NULL,
            `event_id` INTEGER NOT NULL,
            `executions` INTEGER NOT NULL,
            FOREIGN KEY(`scenario_id`) REFERENCES `scenario_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
            FOREIGN KEY(`event_id`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
        )
    """.trimIndent()

    /** Creates the index between a end conditions and its scenario. */
    private val endConditionToScenarioIndex = """
        CREATE INDEX IF NOT EXISTS `index_end_condition_table_scenario_id` 
        ON `end_condition_table` (`scenario_id`)
    """.trimIndent()

    /** Creates the index between a end conditions and its event. */
    private val endConditionToEventIndex = """
        CREATE INDEX IF NOT EXISTS `index_end_condition_table_event_id` 
        ON `end_condition_table` (`event_id`)
    """.trimIndent()

    /** Insert a new end condition for each event with a stop after. */
    private val insertEndConditions = """
        INSERT INTO `end_condition_table` (scenario_id, event_id, executions)
        SELECT `scenario_id`, `id`, `stop_after`
        FROM `event_table`
        WHERE `stop_after` IS NOT NULL
    """.trimIndent()

    /**
     * Add the detection quality column to the scenario table.
     * Default value is the new algorithm default value.
     */
    private val addDetectionQualityColumn = """
        ALTER TABLE `scenario_table`
        ADD COLUMN `detection_quality` INTEGER DEFAULT 600 NOT NULL
    """.trimIndent()

    /**
     * Add the end condition operator column to the scenario table.
     * Default value is OR.
     */
    private val addEndConditionOperatorColumn = """
        ALTER TABLE `scenario_table`
        ADD COLUMN `end_condition_operator` INTEGER DEFAULT 2 NOT NULL
    """.trimIndent()
}