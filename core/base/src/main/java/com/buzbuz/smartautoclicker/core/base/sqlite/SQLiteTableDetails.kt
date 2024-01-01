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

import androidx.sqlite.db.SupportSQLiteDatabase

internal data class SQLiteTableDetails(
    val sqliteCreateTable: String,
    val sqliteCreateIndexes: Collection<String>,
    val tableName: String,
    val columnNames: Set<String>,
)

internal fun SupportSQLiteDatabase.getTableCopyDetails(
    originTableName: String,
    copyName: String,
    filteredColumns: Collection<String>,
): SQLiteTableDetails {

    var sqlCreateTable: String? = null
    val sqlCreateIndexes: MutableList<String> = mutableListOf()
    val sqlKeptColumns: MutableSet<String> = mutableSetOf()

    val sqliteMasterTable = getSQLiteTableReference("sqlite_master")
    val sqlColumn = SQLiteColumn.Default("sql", String::class)

    sqliteMasterTable.forEachRow("WHERE `tbl_name` = \"$originTableName\"", sqlColumn) { rawSql ->
        val sql = rawSql.trim()
        when {
            sql.startsWith("CREATE TABLE") -> {
                if (sqlCreateTable != null)
                    throw IllegalStateException("There is two create table for $originTableName")

                val createTableParams =
                    buildList {
                        sql.substring(sql.indexOfFirst { it == '(' } + 1, sql.indexOfLast { it == ')' })
                            .split(",")
                            .forEach { rawSQLParam ->
                                rawSQLParam.trim().let { sqlParam ->
                                    val columnName = sqlParam.getColumnNameFromSQLiteCreateParam()
                                    if (filteredColumns.contains(columnName)) return@forEach

                                    sqlKeptColumns.add(columnName)
                                    add(sqlParam)
                                }
                            }
                    }.formatAsSQLiteList()

                sqlCreateTable = "CREATE TABLE `$copyName` ( $createTableParams )"
            }

            sql.startsWith("CREATE INDEX") -> {
                val columnName = sql.getColumnNameFromSQLiteCreateIndex()
                if (filteredColumns.contains(columnName)) return@forEachRow

                sqlCreateIndexes.add(sql.replace("`$originTableName`", "`$copyName`"))
            }
        }
    }

    val sqlCreateTempTable =
        if (sqlCreateTable.isNullOrEmpty()) throw IllegalStateException("Can't drop column for $originTableName")
        else sqlCreateTable!!.replace("CREATE TABLE `$originTableName`", "CREATE TABLE `$copyName`")

    return SQLiteTableDetails(sqlCreateTempTable, sqlCreateIndexes, copyName, sqlKeptColumns)
}

private fun String.getColumnNameFromSQLiteCreateParam(): String {
    val firstIndexOfQuote = indexOf("`")
    val secondIndexOfQuote = indexOf("`", firstIndexOfQuote + 1)

    return substring(firstIndexOfQuote + 1, secondIndexOfQuote)
}

private fun String.getColumnNameFromSQLiteCreateIndex(): String {
    val lastIndexOfQuote = lastIndexOf("`")
    val beforeLastIndexOfQuote = lastIndexOf("`", lastIndexOfQuote - 1)

    return substring(beforeLastIndexOfQuote + 1, lastIndexOfQuote)
}