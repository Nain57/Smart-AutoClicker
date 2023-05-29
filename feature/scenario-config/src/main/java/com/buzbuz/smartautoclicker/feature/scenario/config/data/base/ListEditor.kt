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

import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditedElementState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

abstract class ListEditor<Item>(
    private val onListUpdated: ((List<Item>) -> Unit)? = null,
    canBeEmpty: Boolean = false,
) {

    private val referenceList: MutableStateFlow<List<Item>?> = MutableStateFlow(null)
    private val _editedList: MutableStateFlow<List<Item>?> = MutableStateFlow((null))
    val editedList: StateFlow<List<Item>?> = _editedList
    val listState: Flow<EditedElementState<List<Item>>> = combine(referenceList, _editedList) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = when {
            edit == null -> false
            edit.isEmpty() -> canBeEmpty
            else -> edit.find { !isItemComplete(it) } == null
        }

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    private val referenceEditedItem: MutableStateFlow<Item?> = MutableStateFlow(null)
    protected val _editedItem: MutableStateFlow<Item?> = MutableStateFlow((null))
    val editedItem: StateFlow<Item?> = _editedItem
    val editedItemState: Flow<EditedElementState<Item>> = combine(referenceEditedItem, _editedItem) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = edit?.let { isItemComplete(it) } ?: false

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    abstract fun areItemsTheSame(a: Item, b: Item): Boolean
    abstract fun isItemComplete(item: Item): Boolean

    fun startEdition(referenceItems: List<Item>) {
        referenceList.value = referenceItems
        _editedList.value = referenceItems.toList()
    }

    @CallSuper
    open fun startItemEdition(item: Item) {
        _editedList.value ?: return

        referenceEditedItem.value = item
        _editedItem.value = item
    }

    @CallSuper
    open fun stopItemEdition() {
        referenceEditedItem.value = null
        _editedItem.value = null
    }

    fun stopEdition() {
        referenceList.value = null
        _editedList.value = null
    }

    fun updateEditedItem(item: Item) {
        _editedItem.value ?: return
        _editedItem.value = item
    }

    /** Update/Insert a new item to the list. */
    fun upsertEditedItem() {
        val newItem = _editedItem.value ?: return
        val newItems = _editedList.value?.toMutableList() ?: return
        val itemIndex = newItems.indexOfItem(newItem)

        if (itemIndex == -1) newItems.add(newItem)
        else newItems[itemIndex] = newItem

        updateList(newItems)
        stopItemEdition()
    }

    /** Delete a item from the scenario. */
    @CallSuper
    open fun deleteEditedItem() {
        val newItem = _editedItem.value ?: return
        val newItems = _editedList.value?.toMutableList() ?: return
        val index = newItems.indexOfItem(newItem)

        if (index == -1) {
            stopItemEdition()
            return
        }

        newItems.removeAt(index)
        updateList(newItems)
        stopItemEdition()
    }

    /**
     * Update the order of the items in the list.
     * @param items the items, ordered by their new order.
     */
    fun updateList(items: List<Item>?) {
        val newList = items?.toList() ?: return

        _editedList.value = newList
        onListUpdated?.invoke(newList)
    }

    private fun List<Item>.indexOfItem(item: Item): Int =
        indexOfFirst { areItemsTheSame(it, item) }
}