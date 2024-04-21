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
package com.buzbuz.smartautoclicker.feature.smart.config.data.base

import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

internal open class ListEditor<Item , Parent>(
    private val onListUpdated: ((List<Item>) -> Unit)? = null,
    canBeEmpty: Boolean = false,
    parentItem: StateFlow<Parent?>,
) where Item: Identifiable, Item: Completable {

    private val referenceList: MutableStateFlow<List<Item>?> = MutableStateFlow(null)
    private val _editedList: MutableStateFlow<List<Item>?> = MutableStateFlow(null)
    val editedList: StateFlow<List<Item>?> = _editedList
    val listState: Flow<EditedListState<Item>> = combine(referenceList, _editedList, parentItem) { ref, edit, parent ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        var canBeSaved = true
        val itemValidity: MutableList<Boolean> = ArrayList(edit?.size ?: 0)
        when {
            edit == null -> canBeSaved = false
            edit.isEmpty() -> canBeSaved = canBeEmpty
            else -> {
                edit.forEach { item ->
                    if (!itemCanBeSaved(item, parent)) {
                        canBeSaved = false
                        itemValidity.add(false)
                    } else {
                        itemValidity.add(true)
                    }
                }
            }
        }

        EditedListState(edit, itemValidity, hasChanged, canBeSaved)
    }

    private val referenceEditedItem: MutableStateFlow<Item?> = MutableStateFlow(null)
    private val _editedItem: MutableStateFlow<Item?> = MutableStateFlow(null)
    val editedItem: StateFlow<Item?> = _editedItem
    val editedItemState: Flow<EditedElementState<Item>> = combine(referenceEditedItem, _editedItem, parentItem) { ref, edit, parent ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = itemCanBeSaved(edit, parent)

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    val allEditedItems: Flow<List<Item>> =
        combine(editedList, editedItem, ::buildAllItemList)

    fun getAllEditedItems(): List<Item> =
        buildAllItemList(editedList.value, editedItem.value)

    fun startEdition(referenceItems: List<Item>) {
        referenceList.value = referenceItems
        _editedList.value = referenceItems.toList()
    }

    fun isItemEditionStarted(): Boolean =
        referenceEditedItem.value != null

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
        stopItemEdition()
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
        val removedItem = _editedItem.value ?: return
        val newItems = _editedList.value?.toMutableList() ?: return
        val index = newItems.indexOfItem(removedItem)

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

    open fun itemCanBeSaved(item: Item?, parent: Parent?): Boolean =
        item?.isComplete() ?: false

    private fun List<Item>.indexOfItem(item: Item): Int =
        indexOfFirst { it.id == item.id }

    private fun buildAllItemList(editedList: List<Item>?, editedItem: Item?) =
        buildList {
            val items = editedList ?: emptyList()
            addAll(items)

            editedItem?.let { item ->
                if (items.find { item.id == it.id } == null) add(item)
            }
        }
}