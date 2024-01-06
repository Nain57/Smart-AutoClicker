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

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Contains all details for the creation of a specific SQLite table.
 * Can be instantiated by using [SQLiteTableDetails.getFrom].
 */
internal class SQLiteTableDetails private constructor(
    val sqliteCreateTable: String,
    val sqliteCreateIndexes: Collection<String>,
    val tableName: String,
    val columnNames: Set<String>,
) {

    companion object {

        internal fun getFrom(
            database: SupportSQLiteDatabase,
            originTableName: String,
            copyName: String,
            filteredColumns: Collection<String>,
        ): SQLiteTableDetails {

            var sqlCreateTable: String? = null
            val sqlCreateIndexes: MutableList<String> = mutableListOf()
            val sqlKeptColumns: MutableSet<String> = mutableSetOf()

            // Fetch the copied table create information in 'sqlite_master' table.
            // Column 'tbl_name' contains the names of all tables, and column 'sql' the sql to execute to create them.
            database.getSQLiteTableReference("sqlite_master").forEachRow(
                extraClause = "WHERE `tbl_name` = \"$originTableName\"",
                columnA = SQLiteColumn.Text("sql")
            ) { rawSql ->

                val sql = rawSql.trim()
                when {
                    // This is the table creation
                    sql.startsWith("CREATE TABLE") -> {
                        // This should not happen but let's be sure
                        if (sqlCreateTable != null)
                            throw IllegalStateException("There is two create table for $originTableName")

                        // Parse the columns between '( )', they are all split with ','.
                        // For each column, parse the column name, or filter if requested
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

                    // This is the table indexes creation
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
    }
}
