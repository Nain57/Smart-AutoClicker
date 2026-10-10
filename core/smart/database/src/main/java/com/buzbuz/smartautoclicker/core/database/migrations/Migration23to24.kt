/*
 * Copyright (C) 2026 Kevin Buzeau
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
import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.GESTURE_FINGER_MOTION_TABLE
import com.buzbuz.smartautoclicker.core.database.GESTURE_FINGER_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType


/**
 * Introduces the gesture tables and moves the swipes and clicks into them.
 *
 * A swipe becomes a gesture with one finger doing a press, a move and a release. A click keeps its
 * [ActionType.CLICK] type, but its position and press duration are now stored as one finger doing a press, a wait
 * and a release.
 */
object Migration23to24 : Migration(23, 24) {

    private const val LEGACY_ACTION_TYPE_SWIPE = "SWIPE"
    /** Press duration used for the clicks having an invalid one, in milliseconds. */
    private const val DEFAULT_PRESS_DURATION_MS = 1L

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionPriorityColumn = SQLiteColumn.Int("priority")
    private val actionNameColumn = SQLiteColumn.Text("name")
    private val actionFromXColumn = SQLiteColumn.Int("fromX", isNotNull = false)
    private val actionFromYColumn = SQLiteColumn.Int("fromY", isNotNull = false)
    private val actionToXColumn = SQLiteColumn.Int("toX", isNotNull = false)
    private val actionToYColumn = SQLiteColumn.Int("toY", isNotNull = false)
    private val actionSwipeDurationColumn = SQLiteColumn.Long("swipeDuration", isNotNull = false)
    private val actionClickPositionTypeColumn = SQLiteColumn.Text("clickPositionType", isNotNull = false)
    private val actionXColumn = SQLiteColumn.Int("x", isNotNull = false)
    private val actionYColumn = SQLiteColumn.Int("y", isNotNull = false)
    private val actionClickOnConditionIdColumn = SQLiteColumn.Long("clickOnConditionId", isNotNull = false)
    private val actionPressDurationColumn = SQLiteColumn.Long("pressDuration", isNotNull = false)
    private val actionClickOffsetXColumn = SQLiteColumn.Int("clickOffsetX", isNotNull = false)
    private val actionClickOffsetYColumn = SQLiteColumn.Int("clickOffsetY", isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new tables with their indexes
        db.getSQLiteTableReference(GESTURE_FINGER_TABLE, GestureFingerTableColumns.id).apply {
            createTable(columns = GestureFingerTableColumns.allColumns())
            createIndex(GestureFingerTableColumns.actionId)
        }
        db.getSQLiteTableReference(GESTURE_FINGER_MOTION_TABLE, GestureFingerMotionTableColumns.id).apply {
            createTable(columns = GestureFingerMotionTableColumns.allColumns())
            createIndex(GestureFingerMotionTableColumns.fingerId)
        }

        // Migrate to gestures
        db.migrateClicks()
        db.migrateSwipes()

        // Remove all swipe and click columns
        db.getSQLiteTableReference(ACTION_TABLE).alterTableDropColumn(
            droppedColumns = setOf(
                actionFromXColumn.name, actionFromYColumn.name, actionToXColumn.name, actionToYColumn.name,
                actionSwipeDurationColumn.name, actionClickPositionTypeColumn.name, actionXColumn.name,
                actionYColumn.name, actionClickOnConditionIdColumn.name, actionPressDurationColumn.name,
                actionClickOffsetXColumn.name, actionClickOffsetYColumn.name,
            )
        )
    }

    private fun SupportSQLiteDatabase.migrateClicks() {
        getSQLiteTableReference(ACTION_TABLE).forEachClick { legacyClickData ->
            val gestureFingerId = insertGestureFinger(legacyClickData.id)

            getSQLiteTableReference(GESTURE_FINGER_MOTION_TABLE)
                .recreateClickWithNewApi(gestureFingerId, legacyClickData)
        }
    }

