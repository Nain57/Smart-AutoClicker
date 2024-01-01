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
package com.buzbuz.smartautoclicker.core.base.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

class SQLiteTable internal constructor(
    private val databaseSQLite: SupportSQLiteDatabase, name: String,
    private val primaryKey: SQLiteColumn.PrimaryKey,
) {

    var tableName: String = name
        private set

    fun createTable(columns: Set<SQLiteColumn<*>>): Unit =
        execSQLite(getSQLiteCreateTable(primaryKey, columns))

    fun createIndex(foreignKey: SQLiteColumn.ForeignKey<*>, indexName: String? = null): Unit =
        execSQLite(getSQLiteCreateIndex(foreignKey, indexName))

    fun <T: Any> alterTableAddColumn(column: SQLiteColumn.Default<T>): Unit =
        execSQLite(getSQLiteAlterTableAddColumn(column))

    fun alterTableAddColumns(columns: Set<SQLiteColumn.Default<*>>): Unit =
        columns.forEach { alterTableAddColumn(it) }

    fun alterTableDropColumn(droppedColumns: Set<String>) {
        val tempTableDetails = databaseSQLite.getTableCopyDetails(
            originTableName = tableName,
            copyName = "${tableName}_new",
            filteredColumns = droppedColumns,
        )

        databaseSQLite.getSQLiteTableReference(tempTableDetails.tableName).apply {
            // Create temporary table
            execSQLite(tempTableDetails.sqliteCreateTable)
            // Copy all values from table into temp beside deleted columns
            insertIntoSelect(
                fromTableName = this@SQLiteTable.tableName,
                extraClause = null,
                columnsToFromColumns = tempTableDetails.columnNames.map { copyColumn(it) }.toTypedArray(),
            )
            // Delete actual table
            this@SQLiteTable.dropTable()
            // Create indexes for temp table
            tempTableDetails.sqliteCreateIndexes.forEach(::execSQLite)
            // Rename temp table into copied table
            alterTableRename(this@SQLiteTable.tableName)
        }
    }

    fun alterTableRename(newTableName: String) {
        execSQLite(getSQLiteAlterTableRename(newTableName))
        tableName = newTableName
    }

    fun insertIntoValues(vararg columnNamesToValues: Pair<String, String>): Long =
        insertSQLite(columnNamesToValues)

    fun insertIntoSelect(fromTableName: String, extraClause: String? = null, vararg columnsToFromColumns: Pair<String, String>): Unit =
        execSQLite(getSQLiteInsertIntoSelect(fromTableName, extraClause, columnsToFromColumns))

    fun update(extraClause: String?, vararg columnNamesToValues: Pair<SQLiteColumn<*>, String>): Unit =
        execSQLite(
            getSQLiteUpdate(
                extraClause,
                columnNamesToValues.map { it.first.name to it.second }.toTypedArray(),
            )
        )

    fun updateWithNames(extraClause: String?, vararg columnNamesToValues: Pair<String, String>): Unit =
        execSQLite(getSQLiteUpdate(extraClause, columnNamesToValues))

    fun deleteFrom(primaryKeys: Set<Long>): Unit =
        execSQLite(getSQLiteDeleteFrom(primaryKeys))

    fun select(extraClause: String? = null, vararg columns: SQLiteColumn<*>) : SQLiteQueryResult =
        SQLiteQueryResult(
            cursor = querySQLite(
                getSQLiteSelect(
                    columns = columns.map { it.name },
                    extraClause = extraClause,
                )
            ),
            columns = columns.asList(),
        )

    fun dropTable(): Unit =
        execSQLite(getSQLiteDropTable())

    fun copyTable(copyName: String= "${tableName}_new", droppedColumns: Collection<String>): Pair<SQLiteTable, Collection<String>> {
        val tempTableDetails = databaseSQLite.getTableCopyDetails(
            originTableName = tableName,
            copyName = copyName,
            filteredColumns = droppedColumns,
        )

        val copyTable = databaseSQLite.getSQLiteTableReference(tempTableDetails.tableName).apply {
            // Create temporary table
            execSQLite(tempTableDetails.sqliteCreateTable)
            // Copy all values from table into temp beside deleted columns
            insertIntoSelect(
                fromTableName = this@SQLiteTable.tableName,
                extraClause = null,
                columnsToFromColumns = tempTableDetails.columnNames.map { copyColumn(it) }.toTypedArray(),
            )
        }

        return copyTable to tempTableDetails.sqliteCreateIndexes
    }

    fun execSQLite(statement: String) {
        Log.d(TAG, "Executing: $statement")
        databaseSQLite.execSQL(statement)
    }

    private fun querySQLite(query: String): Cursor {
        Log.d(TAG, "Executing: $query")
        return databaseSQLite.query(query)
    }

    private fun insertSQLite(columnNamesToValues: Array<out Pair<String, String>>): Long {
        Log.d(TAG, "Inserting into $tableName: $columnNamesToValues")

        val contentValues = ContentValues().apply {
            columnNamesToValues.forEach { (column, value) -> put(column, value) }
        }
        return databaseSQLite.insert(tableName, CONFLICT_FAIL, contentValues)
    }
}

private const val TAG = "SQLiteTable"