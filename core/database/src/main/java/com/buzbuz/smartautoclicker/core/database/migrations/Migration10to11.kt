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
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType

/**
 * Migration from database v10 to v11.
 *
 * * change the clickOnCondition boolean column into click_on_condition_id integer column. This will
 * allow to reference a condition to click on.
 * * detection algorithm have been updated, and the detection quality value must be updated as well.
 */
object Migration10to11 : Migration(10, 11) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            migrateDetectionQuality()

            execSQL(createTempActionTable)
            execSQL(copyAllExceptChangedParams)
            migrateToClickOnCondition()

            execSQL(deleteOldActionTable)

            execSQL(createEventIdIndexTable)
            execSQL(createToggleEventIdIndexTable)
            execSQL(createClickOnConditionIdIndexTable)

            execSQL(renameTempActionTable)
        }
    }

    // --- START Update detection quality migration --- //

    private fun SupportSQLiteDatabase.migrateDetectionQuality() {
        query(getAllScenario).use { cursor ->
            // Nothing to do ?
            if (cursor.count == 0) return
            cursor.moveToFirst()

            // Get all columns indexes
            val idColumnIndex = cursor.getColumnIndex("id")
            val qualityColumnIndex = cursor.getColumnIndex("detection_quality")
            if (idColumnIndex < 0 || qualityColumnIndex < 0)
                throw IllegalStateException("Can't find columns")

            do {
                val scenarioId = cursor.getLong(idColumnIndex)
                val oldQuality = cursor.getInt(qualityColumnIndex)
                val newQuality = (oldQuality + DETECTION_QUALITY_INCREASE).coerceAtMost(DETECTION_QUALITY_NEW_MAX)

                execSQL(updateDetectionQuality(scenarioId, newQuality))
            } while (cursor.moveToNext())
        }
    }

    private val getAllScenario = """
        SELECT `id`, `detection_quality`
        FROM `scenario_table`
    """.trimIndent()

    /** Update the current detection quality to have at least the same quality (should overall be better). */
    private fun updateDetectionQuality(id: Long, detectionQuality: Int) = """
        UPDATE `scenario_table` 
        SET `detection_quality` = $detectionQuality
        WHERE `id` = $id
    """.trimIndent()

    // --- END Update detection quality migration --- //

    // --- START Click on condition migration --- //
    private fun SupportSQLiteDatabase.migrateToClickOnCondition() {
        query(getAllClicks).use { cursor ->
            // Nothing to do ?
            if (cursor.count == 0) return
            cursor.moveToFirst()

            // Get all columns indexes
            val idColumnIndex = cursor.getColumnIndex("id")
            val eventIdColumnIndex = cursor.getColumnIndex("eventId")
            val clickOnConditionIndex = cursor.getColumnIndex("clickOnCondition")
            if (idColumnIndex < 0 || eventIdColumnIndex < 0 || clickOnConditionIndex < 0)
                throw IllegalStateException("Can't find columns")

            do {
                val actionId = cursor.getLong(idColumnIndex)
                if (cursor.getInt(clickOnConditionIndex) == 1) {
                    execSQL(updateClickOnConditionToId(
                        actionId = cursor.getLong(idColumnIndex),
                        conditionId = getEventFirstValidConditionId(cursor.getLong(eventIdColumnIndex)),
                    ))
                    execSQL(updateClickPositionType(actionId, ClickPositionType.ON_DETECTED_CONDITION))
                } else {
                    execSQL(updateClickPositionType(actionId, ClickPositionType.USER_SELECTED))
                }
            } while (cursor.moveToNext())
        }
    }

    private fun SupportSQLiteDatabase.getEventFirstValidConditionId(eventId: Long): Long? =
        query(getEventConditions(eventId)).use { conditionsCursor ->
            // Nothing to do ?
            if (conditionsCursor.count == 0) return null
            conditionsCursor.moveToFirst()

            // Get all columns indexes
            val conditionIdColumnIndex = conditionsCursor.getColumnIndex("id")
            val shouldBeDetectedColumnIndex = conditionsCursor.getColumnIndex("shouldBeDetected")

            if (conditionIdColumnIndex < 0 || shouldBeDetectedColumnIndex < 0)
                throw IllegalStateException("Can't find columns")

            do {
                if (conditionsCursor.getInt(shouldBeDetectedColumnIndex) == 1)
                    return conditionsCursor.getLong(conditionIdColumnIndex)
            } while (conditionsCursor.moveToNext())

            // No valid condition found
            return null
        }

    private val createTempActionTable = """
        CREATE TABLE IF NOT EXISTS `temp_action_table` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
            `eventId` INTEGER NOT NULL, 
            `priority` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `type` TEXT NOT NULL, 
            `clickPositionType` TEXT,
            `x` INTEGER, 
            `y` INTEGER, 
            `clickOnConditionId` INTEGER, 
            `pressDuration` INTEGER, 
            `fromX` INTEGER, 
            `fromY` INTEGER, 
            `toX` INTEGER, 
            `toY` INTEGER, 
            `swipeDuration` INTEGER, 
            `pauseDuration` INTEGER, 
            `isAdvanced` INTEGER, 
            `isBroadcast` INTEGER, 
            `intent_action` TEXT, 
            `component_name` TEXT, 
            `flags` INTEGER, 
            `toggle_event_id` INTEGER, 
            `toggle_type` TEXT, 
            FOREIGN KEY(`eventId`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
            FOREIGN KEY(`clickOnConditionId`) REFERENCES `condition_table`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
            FOREIGN KEY(`toggle_event_id`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
        )
    """.trimIndent()

    private val createEventIdIndexTable = """
        CREATE INDEX IF NOT EXISTS `index_action_table_eventId` ON `temp_action_table` (`eventId`)
    """.trimIndent()

    private val createClickOnConditionIdIndexTable = """
        CREATE INDEX IF NOT EXISTS `index_action_table_clickOnConditionId` ON `temp_action_table` (`clickOnConditionId`)
    """.trimIndent()

    private val createToggleEventIdIndexTable = """
        CREATE INDEX IF NOT EXISTS `index_action_table_toggle_event_id` ON `temp_action_table` (`toggle_event_id`)
    """.trimIndent()

    private val copyAllExceptChangedParams = """
        INSERT INTO temp_action_table (
            `id`, `eventId`, `priority`, `name`, `type`, `clickPositionType`, `x`, `y`, `clickOnConditionId`, 
            `pressDuration`, `fromX`, `fromY`, `toX`, `toY`, `swipeDuration`, `pauseDuration`, `isAdvanced`, 
            `isBroadcast`, `intent_action`, `component_name`, `flags`, `toggle_event_id`, `toggle_type`
        ) 
        SELECT 
            `id`, `eventId`, `priority`, `name`, `type`, NULL, `x`, `y`, NULL, `pressDuration`, 
            `fromX`, `fromY`, `toX`, `toY`, `swipeDuration`, `pauseDuration`, `isAdvanced`, `isBroadcast`, 
            `intent_action`, `component_name`, `flags`, `toggle_event_id`, `toggle_type`
        FROM action_table
    """.trimIndent()

    private val getAllClicks = """
        SELECT `id`, `eventId`, `type`, `x`, `y`, `clickOnCondition`
        FROM `action_table`
        WHERE `type` = "${ActionType.CLICK}"
    """.trimIndent()

    private fun getEventConditions(eventId: Long) = """
        SELECT `id`, `eventId`, `shouldBeDetected`
        FROM `condition_table`
        WHERE `eventId` = "$eventId" AND `shouldBeDetected` = 1
    """.trimIndent()

    private fun updateClickPositionType(actionId: Long, positionType: ClickPositionType) = """
        UPDATE `temp_action_table`
        SET `clickPositionType` = "$positionType"
        WHERE `id` = $actionId
    """.trimIndent()

    private fun updateClickOnConditionToId(actionId: Long, conditionId: Long?) = """
        UPDATE `temp_action_table`
        SET `clickOnConditionId` = $conditionId
        WHERE `id` = $actionId
    """.trimIndent()

    private val deleteOldActionTable = """
        DROP TABLE `action_table`
    """.trimIndent()

    private val renameTempActionTable = """
        ALTER TABLE `temp_action_table` 
        RENAME TO `action_table`
    """.trimIndent()

    // --- END Click on condition migration --- //
}

private const val DETECTION_QUALITY_INCREASE = 600
private const val DETECTION_QUALITY_NEW_MAX = 3216