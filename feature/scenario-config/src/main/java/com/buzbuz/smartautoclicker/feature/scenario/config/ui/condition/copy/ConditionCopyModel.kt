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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.copy

import android.app.Application
import android.graphics.Bitmap

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [ConditionCopyDialog].
 * @param application the Android application.
 */
class ConditionCopyModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** List of displayed condition items. */
    val conditionList: Flow<List<ConditionCopyItem>?> =
        combine(repository.getAllConditions(), editionRepository.editionState.editedEventConditionsState, searchQuery) { dbCond, eventCond, query ->
            val editedConditions = eventCond.value ?: return@combine null
            if (query.isNullOrEmpty()) getAllItems(dbCond, editedConditions) else dbCond.toCopyItemsFromSearch(query)
        }

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        if (condition.bitmap != null) {
            onBitmapLoaded.invoke(condition.bitmap)
            return null
        }

        if (condition.path != null) {
            return viewModelScope.launch(Dispatchers.IO) {
                val bitmap = repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        onBitmapLoaded.invoke(bitmap)
                    }
                }
            }
        }

        onBitmapLoaded.invoke(null)
        return null
    }

    /**
     * Get all items with the headers.
     * @param dbConditions all conditions in the database.
     * @param eventConditions all conditions in the current event.
     * @return the complete list of condition items.
     */
    private fun getAllItems(dbConditions: List<Condition>, eventConditions: List<Condition>): List<ConditionCopyItem> {
        val allItems = mutableListOf<ConditionCopyItem>()

        // First, add the actions from the current event
        val eventItems = eventConditions.toCopyItemsFromCurrentEvent()
        if (eventItems.isNotEmpty()) {
            allItems.add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_this))
            allItems.addAll(eventItems)
        }

        // Then, add all other conditions. Remove the one already in this event.
        val conditions = dbConditions.toCopyItemsFromOtherEvents(eventItems)
        if (conditions.isNotEmpty()) {
            allItems.add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_all))
            allItems.addAll(conditions)
        }

        return allItems
    }

    /**
     * Get the result of the search query.
     * @param query the current search query.
     */
    private fun List<Condition>.toCopyItemsFromSearch(query: String) =
        filter { condition -> condition.name.contains(query, true) }
            .map { ConditionCopyItem.ConditionItem(it) }
            .distinct()

    /** */
    private fun List<Condition>.toCopyItemsFromCurrentEvent() =
        sortedBy { it.name }
            .map { ConditionCopyItem.ConditionItem(it) }
            .distinct()

    /** */
    private fun List<Condition>.toCopyItemsFromOtherEvents(eventItems: List<ConditionCopyItem.ConditionItem>) =
        map { ConditionCopyItem.ConditionItem(it) }
            .toMutableList()
            .apply {
                removeIf { allItem ->
                    eventItems.find {
                        allItem.condition.id == it.condition.id || allItem == it
                    } != null
                }
            }
            .distinct()

    /** Types of items in the condition copy list. */
    sealed class ConditionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(@StringRes val title: Int) : ConditionCopyItem()

        /**
         * Condition item.
         * @param condition the details for the condition.
         */
        data class ConditionItem (val condition: Condition) : ConditionCopyItem()
    }
}