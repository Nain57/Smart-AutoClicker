/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.data

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal class CountersEditor {

    private val referenceList: MutableStateFlow<List<Counter>?> = MutableStateFlow(null)

    private val _editedList: MutableStateFlow<List<Counter>?> = MutableStateFlow(null)
    val editedList: StateFlow<List<Counter>?> = _editedList

    val listState: Flow<EditedListState<Counter>> = combine(referenceList, _editedList) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        var canBeSaved = true
        val itemValidity: MutableList<Boolean> = ArrayList(edit?.size ?: 0)
        when {
            edit == null -> canBeSaved = false
            edit.isEmpty() -> canBeSaved = true
            else -> {
                edit.forEach { item ->
                    if (!item.isComplete()) {
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


    fun startEdition(referenceItems: List<Counter>) {
        referenceList.value = referenceItems.toList()
        _editedList.value = referenceItems.toList()
    }

    fun saveEditionAsReference() {
        referenceList.value = _editedList.value
    }

    fun getCounter(name: String): Counter? =
        _editedList.value?.find { counter -> counter.counterName == name }

    fun isCounterDefined(counterName: String): Boolean =
        _editedList.value?.let { it.find { counter -> counter.counterName == counterName } != null } ?: false

    fun addCounter(item: Counter) {
        val currentCounters = _editedList.value ?: emptyList()
        if (isCounterDefined(item.counterName)) return

        _editedList.value = buildList {
            addAll(currentCounters)
            add(item)
        }
    }

    fun updateCounter(item: Counter) {
        _editedList.update { currentCounters ->
            val toBeUpdatedIndex = currentCounters?.indexOfFirst { counter ->
                counter.counterName == item.counterName
            } ?: return
            if (toBeUpdatedIndex !in currentCounters.indices) return

            currentCounters.toMutableList().apply {
                set(toBeUpdatedIndex, item)
            }.toList()
        }
    }

    fun deleteEditedCounter(item: Counter) {
        val currentCounters = _editedList.value ?: emptyList()
        _editedList.value = currentCounters.toMutableList().apply { remove(item) }
    }

    fun stopEdition() {
        referenceList.value = null
        _editedList.value = null
    }
}