    private fun SupportSQLiteDatabase.migrateSwipes() {
        val actionTable = getSQLiteTableReference(ACTION_TABLE)
        actionTable.forEachSwipe { legacySwipeData ->
            val gestureFingerId = insertGestureFinger(legacySwipeData.id)

            getSQLiteTableReference(GESTURE_FINGER_MOTION_TABLE)
                .recreateSwipeWithNewApi(gestureFingerId, legacySwipeData)
        }

        // Update all swipes to become gestures
        actionTable.update(
            extraClause = "WHERE `type` = \"${LEGACY_ACTION_TYPE_SWIPE}\"",
            contentValues = ContentValues().apply {
                put("type", ActionType.GESTURE.name)
            }
        )
    }

    private fun SupportSQLiteDatabase.insertGestureFinger(actionId: Long): Long =
        getSQLiteTableReference(GESTURE_FINGER_TABLE).insertIntoValues(
            ContentValues().apply {
                put(GestureFingerTableColumns.actionId.name, actionId)
                put("`${GestureFingerTableColumns.index.name}`", 0)
                put(GestureFingerTableColumns.startTimeMs.name, 0)
            }
        )

    private fun SQLiteTable.recreateClickWithNewApi(fingerId: Long, legacyClickData: LegacyClickData) {
        // Press, on the position the click was configured with
        insertIntoValues(
            ContentValues().apply {
                put(GestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${GestureFingerMotionTableColumns.index.name}`", 0)
                put(GestureFingerMotionTableColumns.motionType.name, GestureFingerMotionType.PRESS.name)
                put(GestureFingerMotionTableColumns.conditionId.name, legacyClickData.clickOnConditionId)

                when (legacyClickData.positionType) {
                    ClickPositionType.USER_SELECTED -> {
                        put(GestureFingerMotionTableColumns.xCoordinatesType.name, CoordinatesType.LITERAL.name)
                        put(GestureFingerMotionTableColumns.yCoordinatesType.name, CoordinatesType.LITERAL.name)
                        put(GestureFingerMotionTableColumns.xLiteral.name, legacyClickData.x)
                        put(GestureFingerMotionTableColumns.yLiteral.name, legacyClickData.y)
                    }

                    ClickPositionType.ON_DETECTED_CONDITION -> {
                        put(GestureFingerMotionTableColumns.xCoordinatesType.name, CoordinatesType.CONDITION.name)
                        put(GestureFingerMotionTableColumns.yCoordinatesType.name, CoordinatesType.CONDITION.name)

                        legacyClickData.clickOffsetX?.let { offsetX ->
                            put(GestureFingerMotionTableColumns.xOffsetCoordinatesType.name, CoordinatesType.LITERAL.name)
                            put(GestureFingerMotionTableColumns.xOffsetLiteral.name, offsetX)
                        }
                        legacyClickData.clickOffsetY?.let { offsetY ->
                            put(GestureFingerMotionTableColumns.yOffsetCoordinatesType.name, CoordinatesType.LITERAL.name)
                            put(GestureFingerMotionTableColumns.yOffsetLiteral.name, offsetY)
                        }
                    }
                }
            }
        )

        // Wait, for the duration the click was pressing the screen
        insertIntoValues(
            ContentValues().apply {
                put(GestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${GestureFingerMotionTableColumns.index.name}`", 1)
                put(GestureFingerMotionTableColumns.motionType.name, GestureFingerMotionType.WAIT.name)
                put(GestureFingerMotionTableColumns.durationMs.name, legacyClickData.pressDurationMs)
            }
        )

        // Release
        insertRelease(fingerId, index = 2)
    }

    private fun SQLiteTable.recreateSwipeWithNewApi(fingerId: Long, legacySwipeData: LegacySwipeData) {
        // Press
        insertIntoValues(
            ContentValues().apply {
                put(GestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${GestureFingerMotionTableColumns.index.name}`", 0)
                put(GestureFingerMotionTableColumns.motionType.name, GestureFingerMotionType.PRESS.name)
                put(GestureFingerMotionTableColumns.xCoordinatesType.name, CoordinatesType.LITERAL.name)
                put(GestureFingerMotionTableColumns.yCoordinatesType.name, CoordinatesType.LITERAL.name)
                put(GestureFingerMotionTableColumns.xLiteral.name, legacySwipeData.fromX)
                put(GestureFingerMotionTableColumns.yLiteral.name, legacySwipeData.fromY)
            }
        )

        // Move
        insertIntoValues(
            ContentValues().apply {
                put(GestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${GestureFingerMotionTableColumns.index.name}`", 1)
                put(GestureFingerMotionTableColumns.motionType.name, GestureFingerMotionType.MOVE.name)
                put(GestureFingerMotionTableColumns.xCoordinatesType.name, CoordinatesType.LITERAL.name)
                put(GestureFingerMotionTableColumns.yCoordinatesType.name, CoordinatesType.LITERAL.name)
                put(GestureFingerMotionTableColumns.xLiteral.name, legacySwipeData.toX)
                put(GestureFingerMotionTableColumns.yLiteral.name, legacySwipeData.toY)
                put(GestureFingerMotionTableColumns.durationMs.name, legacySwipeData.swipeDurationMs)
            }
        )

        // Release
        insertRelease(fingerId, index = 2)
    }

    private fun SQLiteTable.insertRelease(fingerId: Long, index: Int) {
        insertIntoValues(
            ContentValues().apply {
                put(GestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${GestureFingerMotionTableColumns.index.name}`", index)
                put(GestureFingerMotionTableColumns.motionType.name, GestureFingerMotionType.RELEASE.name)
            }
        )
    }

    private fun SQLiteTable.forEachClick(closure: (LegacyClickData) -> Unit) {
        select(
            extraClause = "WHERE `type` = \"${ActionType.CLICK.name}\"",
            distinct = false,
            actionIdColumn, actionClickPositionTypeColumn, actionXColumn, actionYColumn,
            actionClickOnConditionIdColumn, actionPressDurationColumn, actionClickOffsetXColumn,
            actionClickOffsetYColumn,
        ).use { queryResult ->
            queryResult.forEachRow { row ->
                val positionTypeName = row.getValue(actionClickPositionTypeColumn)
                val positionType = ClickPositionType.entries.find { it.name == positionTypeName }
                    ?: return@forEachRow

                closure(
                    LegacyClickData(
                        id = row.getValue(actionIdColumn) ?: return@forEachRow,
                        positionType = positionType,
                        x = row.getNullableValue(actionXColumn),
                        y = row.getNullableValue(actionYColumn),
                        clickOnConditionId = row.getNullableValue(actionClickOnConditionIdColumn),
                        pressDurationMs = row.getNullableValue(actionPressDurationColumn)?.takeIf { it > 0 }
                            ?: DEFAULT_PRESS_DURATION_MS,
                        clickOffsetX = row.getNullableValue(actionClickOffsetXColumn),
                        clickOffsetY = row.getNullableValue(actionClickOffsetYColumn),
                    )
                )
            }
        }
    }

    private fun SQLiteTable.forEachSwipe(closure: (LegacySwipeData) -> Unit) {
        select(
            extraClause = "WHERE `type` = \"${LEGACY_ACTION_TYPE_SWIPE}\"",
            distinct = false,
            actionIdColumn, actionPriorityColumn, actionNameColumn, actionFromXColumn,
            actionFromYColumn, actionToXColumn, actionToYColumn, actionSwipeDurationColumn,
        ).use { queryResult ->
            queryResult.forEachRow { row ->
                closure(
                    LegacySwipeData(
                        id = row.getValue(actionIdColumn) ?: return@forEachRow,
                        name = row.getValue(actionNameColumn) ?: return@forEachRow,
                        priority = row.getValue(actionPriorityColumn) ?: return@forEachRow,
                        fromX = row.getValue(actionFromXColumn) ?: return@forEachRow,
                        fromY = row.getValue(actionFromYColumn) ?: return@forEachRow,
                        toX = row.getValue(actionToXColumn) ?: return@forEachRow,
                        toY = row.getValue(actionToYColumn) ?: return@forEachRow,
                        swipeDurationMs = row.getValue(actionSwipeDurationColumn) ?: return@forEachRow,
                    )
                )
            }
        }
    }

    private data class LegacySwipeData(
        val id: Long,
        val name: String,
        val priority: Int,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val swipeDurationMs: Long,
    )

    private data class LegacyClickData(
        val id: Long,
        val positionType: ClickPositionType,
        val x: Int?,
        val y: Int?,
        val clickOnConditionId: Long?,
        val pressDurationMs: Long,
        val clickOffsetX: Int?,
        val clickOffsetY: Int?,
    )
}

private object GestureFingerTableColumns {

    val id = SQLiteColumn.PrimaryKey("id")
    val actionId = SQLiteColumn.ForeignKey(
        name = "action_id",
        referencedTable = ACTION_TABLE,
        referencedColumn = "id",
        deleteAction = ForeignKey.CASCADE,
    )

    val index = SQLiteColumn.Int("index")
    val startTimeMs = SQLiteColumn.Long("start_time_ms")


    fun allColumns(): Set<SQLiteColumn<*>> =
        setOf(actionId, index, startTimeMs)
}

private object GestureFingerMotionTableColumns {

    val id = SQLiteColumn.PrimaryKey("id")
    val fingerId = SQLiteColumn.ForeignKey(
        name = "finger_id",
        referencedTable = GESTURE_FINGER_TABLE,
        referencedColumn = "id",
        deleteAction = ForeignKey.CASCADE,
    )

    val index = SQLiteColumn.Int("index")
    val motionType = SQLiteColumn.Text("motion_type")
    val durationMs = SQLiteColumn.Long("duration_ms", isNotNull = false)
    val xCoordinatesType = SQLiteColumn.Text("x_coordinate_type", isNotNull = false)
    val yCoordinatesType = SQLiteColumn.Text("y_coordinate_type", isNotNull = false)
    val conditionId = SQLiteColumn.Long("condition_id", isNotNull = false)
    val xLiteral = SQLiteColumn.Int("x_literal", isNotNull = false)
    val xCounter = SQLiteColumn.Text("x_counter", isNotNull = false)
    val yLiteral = SQLiteColumn.Int("y_literal", isNotNull = false)
    val yCounter = SQLiteColumn.Text("y_counter", isNotNull = false)
    val xOffsetCoordinatesType = SQLiteColumn.Text("x_offset_coordinate_type", isNotNull = false)
    val yOffsetCoordinatesType = SQLiteColumn.Text("y_offset_coordinate_type", isNotNull = false)
    val xOffsetLiteral = SQLiteColumn.Int("x_offset_literal", isNotNull = false)
    val xOffsetCounter = SQLiteColumn.Text("x_offset_counter", isNotNull = false)
    val yOffsetLiteral = SQLiteColumn.Int("y_offset_literal", isNotNull = false)
    val yOffsetCounter = SQLiteColumn.Text("y_offset_counter", isNotNull = false)


    fun allColumns(): Set<SQLiteColumn<*>> =
        setOf(fingerId, index, motionType, durationMs, xCoordinatesType, yCoordinatesType, conditionId, xLiteral,
            xCounter, yLiteral, yCounter, xOffsetCoordinatesType, yOffsetCoordinatesType, xOffsetLiteral,
            xOffsetCounter, yOffsetLiteral, yOffsetCounter)
}
