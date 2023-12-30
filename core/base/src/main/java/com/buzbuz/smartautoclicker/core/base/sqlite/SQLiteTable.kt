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
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase


fun SupportSQLiteDatabase.getTable(tableName: String) =
    SQLiteTable(this, tableName)

/** Convenience method to simply copy a value between tables with the same column name during [getSQLiteInsertIntoSelect] */
fun keepColumn(column: String): Pair<String, String> =
    column to column

class SQLiteTable internal constructor(private val databaseSQLite: SupportSQLiteDatabase, name: String) {

    var tableName: String = name
        private set

    fun createTable(primaryKey: SQLiteColumn.PrimaryKey, columns: Set<SQLiteColumn<*>>): Unit =
        execSQLite(getSQLiteCreateTable(primaryKey, columns))

    fun createIndex(foreignKey: SQLiteColumn.ForeignKey<*>, indexName: String? = null): Unit =
        execSQLite(getSQLiteCreateIndex(foreignKey, indexName))

    fun <T: Any> alterTableAddColumn(column: SQLiteColumn.Default<T>): Unit =
        execSQLite(getSQLiteAlterTableAddColumn(column))

    fun alterTableDropColumn(columnName: String): Unit =
        execSQLite(getSQLiteAlterTableDropColumn(columnName))

    fun alterTableRename(newTableName: String) {
        execSQLite(getSQLiteAlterTableRename(newTableName))
        tableName = newTableName
    }

    fun insertIntoValues(vararg columnNamesToValues: Pair<String, String>): Unit =
        execSQLite(getSQLiteInsertIntoValues(*columnNamesToValues))

    fun insertIntoSelect(fromTableName: String, extraClause: String? = null, vararg columnsToFromColumns: Pair<String, String>): Unit =
        execSQLite(getSQLiteInsertIntoSelect(fromTableName, extraClause, *columnsToFromColumns))

    fun update(extraClause: String?, vararg columnNamesToValues: Pair<String, String>): Unit =
        execSQLite(getSQLiteUpdate(extraClause, *columnNamesToValues))

    fun deleteFrom(primaryKeys: Set<Long>): Unit =
        execSQLite(getSQLiteDeleteFrom(primaryKeys))

    fun select(columns: Set<String>, extraClause: String? = null, closure: (SQLiteQueryRow) -> Unit) {
        SQLiteQueryResult(
            cursor = querySQLite(getSQLiteSelect(columns, extraClause)),
            columnNames = columns,
        ).use { queryResult -> queryResult.forEachRow(closure) }
    }

    fun dropTable(): Unit =
        execSQLite(getSQLiteDropTable())

    private fun execSQLite(statement: String) {
        Log.d(TAG, "Executing: $this")
        println(statement)
        databaseSQLite.execSQL(statement)
    }

    private fun querySQLite(query: String): Cursor {
        Log.d(TAG, "Executing: $this")
        println(query)
        return databaseSQLite.query(query)
    }
}

private const val TAG = "SQLiteTable"