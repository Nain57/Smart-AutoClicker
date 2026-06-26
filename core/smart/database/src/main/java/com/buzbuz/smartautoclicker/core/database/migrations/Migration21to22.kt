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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE

/**
 * Migration from database v21 to v22.
 *
 * Hotfix: removes blank counters (counterName is empty or whitespace-only) that were incorrectly
 * created during Migration19to20 when a v19 database had empty-string values in counter-name
 * columns instead of NULL.
 */
object Migration21to22 : Migration(21, 22) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `$COUNTERS_TABLE` WHERE length(trim(`counterName`)) = 0")
    }
}
