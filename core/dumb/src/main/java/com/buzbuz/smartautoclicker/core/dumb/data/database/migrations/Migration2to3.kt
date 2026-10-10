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
package com.buzbuz.smartautoclicker.core.dumb.data.database.migrations

import android.content.ContentValues
import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_ACTION_TABLE
import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_GESTURE_FINGER_MOTION_TABLE
import com.buzbuz.smartautoclicker.core.dumb.data.database.DUMB_GESTURE_FINGER_TABLE
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerMotionType


/**
 * Introduces the dumb gesture tables and moves the swipes into them.
 *
 * A swipe becomes a gesture with one finger doing a press, a move and a release. The swipe columns are then removed
 * from the dumb action table.
 */
object Migration2to3 : Migration(2, 3) {

    private const val LEGACY_ACTION_TYPE_SWIPE = "SWIPE"

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionFromXColumn = SQLiteColumn.Int("fromX", isNotNull = false)
    private val actionFromYColumn = SQLiteColumn.Int("fromY", isNotNull = false)
    private val actionToXColumn = SQLiteColumn.Int("toX", isNotNull = false)
    private val actionToYColumn = SQLiteColumn.Int("toY", isNotNull = false)
    private val actionSwipeDurationColumn = SQLiteColumn.Long("swipe_duration", isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(DUMB_GESTURE_FINGER_TABLE, DumbGestureFingerTableColumns.id).apply {
            createTable(columns = DumbGestureFingerTableColumns.allColumns())
            createIndex(DumbGestureFingerTableColumns.dumbActionId)
        }
        db.getSQLiteTableReference(DUMB_GESTURE_FINGER_MOTION_TABLE, DumbGestureFingerMotionTableColumns.id).apply {
            createTable(columns = DumbGestureFingerMotionTableColumns.allColumns())
            createIndex(DumbGestureFingerMotionTableColumns.fingerId)
        }

        db.migrateSwipes()

        db.getSQLiteTableReference(DUMB_ACTION_TABLE).alterTableDropColumn(
            droppedColumns = setOf(
                actionFromXColumn.name, actionFromYColumn.name, actionToXColumn.name, actionToYColumn.name,
                actionSwipeDurationColumn.name,
            )
        )
    }

    private fun SupportSQLiteDatabase.migrateSwipes() {
        val actionTable = getSQLiteTableReference(DUMB_ACTION_TABLE)
        actionTable.forEachSwipe { legacySwipeData ->
            val fingerId = insertGestureFinger(legacySwipeData.id)
            getSQLiteTableReference(DUMB_GESTURE_FINGER_MOTION_TABLE).insertSwipeMotions(fingerId, legacySwipeData)
        }

        actionTable.update(
            extraClause = "WHERE `type` = \"${LEGACY_ACTION_TYPE_SWIPE}\"",
            contentValues = ContentValues().apply {
                put("type", DumbActionType.GESTURE.name)
            }
        )
    }

    private fun SupportSQLiteDatabase.insertGestureFinger(actionId: Long): Long =
        getSQLiteTableReference(DUMB_GESTURE_FINGER_TABLE).insertIntoValues(
            ContentValues().apply {
                put(DumbGestureFingerTableColumns.dumbActionId.name, actionId)
                put("`${DumbGestureFingerTableColumns.index.name}`", 0)
                put(DumbGestureFingerTableColumns.startTimeMs.name, 0)
            }
        )

    private fun SQLiteTable.insertSwipeMotions(fingerId: Long, legacySwipeData: LegacySwipeData) {
        // Press
        insertIntoValues(
            ContentValues().apply {
                put(DumbGestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${DumbGestureFingerMotionTableColumns.index.name}`", 0)
                put(DumbGestureFingerMotionTableColumns.motionType.name, DumbGestureFingerMotionType.PRESS.name)
                put(DumbGestureFingerMotionTableColumns.x.name, legacySwipeData.fromX)
                put(DumbGestureFingerMotionTableColumns.y.name, legacySwipeData.fromY)
            }
        )

        // Move
        insertIntoValues(
            ContentValues().apply {
                put(DumbGestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${DumbGestureFingerMotionTableColumns.index.name}`", 1)
                put(DumbGestureFingerMotionTableColumns.motionType.name, DumbGestureFingerMotionType.MOVE.name)
                put(DumbGestureFingerMotionTableColumns.x.name, legacySwipeData.toX)
                put(DumbGestureFingerMotionTableColumns.y.name, legacySwipeData.toY)
                put(DumbGestureFingerMotionTableColumns.durationMs.name, legacySwipeData.swipeDurationMs)
            }
        )

        // Release
        insertIntoValues(
            ContentValues().apply {
                put(DumbGestureFingerMotionTableColumns.fingerId.name, fingerId)
                put("`${DumbGestureFingerMotionTableColumns.index.name}`", 2)
                put(DumbGestureFingerMotionTableColumns.motionType.name, DumbGestureFingerMotionType.RELEASE.name)
            }
        )
    }

    private fun SQLiteTable.forEachSwipe(closure: (LegacySwipeData) -> Unit) {
        select(
            extraClause = "WHERE `type` = \"${LEGACY_ACTION_TYPE_SWIPE}\"",
            distinct = false,
            actionIdColumn, actionFromXColumn, actionFromYColumn, actionToXColumn, actionToYColumn,
            actionSwipeDurationColumn,
        ).use { queryResult ->
            queryResult.forEachRow { row ->
                closure(
                    LegacySwipeData(
                        id = row.getValue(actionIdColumn) ?: return@forEachRow,
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
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val swipeDurationMs: Long,
    )
}

private object DumbGestureFingerTableColumns {

    val id = SQLiteColumn.PrimaryKey("id")
    val dumbActionId = SQLiteColumn.ForeignKey(
        name = "dumb_action_id",
        referencedTable = DUMB_ACTION_TABLE,
        referencedColumn = "id",
        deleteAction = ForeignKey.CASCADE,
    )

    val index = SQLiteColumn.Int("index")
    val startTimeMs = SQLiteColumn.Long("start_time_ms")

    fun allColumns(): Set<SQLiteColumn<*>> =
        setOf(dumbActionId, index, startTimeMs)
}

private object DumbGestureFingerMotionTableColumns {

    val id = SQLiteColumn.PrimaryKey("id")
    val fingerId = SQLiteColumn.ForeignKey(
        name = "finger_id",
        referencedTable = DUMB_GESTURE_FINGER_TABLE,
        referencedColumn = "id",
        deleteAction = ForeignKey.CASCADE,
    )

    val index = SQLiteColumn.Int("index")
    val motionType = SQLiteColumn.Text("motion_type")
    val durationMs = SQLiteColumn.Long("duration_ms", isNotNull = false)
    val x = SQLiteColumn.Int("x", isNotNull = false)
    val y = SQLiteColumn.Int("y", isNotNull = false)

    fun allColumns(): Set<SQLiteColumn<*>> =
        setOf(fingerId, index, motionType, durationMs, x, y)
}
