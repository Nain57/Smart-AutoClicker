/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import android.content.Context
import android.graphics.Bitmap

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionHeader
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiTextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** View model for the [ConditionCopyDialog]. */
class ConditionCopyModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: IRepository,
    editionRepository: EditionRepository,
) : ViewModel() {

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    private val allCopyItems: Flow<List<UiConditionItem>> =
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
                    add(UiConditionHeader(R.string.list_header_copy_conditions_this))
                    addAll(editedConditions.toCopyItems(context)
                        .distinctByUiDisplay().sortedBy { it.name })
                }
                if (otherConditions.isNotEmpty()) {
                    add(UiConditionHeader(R.string.list_header_copy_conditions_all))
                    addAll(otherConditions.toCopyItems(context)
                        .distinctByUiDisplay().sortedBy { it.name })
                }
            }
        }

    /** List of displayed condition items. */
    val conditionList: Flow<List<UiConditionItem>?> = allCopyItems.combine(searchQuery) { allItems, query ->
        if (query.isNullOrEmpty()) allItems
        else allItems
            .filterIsInstance<UiCondition>()
            .filter { item -> item.name.contains(query, true) }
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

    private fun List<Condition>.toCopyItems(context: Context) = mapNotNull { condition ->
        when (condition) {
            is ImageCondition -> condition.toUiImageCondition(context, inError = !condition.isComplete())
            is TextCondition -> condition.toUiTextCondition(context, inError = !condition.isComplete())
            is TriggerCondition -> condition.toUiTriggerCondition(context, inError = !condition.isComplete())
        }
    }

    private fun List<UiCondition>.distinctByUiDisplay() =
        distinctBy { item ->
            when (item) {
                is UiImageCondition ->
                    item.condition.hashCodeNoIds()

                is UiTextCondition ->
                    item.condition.hashCodeNoIds()

                is UiTriggerCondition ->
                    when (item.condition) {
                        is TriggerCondition.OnBroadcastReceived -> item.condition.name.hashCode() +
                                item.condition.intentAction.hashCode()

                        is TriggerCondition.OnCounterCountReached -> item.condition.name.hashCode() +
                                item.condition.counterName.hashCode() +
                                item.condition.counterValue.hashCode() +
                                item.condition.comparisonOperation.hashCode()

                        is TriggerCondition.OnTimerReached -> item.condition.name.hashCode() +
                                item.condition.durationMs

                        else -> 0
                    }
            }
        }
}