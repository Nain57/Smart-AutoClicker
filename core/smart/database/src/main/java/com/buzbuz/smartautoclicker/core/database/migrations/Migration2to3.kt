
package com.buzbuz.smartautoclicker.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference

/**
 * Migration from database v2 to v3.
 * Changes: clicks have now an optional amount of executions before the scenario is stopped.
 */
object Migration2to3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference("click_table")
            .alterTableAddColumn(SQLiteColumn.Int("stop_after", isNotNull = false))
    }
}