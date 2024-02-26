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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.copy

import android.app.Application
import android.graphics.Bitmap

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getImageConditionBitmap

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

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

    private val allCopyItems: Flow<List<ConditionCopyItem>> =
        combine(
            editionRepository.editionState.editedEventState,
            editionRepository.editionState.conditionsForCopy,
        ) { editedEventState, conditions ->

            val editedEvent = editedEventState.value ?: return@combine emptyList()
            val editedConditions = mutableListOf<Condition>()
            val otherConditions = mutableListOf<Condition>()
            conditions.forEach { condition ->
                if (editedEvent.id == condition.eventId) editedConditions.add(condition)
                else otherConditions.add(condition)
            }

            buildList {
                if (editedConditions.isNotEmpty()) {
                    add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_this))
                    addAll(editedConditions.toCopyItems().distinctByUiDisplay().sortedBy { it.condition.name })
                }
                if (otherConditions.isNotEmpty()) {
                    add(ConditionCopyItem.HeaderItem(R.string.list_header_copy_conditions_all))
                    addAll(otherConditions.toCopyItems().distinctByUiDisplay().sortedBy { it.condition.name })
                }
            }
        }

    /** List of displayed condition items. */
    val conditionList: Flow<List<ConditionCopyItem>?> = allCopyItems.combine(searchQuery) { allItems, query ->
        if (query.isNullOrEmpty()) allItems
        else allItems
            .filterIsInstance<ConditionCopyItem.ConditionItem>()
            .filter { item -> item.condition.name.contains(query, true) }
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
    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(repository, condition, onBitmapLoaded)

    /** */
    private fun List<Condition>.toCopyItems() = map { condition ->
        when (condition) {
            is ImageCondition -> ConditionCopyItem.ConditionItem.Image(condition)
            is TriggerCondition -> ConditionCopyItem.ConditionItem.Trigger(condition)
        }
    }

    private fun List<ConditionCopyItem.ConditionItem>.distinctByUiDisplay() =
        distinctBy { item ->
            when (item) {
                is ConditionCopyItem.ConditionItem.Image ->
                    item.condition.hashCodeNoIds()

                is ConditionCopyItem.ConditionItem.Trigger ->
                    when (item.condition) {
                        is TriggerCondition.OnBroadcastReceived -> item.condition.name.hashCode() +
                                item.condition.intentAction.hashCode()

                        is TriggerCondition.OnCounterCountReached -> item.condition.name.hashCode() +
                                item.condition.counterName.hashCode() +
                                item.condition.counterValue +
                                item.condition.comparisonOperation.hashCode()

                        is TriggerCondition.OnTimerReached -> item.condition.name.hashCode() +
                                item.condition.durationMs

                        else -> 0
                    }
            }
        }

    /** Types of items in the condition copy list. */
    sealed class ConditionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(@StringRes val title: Int) : ConditionCopyItem()

        sealed class ConditionItem : ConditionCopyItem() {

            abstract val condition: Condition

            /**
             * Image Condition item.
             * @param condition the details for the condition.
             */
            data class Image(override val condition: ImageCondition) : ConditionItem()

            /**
             * Trigger Condition item.
             * @param condition the details for the condition.
             */
            data class Trigger(override val condition: TriggerCondition) : ConditionItem()

        }
    }
}