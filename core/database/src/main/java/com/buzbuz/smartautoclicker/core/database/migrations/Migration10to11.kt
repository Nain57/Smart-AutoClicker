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
import com.buzbuz.smartautoclicker.core.base.sqlite.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.base.sqlite.copyColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.forEachRow
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.SCENARIO_TABLE
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

    private const val DETECTION_QUALITY_INCREASE = 600
    private const val DETECTION_QUALITY_NEW_MAX = 3216


    private val scenarioIdColumn = SQLiteColumn.PrimaryKey()
    private val scenarioDetectionQualityColumn = SQLiteColumn.Default("detection_quality", Int::class)

    private val conditionIdColumn = SQLiteColumn.PrimaryKey()
    private val conditionShouldBeDetectedColumn = SQLiteColumn.Default("shouldBeDetected", Boolean::class)

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionEventIdColumn = SQLiteColumn.ForeignKey(
        name = "eventId", type = Long::class,
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )
    private val actionTypeColumn = SQLiteColumn.Default("type", String::class)
    private val actionOldClickOnConditionColumn = SQLiteColumn.Default("clickOnCondition", Boolean::class)
    private val actionClickOnConditionIdColumn = SQLiteColumn.ForeignKey(
        name = "clickOnConditionId", type = Long::class, isNotNull = false,
        referencedTable = CONDITION_TABLE, referencedColumn = "id", deleteAction = ForeignKey.SET_NULL,
    )
    private val actionToggleEventIdColumn = SQLiteColumn.ForeignKey(
        name = "toggle_event_id", type = Long::class, isNotNull = false,
        referencedTable = EVENT_TABLE, referencedColumn = "id", deleteAction = ForeignKey.SET_NULL,
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(SCENARIO_TABLE).apply {
            forEachScenario { id, oldQuality ->
                updateDetectionQuality(
                    id,
                    (oldQuality + DETECTION_QUALITY_INCREASE).coerceAtMost(DETECTION_QUALITY_NEW_MAX)
                )
            }
        }

        val conditionTable = db.getSQLiteTableReference(CONDITION_TABLE)
        val oldActionTable = db.getSQLiteTableReference(ACTION_TABLE)
        val newActionTable = db.createTempActionTable()

        newActionTable.copyAllActionsExceptChangedParams()
        oldActionTable.forEachClick { id, eventId, clickOnCondition ->
            newActionTable.apply {
                if (clickOnCondition) {
                    updateClickOnConditionToId(id, conditionTable.getEventFirstValidConditionId(eventId))
                    updateClickPositionType(id, ClickPositionType.ON_DETECTED_CONDITION)
                } else {
                    updateClickPositionType(id, ClickPositionType.USER_SELECTED)
                }
            }
        }
        oldActionTable.dropTable()

        newActionTable.apply {
            createIndex(actionEventIdColumn, "index_action_table_eventId")
            createIndex(actionToggleEventIdColumn, "index_action_table_toggle_event_id")
            createIndex(actionClickOnConditionIdColumn, "index_action_table_clickOnConditionId")
            alterTableRename(ACTION_TABLE)
        }
    }

    private fun SupportSQLiteDatabase.createTempActionTable(): SQLiteTable =
        getSQLiteTableReference("temp_action_table").apply {
            createTable(
                columns = setOf(
                    actionEventIdColumn,
                    SQLiteColumn.Default("priority", Int::class),
                    SQLiteColumn.Default("name", String::class),
                    SQLiteColumn.Default("type", String::class),
                    SQLiteColumn.Default("clickPositionType", String::class, isNotNull = false),
                    SQLiteColumn.Default("x", Int::class, isNotNull = false),
                    SQLiteColumn.Default("y", Int::class, isNotNull = false),
                    actionClickOnConditionIdColumn,
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
                    actionToggleEventIdColumn,
                    SQLiteColumn.Default("toggle_type", String::class, isNotNull = false),
                ),
            )
        }

    private fun SQLiteTable.forEachScenario(closure: (id: Long, detectionQuality: Int) -> Unit): Unit =
        forEachRow(null, scenarioIdColumn, scenarioDetectionQualityColumn, closure)

    private fun SQLiteTable.forEachClick(closure: (id: Long, eventId: Long, clickOnCondition: Boolean) -> Unit): Unit =
        forEachRow(
            extraClause = "WHERE `type` = \"${ActionType.CLICK}\"",
            actionIdColumn, actionEventIdColumn, actionTypeColumn, actionOldClickOnConditionColumn,
        ) { id, eventId, _, clickOnCondition -> closure(id, eventId, clickOnCondition) }

    private fun SQLiteTable.copyAllActionsExceptChangedParams() = insertIntoSelect(
        fromTableName = ACTION_TABLE,
        columnsToFromColumns = arrayOf(
            copyColumn("`id`"), copyColumn("`eventId`"), copyColumn("`priority`"), copyColumn("`name`"), copyColumn("`type`"),
            "`clickPositionType`" to "NULL",
            copyColumn("`x`"), copyColumn("`y`"),
            "`clickOnConditionId`" to "NULL",
            copyColumn("`pressDuration`"), copyColumn("`fromX`"), copyColumn("`fromY`"), copyColumn("`toX`"), copyColumn("`toY`"),
            copyColumn("`swipeDuration`"), copyColumn("`pauseDuration`"), copyColumn("`isAdvanced`"), copyColumn("`isBroadcast`"),
            copyColumn("`intent_action`"), copyColumn("`component_name`"), copyColumn("`flags`"), copyColumn("`toggle_event_id`"),
            copyColumn("`toggle_type`"),
        )
    )

    private fun SQLiteTable.getEventFirstValidConditionId(eventId: Long): Long? {
        var validConditionId: Long? = null

        forEachRow(
            extraClause = "WHERE `eventId` = $eventId AND `shouldBeDetected` = 1",
            conditionIdColumn, conditionShouldBeDetectedColumn,
        ) { id, shouldBeDetected ->

            if (validConditionId == null && shouldBeDetected) {
                validConditionId = id
            }
        }

        return validConditionId
    }

    /** Update the current detection quality to have at least the same quality (should overall be better). */
    private fun SQLiteTable.updateDetectionQuality(id: Long, detectionQuality: Int) = updateWithNames(
        "WHERE `id` = $id", "detection_quality" to detectionQuality.toString()
    )

    private fun SQLiteTable.updateClickPositionType(actionId: Long, positionType: ClickPositionType) = updateWithNames(
        "WHERE `id` = $actionId", "clickPositionType" to "\"${positionType.name}\"",
    )

    private fun SQLiteTable.updateClickOnConditionToId(actionId: Long, conditionId: Long?) = update(
        "WHERE `id` = $actionId", actionClickOnConditionIdColumn to "$conditionId",
    )
}
