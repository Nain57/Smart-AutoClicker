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
package com.buzbuz.smartautoclicker.feature.scenario.config.data.base

import com.buzbuz.smartautoclicker.core.domain.model.Identifier

internal abstract class ListEditor<Reference, Item> : Editor<Reference, List<Item>>() {

    /** Manages the creation of the identifiers for the new elements. */
    private val idCreator = IdentifierCreator()

    /**
     * Update/Insert a new item to the list.
     * @param item the item to be added.
     */
    fun upsertItem(item: Item) {
        val newItems = getEditedValueOrThrow().toMutableList()
        val itemIndex = newItems.indexOfItem(item)

        if (itemIndex == -1) newItems.add(item)
        else newItems[itemIndex] = item

        _editedValue.value = newItems
    }

    /**
     * Delete a item from the scenario.
     * @param item the item to be removed.
     */
    fun deleteItem(item: Item) {
        _editedValue.value?.let { items ->
            val index = items.indexOfItem(item)
            if (index == -1) return

            _editedValue.value = items.toMutableList().apply { removeAt(index) }
        }
    }

    /**
     * Update the order of the items in the list.
     *
     * @param items the items, ordered by their new order.
     */
    fun updateList(items: List<Item>) {
        _editedValue.value = items.toList()
    }

    /** Finish the list edition and returns the last value. */
    final override fun onEditionFinished(): Reference {
        val endReference = createReferenceFromEdition()

        return endReference
    }

    /** */
    protected fun generateNewIdentifier(): Identifier = idCreator.generateNewIdentifier()

    protected fun getEditedListSize(): Int =
        _editedValue.value?.size ?: 0

    private fun List<Item>.indexOfItem(item: Item): Int =
        indexOfFirst { itemMatcher(it, item) }

    /** */
    protected abstract fun createReferenceFromEdition(): Reference

    /** */
    protected abstract fun itemMatcher(first: Item, second: Item): Boolean
}