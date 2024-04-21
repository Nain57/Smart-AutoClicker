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

    fun createTable(columns: Set<SQLiteColumn<*>>): Unit = execSQLite(
        """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                ${primaryKey.formatAsSQLiteCreateTablePrimaryKeyColumn()},
                ${columns.formatAsSQLiteCreateTableColumnList()}
            )
        """.trimIndent()
    )

    fun createIndex(foreignKey: SQLiteColumn.ForeignKey, indexName: String? = null): Unit = execSQLite(
        """
            CREATE INDEX IF NOT EXISTS `${foreignKey.formatAsRoomDefaultIndexName(tableName, indexName)}` 
                ON `$tableName` (`${foreignKey.name}`)
        """.trimIndent()
    )

    fun <T: Any> alterTableAddColumn(column: SQLiteColumn.Default<T>): Unit = execSQLite(
        """
            ALTER TABLE `$tableName`
            ADD COLUMN `${column.name}` ${column.typeSQLLite}  
            ${if (column.isNotNull) "DEFAULT ${column.defaultValue!!} NOT NULL" else ""}
        """.trimIndent()
    )

    fun alterTableAddColumns(columns: Set<SQLiteColumn.Default<*>>): Unit =
        columns.forEach { alterTableAddColumn(it) }

    fun alterTableDropColumn(droppedColumns: Set<String>) {
        val tempTableDetails = SQLiteTableDetails.getFrom(
            database = databaseSQLite,
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
        execSQLite("""
            ALTER TABLE `$tableName` RENAME TO $newTableName
        """.trimIndent())
        tableName = newTableName
    }

    fun insertIntoValues(contentValues: ContentValues): Long =
        insertSQLite(contentValues)

    fun insertIntoSelect(
        fromTableName: String, extraClause: String? = null,
        vararg columnsToFromColumns: Pair<String, String>,
    ): Unit = execSQLite(
        """
            INSERT INTO `$tableName` (${columnsToFromColumns.map { it.first }.formatAsSQLiteList()})
            SELECT ${columnsToFromColumns.map { it.second }.formatAsSQLiteList()}
            FROM `$fromTableName`
            ${extraClause.formatAsOptionalClause()}
        """.trimIndent()
    )

    fun update(extraClause: String?, contentValues: ContentValues): Unit = execSQLite(
        """
            UPDATE `$tableName` 
            SET ${contentValues.valueSet().map { it.key to (it.value?.toString() ?: "NULL") }.formatAsSQLiteUpdateList()}
            ${extraClause.formatAsOptionalClause()}
        """.trimIndent()
    )

    fun deleteFrom(primaryKeys: Set<Long>): Unit = execSQLite(
        """
            DELETE FROM `$tableName` 
            WHERE `id` IN (${primaryKeys.formatAsSQLiteList()})
        """.trimIndent()
    )

    fun select(extraClause: String? = null, vararg columns: SQLiteColumn<*>) : SQLiteQueryResult = SQLiteQueryResult(
        cursor = querySQLite("""
            SELECT ${columns.map { it.name }.formatAsSQLiteList()}
            FROM `$tableName`
            ${extraClause.formatAsOptionalClause()}
        """.trimIndent()),
        columns = columns.asList(),
    )

    fun dropTable(): Unit = execSQLite(
        """
            DROP TABLE IF EXISTS `$tableName`
        """.trimIndent()
    )

    fun copyTable(
        copyName: String= "${tableName}_new",
        droppedColumns: Collection<String>,
        withValues: Boolean = true,
    ): Pair<SQLiteTable, Collection<String>> {
        val tempTableDetails = SQLiteTableDetails.getFrom(
            database = databaseSQLite,
            originTableName = tableName,
            copyName = copyName,
            filteredColumns = droppedColumns,
        )

        val copyTable = databaseSQLite.getSQLiteTableReference(tempTableDetails.tableName).apply {
            // Create temporary table
            execSQLite(tempTableDetails.sqliteCreateTable)

            // Copy all values from table into temp beside deleted columns if requested
            if (withValues) {
                insertIntoSelect(
                    fromTableName = this@SQLiteTable.tableName,
                    extraClause = null,
                    columnsToFromColumns = tempTableDetails.columnNames.map { copyColumn(it) }.toTypedArray(),
                )
            }
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

    private fun insertSQLite(contentValues: ContentValues): Long {
        Log.d(TAG, "Inserting into $tableName: $contentValues")
        return databaseSQLite.insert(tableName, CONFLICT_FAIL, contentValues)
    }
}

private const val TAG = "SQLiteTable"