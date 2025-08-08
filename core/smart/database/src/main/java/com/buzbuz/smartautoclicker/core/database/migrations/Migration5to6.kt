
package com.buzbuz.smartautoclicker.core.database.migrations

import android.content.ContentValues
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionType

/**
 * Migration from database v5 to v6.
 *
 * Changes:
 * * add clickOnCondition to the action click table.
 * * add shouldBeDetected to the condition table in order to allow condition negation.
 */
object Migration5to6 : Migration(5, 6) {

    private val conditionShouldBeDetectedColumn =
        SQLiteColumn.Boolean("shouldBeDetected", defaultValue = "1")

    private val actionIdColumn = SQLiteColumn.PrimaryKey()
    private val actionTypeColumn = SQLiteColumn.Text("type")
    private val actionClickOnConditionColumn = SQLiteColumn.Int("clickOnCondition", isNotNull = false)

    override fun migrate(db: SupportSQLiteDatabase) {
        db.apply {
            getSQLiteTableReference(CONDITION_TABLE).alterTableAddColumn(conditionShouldBeDetectedColumn)

            getSQLiteTableReference(ACTION_TABLE).apply {
                alterTableAddColumn(actionClickOnConditionColumn)

                forEachRow(null, actionIdColumn, actionTypeColumn) { actionId, actionType ->
                    if (ActionType.valueOf(actionType) == ActionType.CLICK)
                        updateClickOnCondition(actionId, 0)
                }
            }
        }
    }

    /** Update the click on condition value in the action table. */
    private fun SQLiteTable.updateClickOnCondition(id: Long, clickOnCondition: Int): Unit =
        update(
            extraClause = "WHERE `id` = $id",
            contentValues = ContentValues().apply {
                put(actionClickOnConditionColumn.name, clickOnCondition)
            },
        )
}
