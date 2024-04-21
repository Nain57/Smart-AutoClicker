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
package com.buzbuz.smartautoclicker.core.base.interfaces

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

interface Identifiable {
    val id: Identifier

    fun getDatabaseId(): Long = id.databaseId
    fun getDomainId(): Long? = id.tempId
    fun getValidId(): Long = if (isInDatabase()) id.databaseId else id.tempId
        ?: throw IllegalStateException("Identifier is invalid")
    fun isInDatabase(): Boolean = id.isInDatabase()
}

fun List<Identifiable>.containsIdentifiable(id: Identifier): Boolean =
    find { it.id == id } != null