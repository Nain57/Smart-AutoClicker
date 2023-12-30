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

internal fun <T: Any> SQLiteTable.getSQLiteAlterTableAddColumn(column: SQLiteColumn.Default<T>): String =
    """
        ALTER TABLE `$tableName`
        ADD COLUMN `${column.name}` ${column.typeSQLLite} 
        DEFAULT ${if (column.isNotNull) "${column.defaultValue!!} NOT NULL" else "NULL"}
    """.trimIndent()

internal fun SQLiteTable.getSQLiteAlterTableRename(newTableName: String): String =
    """
        ALTER TABLE `$tableName` RENAME TO $newTableName
    """.trimIndent()

internal fun SQLiteTable.getSQLiteAlterTableDropColumn(columnName: String): String =
    """
        ALTER TABLE `$tableName` DROP COLUMN `$columnName`"
    """.trimIndent()

internal fun SQLiteTable.getSQLiteCreateTable(
    primaryKey: SQLiteColumn.PrimaryKey,
    columns: Set<SQLiteColumn<*>>,
): String =
    """
        CREATE TABLE IF NOT EXISTS `$tableName` (
            ${primaryKey.formatAsSQLiteCreateTablePrimaryKeyColumn()},
            ${columns.formatAsSQLiteCreateTableColumnList()}
        )
    """.trimIndent()

internal fun SQLiteTable.getSQLiteCreateIndex(foreignKey: SQLiteColumn.ForeignKey<*>, indexName: String?): String =
    """
        CREATE INDEX IF NOT EXISTS `${if (indexName.isNullOrEmpty()) formatAsRoomDefaultIndexName(foreignKey) else indexName}` ON `$tableName` (`${foreignKey.name}`)
    """.trimIndent()

internal fun SQLiteTable.getSQLiteDeleteFrom(primaryKeys: Set<Long>): String =
    """
        DELETE FROM `$tableName` 
        WHERE `id` IN (${primaryKeys.formatAsSQLiteList()})
    """.trimIndent()

internal fun SQLiteTable.getSQLiteDropTable(): String =
    """
        DROP TABLE IF EXISTS `$tableName`
    """.trimIndent()

internal fun SQLiteTable.getSQLiteInsertIntoSelect(
    fromTableName: String,
    extraClause: String? = null,
    vararg columnsToFromColumns: Pair<String, String>,
): String =
    """
        INSERT INTO `$tableName` (${columnsToFromColumns.map { it.first }.formatAsSQLiteList()})
        SELECT ${columnsToFromColumns.map { it.second }.formatAsSQLiteList()}
        FROM `$fromTableName`
        ${extraClause.formatAsOptionalClause()}
    """

internal fun SQLiteTable.getSQLiteInsertIntoValues(vararg columnNamesToValues: Pair<String, String>): String =
    """
        INSERT INTO `$tableName` (${columnNamesToValues.map { it.first }.formatAsSQLiteList()})
        VALUES (${columnNamesToValues.map { it.second }.formatAsSQLiteList()})
    """.trimIndent()

internal fun SQLiteTable.getSQLiteSelect(columns: Set<String>, extraClause: String?): String =
    """
        SELECT ${columns.formatAsSQLiteList()}
        FROM `$tableName`
        ${extraClause.formatAsOptionalClause()}
    """.trimIndent()

internal fun SQLiteTable.getSQLiteUpdate(
    extraClause: String?,
    vararg columnNamesToValues: Pair<String, String>,
): String =
    """
        UPDATE `$tableName` 
        SET ${columnNamesToValues.asList().formatAsSQLiteUpdateList()}
        ${extraClause.formatAsOptionalClause()}
    """