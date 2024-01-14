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

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [DatabaseListUpdater]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DatabaseListUpdaterTests {

    private data class Item(
        override val id: Identifier,
    ) : Identifiable

    private data class Entity(
        override val id: Long,
    ) : EntityWithId

    private fun Item.toEntity(): Entity =
        Entity(id.databaseId)

    @Test
    fun emptyLists() = runTest {
        val oldEntities = emptyList<Entity>()
        val newItems = emptyList<Item>()

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldEntities, newItems) { it.toEntity() }

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyAdd() = runTest {
        val oldEntities = emptyList<Entity>()
        val newItems = listOf(Item(Identifier(tempId = 1)), Item(Identifier(tempId = 2)), Item(Identifier(tempId = 3)))

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldEntities, newItems) { it.toEntity() }

        assertEquals("ToBeAdded should be containing all new items", newItems.size, updater.toBeAdded.size)
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyUpdate() = runTest {
        val oldEntities = listOf(Entity(1), Entity(2), Entity(3))
        val updateItems = listOf(Item(Identifier(databaseId = 1)), Item(Identifier(databaseId = 2)), Item(Identifier(databaseId = 3)))

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldEntities, updateItems) { it.toEntity() }

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertEquals("ToBeUpdated should be contains all new items", updateItems.size, updater.toBeUpdated.size)
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyRemove() = runTest {
        val oldEntities = listOf(Entity(1), Entity(2), Entity(3))
        val newItems = emptyList<Item>()

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldEntities, newItems) { it.toEntity() }

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertEquals("ToBeRemoved should be contains all new items", oldEntities.size, updater.toBeRemoved.size)
    }

    @Test
    fun allAtOnce() = runTest {
        val deletedId = 47L
        val updatedId = 56L
        val oldEntities = listOf(Entity(deletedId), Entity(updatedId))
        val newItems = listOf(Item(Identifier(tempId = 1)), Item(Identifier(databaseId = updatedId)))

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldEntities, newItems) { it.toEntity() }

        assertEquals("ToBeAdded is invalid", DATABASE_ID_INSERTION, updater.toBeAdded.entities.first().id)
        assertEquals("ToBeUpdated is invalid", updatedId, updater.toBeUpdated.entities.first().id)
        assertEquals("ToBeRemoved is invalid", deletedId, updater.toBeRemoved[0].id)
    }

    @Test
    fun idMapping() = runTest {
        val tempIdentifier1 = Identifier(tempId = 1)
        val tempIdentifier2 = Identifier(tempId = 2)
        val mappedDbId1 = 11L
        val mappedDbId2 = 12L
        val oldItems = emptyList<Entity>()
        val newItems = listOf(Item(tempIdentifier1), Item(tempIdentifier2))

        val updater = DatabaseListUpdater<Item, Entity>()
        updater.refreshUpdateValues(oldItems, newItems) { it.toEntity() }
        updater.executeUpdate(
            addList = { _ -> listOf(mappedDbId1, mappedDbId2) },
            updateList = { },
            removeList = { },
            onSuccess = { newIds, added, updated, removed ->
                assertEquals("added invalid size", newItems.size, added.size)
                assertTrue("updated should be empty", updated.isEmpty())
                assertTrue("removed should be empty", removed.isEmpty())

                assertEquals("invalid mapped id 1", mappedDbId1, newIds[tempIdentifier1.tempId])
                assertEquals("invalid mapped id 2", mappedDbId2, newIds[tempIdentifier2.tempId])
            }
        )
    }
}