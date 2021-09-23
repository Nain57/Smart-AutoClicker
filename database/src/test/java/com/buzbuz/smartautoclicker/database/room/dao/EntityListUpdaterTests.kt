/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room.dao

import android.os.Build

import kotlinx.coroutines.ExperimentalCoroutinesApi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the [EntityListUpdater]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EntityListUpdaterTests {

    @Test
    fun emptyLists() {
        val updater = EntityListUpdater<Long, Long>(DEFAULT_PRIMARY_KEY) { it }
        updater.refreshUpdateValues(emptyList(), emptyList())

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyAdd() {
        val newItems = listOf(DEFAULT_PRIMARY_KEY, DEFAULT_PRIMARY_KEY, DEFAULT_PRIMARY_KEY)

        val updater = EntityListUpdater<Long, Long>(DEFAULT_PRIMARY_KEY) { it }
        updater.refreshUpdateValues(emptyList(), newItems)

        assertEquals("ToBeAdded should be contains all new items", newItems.size, updater.toBeAdded.size)
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyUpdate() {
        val updateItems = listOf(1L, 2L, 3L)

        val updater = EntityListUpdater<Long, Long>(DEFAULT_PRIMARY_KEY) { it }
        updater.refreshUpdateValues(updateItems, updateItems)

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertEquals("ToBeUpdated should be contains all new items", updateItems.size, updater.toBeUpdated.size)
        assertTrue("ToBeRemoved should be empty", updater.toBeRemoved.isEmpty())
    }

    @Test
    fun onlyRemove() {
        val oldItems = listOf(DEFAULT_PRIMARY_KEY, DEFAULT_PRIMARY_KEY, DEFAULT_PRIMARY_KEY)

        val updater = EntityListUpdater<Long, Long>(DEFAULT_PRIMARY_KEY) { it }
        updater.refreshUpdateValues(oldItems, emptyList())

        assertTrue("ToBeAdded should be empty", updater.toBeAdded.isEmpty())
        assertTrue("ToBeUpdated should be empty", updater.toBeUpdated.isEmpty())
        assertEquals("ToBeRemoved should be contains all new items", oldItems.size, updater.toBeRemoved.size)
    }

    @Test
    fun allAtOnce() {
        val deletedKey = 47L
        val updatedKey = 56L
        val oldItems = listOf(deletedKey, updatedKey)
        val newItems = listOf(DEFAULT_PRIMARY_KEY, updatedKey)

        val updater = EntityListUpdater<Long, Long>(DEFAULT_PRIMARY_KEY) { it }
        updater.refreshUpdateValues(oldItems, newItems)

        assertEquals("ToBeAdded is invalid", DEFAULT_PRIMARY_KEY, updater.toBeAdded[0])
        assertEquals("ToBeUpdated is invalid", updatedKey, updater.toBeUpdated[0])
        assertEquals("ToBeRemoved is invalid", deletedKey, updater.toBeRemoved[0])
    }
}

private const val DEFAULT_PRIMARY_KEY = 0L