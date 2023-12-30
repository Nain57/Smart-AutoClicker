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

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed class SQLiteColumn<T : Any> {

    abstract val name: String
    abstract val type: KClass<T>
    abstract val isNotNull: Boolean

    internal val typeSQLLite: String
        get() = when  {
            type == Int::class || type == Long::class || type == Boolean::class-> "INTEGER"
            type == String::class || type.isSubclassOf(Enum::class) -> "TEXT"
            else -> throw UnsupportedOperationException("This type is not supported $this")
        }

    data class Default<T : Any>(
        override val name: String,
        override val type: KClass<T>,
        override val isNotNull: Boolean = true,
        val defaultValue: String? = null,
    ) : SQLiteColumn<T>()

    data class PrimaryKey(
        override val name: String = "id",
    ) : SQLiteColumn<Long>() {
        override val type: KClass<Long> = Long::class
        override val isNotNull: Boolean = true
    }

    data class ForeignKey<T : Any>(
        override val name: String,
        override val type: KClass<T>,
        override val isNotNull: Boolean = true,
        val referencedTable: String,
        val referencedColumn: String,
        @androidx.room.ForeignKey.Action val updateAction: Int = androidx.room.ForeignKey.NO_ACTION,
        @androidx.room.ForeignKey.Action val deleteAction: Int = androidx.room.ForeignKey.NO_ACTION,
    ) : SQLiteColumn<T>() {

        internal val updateActionSQLite: String
            get() = updateAction.toSQLiteForeignKeyAction()

        internal val deleteActionSQLite: String
            get() = deleteAction.toSQLiteForeignKeyAction()

        private fun Int.toSQLiteForeignKeyAction(): String =
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


