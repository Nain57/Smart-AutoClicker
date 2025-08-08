
package com.buzbuz.smartautoclicker.core.base.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

fun SupportSQLiteDatabase.getSQLiteTableReference(
    tableName: String, primaryKey:
    SQLiteColumn.PrimaryKey = SQLiteColumn.PrimaryKey(),
) = SQLiteTable(this, tableName, primaryKey)

/**
 * Convenience method to simply copy a value between tables with the same column name
 * during [SQLiteTable.insertIntoSelect]
 */
fun copyColumn(column: String): Pair<String, String> =
    column to column

inline fun <reified A : Any> SQLiteTable.forEachRow(
    extraClause: String? = null,
    columnA: SQLiteColumn<A>,
    crossinline closure: (A) -> Unit,
): Unit =
    select(extraClause, columnA).use { queryResult -> queryResult.forEachRow { row -> closure(row.getValue(columnA)) } }

inline fun <reified A : Any, reified B : Any> SQLiteTable.forEachRow(
    extraClause: String? = null,
    columnA: SQLiteColumn<A>,
    columnB: SQLiteColumn<B>,
    crossinline closure: (A, B) -> Unit,
): Unit =
    select(extraClause, columnA, columnB).use { queryResult ->
        queryResult.forEachRow { row -> closure(row.getValue(columnA), row.getValue(columnB)) }
    }

inline fun <reified A : Any, reified B : Any, reified C : Any> SQLiteTable.forEachRow(
    extraClause: String? = null,
    columnA: SQLiteColumn<A>,
    columnB: SQLiteColumn<B>,
    columnC: SQLiteColumn<C>,
    crossinline closure: (A, B, C) -> Unit,
): Unit =
    select(extraClause, columnA, columnB, columnC).use { queryResult ->
        queryResult.forEachRow { row -> closure(row.getValue(columnA), row.getValue(columnB), row.getValue(columnC)) }
    }

inline fun <reified A : Any, reified B : Any, reified C : Any, reified D : Any> SQLiteTable.forEachRow(
    extraClause: String? = null,
    columnA: SQLiteColumn<A>,
    columnB: SQLiteColumn<B>,
    columnC: SQLiteColumn<C>,
    columnD: SQLiteColumn<D>,
    crossinline closure: (A, B, C, D) -> Unit,
): Unit =
    select(extraClause, columnA, columnB, columnC, columnD).use { queryResult ->
        queryResult.forEachRow { row ->
            closure(row.getValue(columnA), row.getValue(columnB), row.getValue(columnC), row.getValue(columnD))
        }
    }