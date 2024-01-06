/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.base.migrations

import android.database.Cursor

class SQLiteQueryResult internal constructor(
    private val cursor: Cursor,
    private val columns: Collection<SQLiteColumn<*>>,
) : Cursor by cursor {

    private val columnsNamesToInfo: Map<String, Pair<SQLiteColumn<*>, Int>> = buildMap {
        if (count == 0) return@buildMap

        moveToFirst()
        columns.forEach { column ->
            val columnIndex = getColumnIndex(column.name)
            if (columnIndex < 0) throw IllegalStateException("Can't find column ${column.name}")

            put(column.name, column to columnIndex)
        }
    }

    fun forEachRow(closure: (Row) -> Unit) {
        if (isClosed || count == 0) return

        moveToFirst()
        do {
            closure(Row())
        } while (moveToNext())
    }

    inner class Row internal constructor() {

        fun getInt(columnName: String): Int = columnsNamesToInfo[columnName]?.second?.let(cursor::getInt)
            ?: throw IllegalArgumentException("Can't get Int value, column $columnName doesn't exist")

        fun getLong(columnName: String): Long = columnsNamesToInfo[columnName]?.second?.let(cursor::getLong)
            ?: throw IllegalArgumentException("Can't get Long value, column $columnName doesn't exist")

        fun getString(columnName: String): String = columnsNamesToInfo[columnName]?.second?.let(cursor::getString)
            ?: throw IllegalArgumentException("Can't get String value, column $columnName doesn't exist")

        fun getBoolean(columnName: String): Boolean = columnsNamesToInfo[columnName]?.second?.let { columnIndex ->
            cursor.getInt(columnIndex) != 0
        } ?: throw IllegalArgumentException("Can't get Boolean value, column $columnName doesn't exist")

        inline fun <reified ColumnType : Any> getValue(column: SQLiteColumn<ColumnType>): ColumnType =
            when (column) {
                is SQLiteColumn.Boolean -> getBoolean(column.name) as ColumnType
                is SQLiteColumn.Text -> getString(column.name) as ColumnType
                is SQLiteColumn.Int -> getInt(column.name) as ColumnType
                is SQLiteColumn.PrimaryKey,
                is SQLiteColumn.ForeignKey,
                is SQLiteColumn.Long -> getLong(column.name) as ColumnType
            }
    }
}

