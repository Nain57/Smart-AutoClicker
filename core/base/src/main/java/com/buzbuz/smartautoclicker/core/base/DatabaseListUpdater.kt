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
package com.buzbuz.smartautoclicker.core.base

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Helper class to update a list in the database.
 *
 * When updating a complete list of entities in a database, you have the find which items will be added, updated or
 * removed. This class provides this information using the method [refreshUpdateValues] to parse the list, and the
 * [toBeAdded], [toBeUpdated] and [toBeRemoved] list to get the information.
 *
 * @param I type of the items in the list to be updated.
 * @param E type of the entities in the database.
 * @param itemPrimaryKeySupplier
 * @param entityPrimaryKeySupplier
 */
class DatabaseListUpdater<I, E>(
    private val itemPrimaryKeySupplier: (item: I) -> Identifier,
    private val entityPrimaryKeySupplier: (item: E) -> Long,
) {

    /** The complete new list of items. */
    private val updateItems = mutableListOf<I>()
    /** The complete new list of entities. */
    private val updateEntities = mutableListOf<E>()
    /** The list of items to be added. */
    val toBeAdded = mutableListOf<E>()
    /** The list of items to be updated. */
    val toBeUpdated = mutableListOf<E>()
    /** The list of items to be removed. */
    val toBeRemoved = mutableListOf<E>()

    /**
     * Refresh the add, update and remove lists.
     *
     * @param currentEntities the list of entities currently in the database.
     * @param newItems the updated list of items to be inserted in the database.
     * @param toEntity provides the conversion between [I] and [E].
     */
    suspend fun refreshUpdateValues(currentEntities: List<E>, newItems: List<I>, toEntity: suspend (index: Int, item: I) -> E) {
        // Clear previous use values and init entities to be removed with all current entities
        toBeAdded.clear()
        toBeUpdated.clear()
        toBeRemoved.apply {
            clear()
            addAll(currentEntities)
        }
        updateItems.apply {
            clear()
            addAll(newItems)
        }
        updateEntities.clear()

        // New items with the default primary key should be added, others should be updated.
        // Updated items are removed from toBeRemoved list.
        newItems.forEachIndexed { index, newItem ->
            val newItemPrimaryKey = itemPrimaryKeySupplier(newItem).databaseId
            val newEntity = toEntity(index, newItem)

            if (newItemPrimaryKey == DATABASE_DEFAULT_LONG_PRIMARY_KEY) {
                toBeAdded.add(newEntity)
            } else {
                toBeUpdated.add(newEntity)
                toBeRemoved.removeIf { entityPrimaryKeySupplier(it) == newItemPrimaryKey }
            }
            updateEntities.add(newEntity)
        }
    }

    /** Get the update item corresponding to the entity to add/update/remove. */
    fun getItemFromEntity(entity: E): I? {
        val index = updateEntities.indexOf(entity)
        if (index == -1 || index > updateItems.lastIndex) return null

        return updateItems[index]
    }

    fun clear() {
        toBeAdded.clear()
        toBeUpdated.clear()
        toBeRemoved.clear()
        updateItems.clear()
    }

    override fun toString(): String =
        "DatabaseListUpdater[toBeAdded=${toBeAdded.size}; toBeUpdated=${toBeUpdated.size}; teBeRemoved=${toBeRemoved.size}]"
}

/** Default value for a [Long] primary key in a Room database. */
private const val DATABASE_DEFAULT_LONG_PRIMARY_KEY = 0L