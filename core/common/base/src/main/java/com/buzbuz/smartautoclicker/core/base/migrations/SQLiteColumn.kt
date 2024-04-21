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
package com.buzbuz.smartautoclicker.core.base.migrations

sealed class SQLiteColumn<T : Any> {

    abstract val name: String
    abstract val isNotNull: kotlin.Boolean
    internal abstract val typeSQLLite: String
    sealed class Default<T : Any> : SQLiteColumn<T>() {
        abstract val defaultValue: String?
    }


    data class Int(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Int>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Long(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Long>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Boolean(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<kotlin.Boolean>() {
        override val typeSQLLite: String = "INTEGER"
    }

    data class Text(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        override val defaultValue: String? = null,
    ) : Default<String>() {
        override val typeSQLLite: String = "TEXT"
    }

    data class PrimaryKey(
        override val name: String = "id",
    ) : SQLiteColumn<kotlin.Long>() {
        override val typeSQLLite: String = "INTEGER"
        override val isNotNull: kotlin.Boolean = true
    }

    data class ForeignKey(
        override val name: String,
        override val isNotNull: kotlin.Boolean = true,
        val referencedTable: String,
        val referencedColumn: String,
        @androidx.room.ForeignKey.Action val updateAction: kotlin.Int = androidx.room.ForeignKey.NO_ACTION,
        @androidx.room.ForeignKey.Action val deleteAction: kotlin.Int = androidx.room.ForeignKey.NO_ACTION,
    ) : SQLiteColumn<kotlin.Long>() {

        override val typeSQLLite: String = "INTEGER"

        internal val updateActionSQLite: String
            get() = updateAction.toSQLiteForeignKeyAction()

        internal val deleteActionSQLite: String
            get() = deleteAction.toSQLiteForeignKeyAction()

        private fun kotlin.Int.toSQLiteForeignKeyAction(): String =
            when (this) {
                androidx.room.ForeignKey.NO_ACTION -> "NO ACTION"
                androidx.room.ForeignKey.RESTRICT -> "RESTRICT"
                androidx.room.ForeignKey.SET_NULL -> "SET NULL"
                androidx.room.ForeignKey.SET_DEFAULT -> "SET DEFAULT"
                androidx.room.ForeignKey.CASCADE -> "CASCADE"
                else -> throw UnsupportedOperationException("Invalid foreign key action")
            }
    }
}


