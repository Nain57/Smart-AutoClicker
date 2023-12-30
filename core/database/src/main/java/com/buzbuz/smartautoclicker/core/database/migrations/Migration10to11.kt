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

import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.sqlite.getTable
import com.buzbuz.smartautoclicker.core.base.sqlite.keepColumn
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType

/**
 * Migration from database v10 to v11.
 *
 * * change the clickOnCondition boolean column into click_on_condition_id integer column. This will
 * allow to reference a condition to click on.
 * * detection algorithm have been updated, and the detection quality value must be updated as well.
 */
object Migration10to11 : Migration(10, 11) {

    private val eventIdForeignKey = SQLiteColumn.ForeignKey(
        name = "eventId", type = Long::class,
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    private val clickOnConditionIdForeignKey = SQLiteColumn.ForeignKey(
        name = "clickOnConditionId", type = Long::class, isNotNull = false,
        referencedTable = CONDITION_TABLE, referencedColumn = "id", deleteAction = ForeignKey.SET_NULL,
    )

    private val toggleEventIdForeignKey = SQLiteColumn.ForeignKey(
        name = "toggle_event_id", type = Long::class, isNotNull = false,
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.SET_NULL,
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getTable(SCENARIO_TABLE).apply {
            forEachScenario { id, oldQuality ->
                updateDetectionQuality(
                    id,
                    (oldQuality + DETECTION_QUALITY_INCREASE).coerceAtMost(DETECTION_QUALITY_NEW_MAX)
                )
            }
        }

        val oldActionTable = db.getTable(ACTION_TABLE)
        db.createTempActionTable().apply {
            copyAllActionsExceptChangedParams()

            forEachClick { id, eventId, clickOnCondition ->
                if (clickOnCondition) {
                    updateClickOnConditionToId(
                        actionId = id,
                        conditionId = getEventFirstValidConditionId(eventId),
                    )
                    updateClickPositionType(id, ClickPositionType.ON_DETECTED_CONDITION)
                } else {
                    updateClickPositionType(id, ClickPositionType.USER_SELECTED)
                }
            }

            oldActionTable.dropTable()

            createIndex(eventIdForeignKey, "index_action_table_eventId")
            createIndex(toggleEventIdForeignKey, "index_action_table_toggle_event_id")
            createIndex(clickOnConditionIdForeignKey, "index_action_table_clickOnConditionId")
            alterTableRename(ACTION_TABLE)
        }
    }

    private fun SupportSQLiteDatabase.createTempActionTable(): SQLiteTable =
        getTable("temp_action_table").apply {
            createTable(
                primaryKey = SQLiteColumn.PrimaryKey(),
                columns = setOf(
                    eventIdForeignKey,
                    SQLiteColumn.Default("priority", Int::class),
                    SQLiteColumn.Default("name", String::class),
                    SQLiteColumn.Default("type", ActionType::class),
                    SQLiteColumn.Default("clickPositionType", ClickPositionType::class, isNotNull = false),
                    SQLiteColumn.Default("x", Int::class, isNotNull = false),
                    SQLiteColumn.Default("y", Int::class, isNotNull = false),
                    clickOnConditionIdForeignKey,
                    SQLiteColumn.Default("pressDuration", Long::class, isNotNull = false),
                    SQLiteColumn.Default("fromX", Int::class, isNotNull = false),
                    SQLiteColumn.Default("fromY", Int::class, isNotNull = false),
                    SQLiteColumn.Default("toX", Int::class, isNotNull = false),
                    SQLiteColumn.Default("toY", Int::class, isNotNull = false),
                    SQLiteColumn.Default("swipeDuration", Long::class, isNotNull = false),
                    SQLiteColumn.Default("pauseDuration", Long::class, isNotNull = false),
                    SQLiteColumn.Default("isAdvanced", Boolean::class, isNotNull = false),
                    SQLiteColumn.Default("isBroadcast", Boolean::class, isNotNull = false),
                    SQLiteColumn.Default("intent_action", String::class, isNotNull = false),
                    SQLiteColumn.Default("component_name", String::class, isNotNull = false),
                    SQLiteColumn.Default("flags", Int::class, isNotNull = false),
                    toggleEventIdForeignKey,
                    SQLiteColumn.Default("toggle_type", EventToggleType::class, isNotNull = false),
                ),
            )
        }
}

private fun SQLiteTable.forEachScenario(closure: (id: Long, detectionQuality: Int) -> Unit) {
    select(setOf("id", "detection_quality")) { sqlRow ->
        closure(
            sqlRow.getLong("id"),
            sqlRow.getInt("detection_quality"),
        )
    }
}

private fun SQLiteTable.forEachClick(closure: (id: Long, eventId: Long, clickOnCondition: Boolean) -> Unit) {
    select(setOf("id", "eventId", "type", "clickOnConditionId"), "WHERE `type` = \"${ActionType.CLICK}\"") { sqlRow ->
        closure(
            sqlRow.getLong("id"),
            sqlRow.getLong("eventId"),
            sqlRow.getBoolean("clickOnConditionId"),
        )
    }
}

private fun SQLiteTable.forEachEventConditions(eventId: Long, closure: (id: Long, shouldBeDetected: Boolean) -> Unit) {
    select(setOf("id", "shouldBeDetected"), "WHERE `eventId` = $eventId AND `shouldBeDetected` = 1") { sqlRow ->
        closure(
            sqlRow.getLong("id"),
            sqlRow.getBoolean("shouldBeDetected"),
        )
    }
}

private fun SQLiteTable.copyAllActionsExceptChangedParams() = insertIntoSelect(
    fromTableName = ACTION_TABLE,
    columnsToFromColumns = arrayOf(
        keepColumn("`id`"), keepColumn("`eventId`"), keepColumn("`priority`"), keepColumn("`name`"), keepColumn("`type`"),
        "`clickPositionType`" to "NULL",
        keepColumn("`x`"), keepColumn("`y`"),
        "`clickOnConditionId`" to "NULL",
        keepColumn("`pressDuration`"), keepColumn("`fromX`"), keepColumn("`fromY`"), keepColumn("`toX`"), keepColumn("`toY`"),
        keepColumn("`swipeDuration`"), keepColumn("`pauseDuration`"), keepColumn("`isAdvanced`"), keepColumn("`isBroadcast`"),
        keepColumn("`intent_action`"), keepColumn("`component_name`"), keepColumn("`flags`"), keepColumn("`toggle_event_id`"),
        keepColumn("`toggle_type`"),
    )
)

/** Update the current detection quality to have at least the same quality (should overall be better). */
private fun SQLiteTable.updateDetectionQuality(id: Long, detectionQuality: Int) = update(
    "WHERE `id` = $id", "detection_quality" to detectionQuality.toString()
)

private fun SQLiteTable.getEventFirstValidConditionId(eventId: Long): Long? {
    var validConditionId: Long? = null
    forEachEventConditions(eventId) { id, shouldBeDetected ->
        if (shouldBeDetected) {
            validConditionId = id
            return@forEachEventConditions
        }
    }

    return validConditionId
}

private fun SQLiteTable.updateClickPositionType(actionId: Long, positionType: ClickPositionType) = update(
    "WHERE `id` = $actionId", "clickPositionType" to positionType.name,
)

private fun SQLiteTable.updateClickOnConditionToId(actionId: Long, conditionId: Long?) = update(
    "WHERE `id` = $actionId", "clickOnConditionId" to "$conditionId",
)

private const val DETECTION_QUALITY_INCREASE = 600
private const val DETECTION_QUALITY_NEW_MAX = 3216