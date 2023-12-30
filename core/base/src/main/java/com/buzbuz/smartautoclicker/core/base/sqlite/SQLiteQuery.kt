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
package com.buzbuz.smartautoclicker.core.base.sqlite

import android.database.Cursor

internal class SQLiteQueryResult(
    private val cursor: Cursor,
    columnNames: Set<String>,
) : Cursor by cursor {

    private val columnsNamesToIndex: Map<String, Int> = buildMap {
        if (count == 0) return@buildMap

        moveToFirst()
        columnNames.forEach { columnName ->
            val columnIndex = getColumnIndex(columnName)
            if (columnIndex < 0) throw IllegalStateException("Can't find column $columnName")

            put(columnName, columnIndex)
        }
    }

    fun forEachRow(closure: (SQLiteQueryRow) -> Unit) {
        if (isClosed || count == 0) return

        moveToFirst()
        do {
            closure(SQLiteQueryRow(cursor, columnsNamesToIndex))
        } while (moveToNext())
    }
}

class SQLiteQueryRow internal constructor(
    private val cursor: Cursor,
    private val columnsNamesToIndex: Map<String, Int>,
) {

    fun getInt(columnName: String): Int = columnsNamesToIndex[columnName]?.let(cursor::getInt)
        ?: throw IllegalArgumentException("Can't get Int value, column $columnName doesn't exist")

    fun getLong(columnName: String): Long = columnsNamesToIndex[columnName]?.let(cursor::getLong)
        ?: throw IllegalArgumentException("Can't get Long value, column $columnName doesn't exist")

    fun getString(columnName: String): String = columnsNamesToIndex[columnName]?.let(cursor::getString)
        ?: throw IllegalArgumentException("Can't get String value, column $columnName doesn't exist")

    fun getBoolean(columnName: String): Boolean = columnsNamesToIndex[columnName]?.let { columnIndex ->
        cursor.getInt(columnIndex) != 0
    } ?: throw IllegalArgumentException("Can't get Boolean value, column $columnName doesn't exist")

    inline fun <reified T: Enum<T>> getEnumValue(columnName: String): T = enumValueOf(getString(columnName))
}