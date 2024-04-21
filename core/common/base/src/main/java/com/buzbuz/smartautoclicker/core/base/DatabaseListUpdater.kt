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
package com.buzbuz.smartautoclicker.core.base

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable

import org.jetbrains.annotations.VisibleForTesting

/**
 * Helper class to update a list in the database.
 *
 * When updating a complete list of entities in a database, you have the find which items will be added, updated or
 * removed. This class provides this information using the method [refreshUpdateValues] to parse the list, and the
 * [toBeAdded], [toBeUpdated] and [toBeRemoved] list to get the information.
 *
 * @param Item type of the items in the list to be updated.
 * @param Entity type of the entities in the database.
 */
class DatabaseListUpdater<Item : Identifiable, Entity : EntityWithId> {

    /** The list of items to be added. */
    @VisibleForTesting
    internal val toBeAdded = UpdateList<Item, Entity>()
    /** The list of items to be updated. */
    @VisibleForTesting
    internal val toBeUpdated = UpdateList<Item, Entity>()
    /** The list of items to be removed. */
    @VisibleForTesting
    internal val toBeRemoved = mutableListOf<Entity>()

    suspend fun refreshUpdateValues(currentEntities: Collection<Entity>, newItems: Collection<Item>, mappingClosure: suspend (Item) -> Entity) {
        // Clear previous use values and init entities to be removed with all current entities
        toBeAdded.clear()
        toBeUpdated.clear()
        toBeRemoved.apply {
            clear()
            addAll(currentEntities)
        }

        // New items with the default primary key should be added, others should be updated.
        // Updated items are removed from toBeRemoved list.
        newItems.forEach { newItem ->
            val newEntity = mappingClosure(newItem)
            if (newEntity.id == DATABASE_ID_INSERTION) toBeAdded.add(newItem, newEntity)
            else {
                val oldItemIndex = toBeRemoved.indexOfFirst { it.id == newItem.id.databaseId }
                if (oldItemIndex != -1) {
                    toBeUpdated.add(newItem, newEntity)
                    toBeRemoved.removeAt(oldItemIndex)
                }
            }
        }
    }

    suspend fun executeUpdate(
        addList: suspend (List<Entity>) -> List<Long>,
        updateList: suspend (List<Entity>) -> Unit,
        removeList: suspend (List<Entity>) -> Unit,
        onSuccess: (suspend (newIds: Map<Long, Long>, added: List<Item>, updated: List<Item>, removed: List<Entity>) -> Unit)? = null,
    ) {
        val newIdsMapping = mutableMapOf<Long, Long>()
        addList(toBeAdded.entities).forEachIndexed { index, dbId ->
            toBeAdded.items[index].let { item ->
                item.getDomainId()?.let { domainId -> newIdsMapping[domainId] = dbId }
            }
        }

        updateList(toBeUpdated.entities)
        removeList(toBeRemoved)

        onSuccess?.invoke(newIdsMapping, toBeAdded.items, toBeUpdated.items, toBeRemoved)
    }

    fun clear() {
        toBeAdded.clear()
        toBeUpdated.clear()
        toBeRemoved.clear()
    }

    override fun toString(): String =
        "DatabaseListUpdater[toBeAdded=${toBeAdded.size}; toBeUpdated=${toBeUpdated.size}; teBeRemoved=${toBeRemoved.size}]"
}

@VisibleForTesting
internal class UpdateList<Item, Entity> {

    private val _items = mutableListOf<Item>()
    val items: List<Item> = _items

    private val _entities = mutableListOf<Entity>()
    val entities: List<Entity> = _entities

    val size: Int get() = _items.size

    fun isEmpty(): Boolean = size == 0

    fun add(item: Item, entity: Entity) {
        _items.add(item)
        _entities.add(entity)
    }

    fun clear() {
        _items.clear()
        _entities.clear()
    }

    fun forEach(closure: (Item, Entity) -> Unit): Unit =
        items.forEachIndexed { index, item -> closure(item, entities[index]) }
}