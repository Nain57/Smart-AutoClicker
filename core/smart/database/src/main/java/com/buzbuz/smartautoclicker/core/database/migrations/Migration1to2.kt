
package com.buzbuz.smartautoclicker.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.migrations.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.migrations.getSQLiteTableReference
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE

/**
 * Migration from database v1 to v2.
 * Changes: conditions have now a threshold, allowing to detect images close to the detection one.
 */
object Migration1to2 : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference(CONDITION_TABLE)
            .alterTableAddColumn(SQLiteColumn.Int("threshold", defaultValue = "1"))
    }
}