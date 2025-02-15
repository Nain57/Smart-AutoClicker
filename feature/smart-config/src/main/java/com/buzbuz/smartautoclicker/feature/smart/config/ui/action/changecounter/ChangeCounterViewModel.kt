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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

import javax.inject.Inject

@OptIn(FlowPreview::class)
class ChangeCounterViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val plusItem = DropdownItem(R.string.dropdown_counter_operation_item_add)
    private val setItem = DropdownItem(R.string.dropdown_counter_operation_item_set)
    private val minusItem = DropdownItem(R.string.dropdown_counter_operation_item_minus)

    /** The action being configured by the user. */
    private val configuredChangeCounter = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<ChangeCounter>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the action. */
    val name: Flow<String?> = configuredChangeCounter
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredChangeCounter.map { it.name?.isEmpty() ?: true }

    /** The name of the counter. */
    val counterName: Flow<String?> = configuredChangeCounter
        .map { it.counterName }
        .take(1)
    /** Tells if the name of the counter is valid or not. */
    val counterNameError: Flow<Boolean> = configuredChangeCounter.map { it.counterName.isEmpty() }

    val operatorDropdownItems = listOf(plusItem, setItem, minusItem)
    val operatorDropdownState: Flow<DropdownItem> = configuredChangeCounter
        .map { condition ->
            when (condition.operation) {
                ChangeCounter.OperationType.ADD -> plusItem
                ChangeCounter.OperationType.MINUS -> minusItem
                ChangeCounter.OperationType.SET -> setItem
            }
        }

    val isNumberValue: Flow<Boolean> = configuredChangeCounter
        .map { it.operationValue is CounterOperationValue.Number }

    val numberValueText: Flow<String?> = configuredChangeCounter
        .map { it.operationValue }
        .filterIsInstance<CounterOperationValue.Number>()
        .map { it.value.toString() }
        .take(1)

    val counterNameValueText: Flow<String?> = configuredChangeCounter
        .map { it.operationValue }
        .filterIsInstance<CounterOperationValue.Counter>()
        .map { it.value }
        .take(1)

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    fun setName(name: String) {
        updateEditedChangeCounter { old -> old.copy(name = "" + name) }
    }

    fun setCounterName(counterName: String) {
        updateEditedChangeCounter { old -> old.copy(counterName = "" + counterName) }
    }

    fun setOperationItem(item: DropdownItem) {
        updateEditedChangeCounter { old ->
            old.copy(
                operation = when (item) {
                    plusItem -> ChangeCounter.OperationType.ADD
                    minusItem -> ChangeCounter.OperationType.MINUS
                    setItem -> ChangeCounter.OperationType.SET
                    else -> ChangeCounter.OperationType.ADD
                }
            )
        }
    }

    fun setOperationValue(value: CounterOperationValue) {
        updateEditedChangeCounter { old ->
            old.copy(operationValue = value)
        }
    }

    private fun updateEditedChangeCounter(closure: (old: ChangeCounter) -> ChangeCounter) {
        editionRepository.editionState.getEditedAction<ChangeCounter>()?.let { old ->
            editionRepository.updateEditedAction(closure(old))
        }
    }
}
