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
package com.buzbuz.smartautoclicker.core.database.utils

import androidx.annotation.StringDef
import kotlin.reflect.KClass

/** Defines the supported types by the room database. */
@StringDef(TEXT, INTEGER)
@Retention(AnnotationRetention.SOURCE)
internal annotation class SQLiteType

internal const val TEXT = "TEXT"
internal const val INTEGER = "INTEGER"


@SQLiteType
internal fun <T : Any> KClass<T>.toSQLiteType(): String =
    when (this) {
        String::class -> TEXT

        Int::class,
        Long::class -> INTEGER

        else -> throw UnsupportedOperationException("This type is not supported ${this.simpleName}")
    }
