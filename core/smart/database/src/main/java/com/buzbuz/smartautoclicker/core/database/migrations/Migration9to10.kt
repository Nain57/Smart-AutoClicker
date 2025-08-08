
package com.buzbuz.smartautoclicker.core.database.migrations

import android.content.ContentValues
import androidx.annotation.VisibleForTesting
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType

import kotlin.math.min

/**
 * Migration from database v9 to v10.
 *
 * This migration has no changes in the database table. It is made to fix a bug that crash the application if a gesture
 * duration is over 1 minute.
 * To fix this crash, we need to add this limitation in the ui, in the scenario import and then ensure all gestures
 * already in database are limited to 1 minute.
 */
object Migration9to10 : Migration(9, 10) {

    /** The new limit for the gesture duration in milliseconds. */
    @VisibleForTesting
    internal const val NEW_GESTURE_DURATION_LIMIT_MS = 59999L

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionTypeColumn = SQLiteColumn.Text("type")
    private val actionPressDurationColumn = SQLiteColumn.Long("pressDuration")
    private val actionSwipeDurationColumn = SQLiteColumn.Long("swipeDuration")

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(ACTION_TABLE).apply {
            forEachClicksAndSwipes { id, type, pressDuration, swipeDuration ->
                when (ActionType.valueOf(type)) {
                    ActionType.CLICK -> updateClickPressDuration(id, pressDuration.toLimitedDuration())
                    ActionType.SWIPE -> updateSwipeDuration(id, swipeDuration.toLimitedDuration())
                    else -> throw IllegalArgumentException("It should be a click or a swipe")
                }
            }
        }
    }

    private fun SQLiteTable.forEachClicksAndSwipes(closure: (Long, String, Long, Long) -> Unit) {
        forEachRow(
            extraClause = "WHERE `type` = \"${ActionType.CLICK}\" OR `type` = \"${ActionType.SWIPE}\"",
            actionIdColumn,
            actionTypeColumn,
            actionPressDurationColumn,
            actionSwipeDurationColumn,
            closure = closure,
        )
    }

    private fun SQLiteTable.updateClickPressDuration(actionId: Long, duration: Long) =
        update(
            extraClause = "WHERE `id` = $actionId",
            contentValues = ContentValues().apply {
                put(actionPressDurationColumn.name, duration)
            }
        )

    private fun SQLiteTable.updateSwipeDuration(actionId: Long, duration: Long) =
        update(
            extraClause = "WHERE `id` = $actionId",
            contentValues = ContentValues().apply {
                put(actionSwipeDurationColumn.name, duration)
            }
        )

    private fun Long.toLimitedDuration(): Long =
        min(this, NEW_GESTURE_DURATION_LIMIT_MS)
}
