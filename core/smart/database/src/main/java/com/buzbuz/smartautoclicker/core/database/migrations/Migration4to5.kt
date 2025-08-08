
package com.buzbuz.smartautoclicker.core.database.migrations

import android.content.ContentValues
import androidx.annotation.VisibleForTesting
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.migrations.forEachRow
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE

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

    private val conditionIdColumn = SQLiteColumn.PrimaryKey()
    private val conditionThresholdColumn = SQLiteColumn.Int("threshold")

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(CONDITION_TABLE).apply {
            addConditionsColumns()

            forEachRow(null, conditionIdColumn, conditionThresholdColumn) { id, threshold ->
                updateThreshold(
                    id = id,
                    threshold = (threshold + THRESHOLD_INCREASE).coerceAtMost(THRESHOLD_MAX_VALUE),
                )
            }
        }
    }

    private fun SQLiteTable.addConditionsColumns() = apply {
        alterTableAddColumn(SQLiteColumn.Text("name", defaultValue = "Condition"))
        alterTableAddColumn(SQLiteColumn.Int("detection_type", defaultValue = "1"))
    }

    private fun SQLiteTable.updateThreshold(id: Long, threshold: Int) =
        update(
            extraClause = "WHERE `id` = $id",
            contentValues = ContentValues().apply {
                put(conditionThresholdColumn.name, threshold)
            }
        )
}
