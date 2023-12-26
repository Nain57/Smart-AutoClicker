/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.utils

import androidx.sqlite.db.SupportSQLiteDatabase

internal inline fun <reified T : Any> SupportSQLiteDatabase.execAddColumnStatement(
    @DatabaseTable tableName: String,
    columnName: String,
    defaultValue: T,
): Unit = execSQL(
    """
        ALTER TABLE `$tableName`
        ADD COLUMN `$columnName` ${T::class.toSQLiteType()} DEFAULT "$defaultValue" NOT NULL
    """.trimIndent()
)

internal fun SupportSQLiteDatabase.execAddNullableColumnStatement(
    @DatabaseTable tableName: String,
    columnName: String,
    @SQLiteType sqlType: String,
) : Unit = execSQL(
    """
        ALTER TABLE `$tableName`
        ADD COLUMN `$columnName` $sqlType DEFAULT NULL
    """.trimIndent()
)

internal fun SupportSQLiteDatabase.execAddNullableColumnsStatement(
    @DatabaseTable tableName: String,
    columns: Map<String, String>,
) : Unit = columns.forEach { (columnName, columnType) ->
    execAddNullableColumnStatement(tableName, columnName, columnType)
}