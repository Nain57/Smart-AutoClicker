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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.GetCounterReadReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.model.CounterReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.GetCounterWriteReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter.ReplaceCounterUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


class CountersConfigViewModel @Inject constructor(
    @ApplicationContext context: Context,
    getCounterReadReferencesUseCase: GetCounterReadReferencesUseCase,
    getCounterWriteReferencesUseCase: GetCounterWriteReferencesUseCase,
    private val replaceCounterUseCase: ReplaceCounterUseCase,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val allCounters: Flow<EditedListState<Counter>> = editionRepository.editionState.editedCountersState
    private val readReferences: Flow<Map<String, Set<CounterReference>>> = getCounterReadReferencesUseCase()
    private val writeReferences: Flow<Map<String, Set<CounterReference>>> = getCounterWriteReferencesUseCase()

    private val selectedForReplacement: MutableStateFlow<CounterUiItem?> = MutableStateFlow(null)
    private val expandedItems: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    val uiState: StateFlow<CountersUiState?> = combine(
        allCounters,
        readReferences,
        writeReferences,
        expandedItems,
        selectedForReplacement
    ) { counters, readRefs, writeRefs, expanded, forReplacement ->
            val counterList = counters.value ?: return@combine CountersUiState.Empty
            val countersItems = counterList.map { counter ->
                counter.toUiItem(
                    context = context,
                    counterCount = counterList.size,
                    readReferences = readRefs.getOrDefault(counter.counterName, emptySet()),
                    writeReferences = writeRefs.getOrDefault(counter.counterName, emptySet()),
                    isExpanded = expanded.contains(counter.counterName),
                    forReplacement = forReplacement?.counterName == counter.counterName,
                )
            }.sortedBy { counter -> counter.counterName }

            if (forReplacement != null) {
                CountersUiState.Replacing(counterItems = countersItems)
            } else {
                CountersUiState.Loaded(
                    canBeSaved = counters.canBeSaved,
                    hasUnsavedModifications = counters.hasChanged,
                    counterItems = countersItems,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), CountersUiState.Loading)

    fun getUiState(): CountersUiState? = uiState.value

    fun expandCollapseItem(item: CounterUiItem) {
        expandedItems.update { old ->
            if (old.contains(item.counterName)) old - item.counterName
            else old + item.counterName
        }
    }

    fun setStartingValue(item: CounterUiItem, value: Double) {
        editionRepository.updateCounter(
            item.counter.copy(defaultValue = value)
        )
    }

    fun selectForReplacement(item: CounterUiItem) {
        expandedItems.update { setOf(item.counterName) }
        selectedForReplacement.update { item }
    }

    fun cancelReplacement() {
        selectedForReplacement.update { null }
    }

    fun replaceAndDelete(replacedBy: CounterUiItem) {
        val toDelete = selectedForReplacement.value ?: return
        viewModelScope.launch {
            if (toDelete.setByButtonIsEmpty && toDelete.readByButtonIsEmpty) return@launch

            replaceCounterUseCase(toDelete.counter, replacedBy.counter)
            editionRepository.deleteCounter(toDelete.counter)
            selectedForReplacement.update { null }
        }
    }

    fun deleteCounter(item: CounterUiItem) {
        if (item.setByButtonIsEmpty && item.readByButtonIsEmpty) {
            editionRepository.deleteCounter(item.counter)
        }
    }

    fun saveEditions() {
        editionRepository.saveCounterEditionsAsReference()
    }
}

private fun Counter.toUiItem(
    context: Context,
    counterCount: Int,
    readReferences: Set<CounterReference>,
    writeReferences: Set<CounterReference>,
    isExpanded: Boolean,
    forReplacement: Boolean,
): CounterUiItem {
    val writeCount = writeReferences.size
    val readCount = readReferences.size
    val totalReferences = writeCount + readCount

    return CounterUiItem(
        counter = this,
        counterName = counterName,
        isExpanded = isExpanded,
        counterDesc = context.getDescription(
            referencesCount = totalReferences,
            isExpanded = isExpanded,
            startingValue = defaultValue,
        ),
        startingValue = defaultValue,
        setByButtonText = context.getSetByButtonText(writeCount),
        setByButtonIsEmpty = writeCount == 0,
        readByButtonText = context.getReadByButtonText(readCount),
        readByButtonIsEmpty = readCount == 0,
        deleteButtonText = context.getDeleteButtonText(totalReferences),
        deleteButtonEnabled = counterCount > 1,
        selectedForReplacement = forReplacement
    )
}

private fun Context.getDescription(referencesCount: Int, isExpanded: Boolean, startingValue: Double): String =
    if (isExpanded) {
        if (referencesCount == 0) getString(R.string.item_counter_desc_expanded_no_reference)
        else getString(R.string.item_counter_desc_expanded_referenced, referencesCount)
    } else {
        val startingValueText = startingValue.toNaturalDisplayString(maxFractionDigits = 2)

        if (referencesCount == 0) getString(R.string.item_counter_desc_collapsed_no_reference, startingValueText)
        else getString(R.string.item_counter_desc_collapsed_referenced, referencesCount, startingValueText)
    }

private fun Context.getSetByButtonText(actionsCount: Int): String =
    if (actionsCount == 0) getString(R.string.button_text_counter_actions_no_reference)
    else getString(R.string.button_text_counter_actions_references, actionsCount)

private fun Context.getReadByButtonText(conditionsCount: Int): String =
    if (conditionsCount == 0) getString(R.string.button_text_counter_conditions_no_reference)
    else getString(R.string.button_text_counter_conditions_references, conditionsCount)

private fun Context.getDeleteButtonText(referencesCount: Int): String =
    if (referencesCount == 0) getString(R.string.button_text_counter_delete)
    else getString(R.string.button_text_counter_delete_and_replace)
