/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.database.SCENARIO_USAGE_TABLE

/**
 * Migration from database v23 to v24.
 *
 * Scenario usage is a one-to-one relation. Older versions only indexed the scenario id, which allowed duplicate
 * rows to be created by racing usage updates. Merge those rows deterministically, then enforce the relation in the
 * schema so future starts and scenario switches have one authoritative statistic record.
 */
object Migration23to24 : Migration(23, 24) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE `$SCENARIO_USAGE_TABLE` " +
                "SET `start_count` = (" +
                "SELECT SUM(`start_count`) FROM `$SCENARIO_USAGE_TABLE` AS duplicate " +
                "WHERE duplicate.`scenario_id` = `$SCENARIO_USAGE_TABLE`.`scenario_id`" +
                "), " +
                "`last_start_timestamp_ms` = (" +
                "SELECT MAX(`last_start_timestamp_ms`) FROM `$SCENARIO_USAGE_TABLE` AS duplicate " +
                "WHERE duplicate.`scenario_id` = `$SCENARIO_USAGE_TABLE`.`scenario_id`" +
                ") " +
                "WHERE `id` IN (" +
                "SELECT MIN(`id`) FROM `$SCENARIO_USAGE_TABLE` GROUP BY `scenario_id`" +
                ")"
        )
        db.execSQL(
            "DELETE FROM `$SCENARIO_USAGE_TABLE` " +
                "WHERE `id` NOT IN (SELECT MIN(`id`) FROM `$SCENARIO_USAGE_TABLE` GROUP BY `scenario_id`)"
        )
        db.execSQL("DROP INDEX IF EXISTS `index_${SCENARIO_USAGE_TABLE}_scenario_id`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_${SCENARIO_USAGE_TABLE}_scenario_id` " +
                "ON `$SCENARIO_USAGE_TABLE` (`scenario_id`)"
        )
    }
}
