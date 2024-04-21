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

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

import javax.inject.Inject

@OptIn(FlowPreview::class)
class ChangeCounterViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val plusItem = DropdownItem(R.string.item_title_add)
    private val setItem = DropdownItem(R.string.item_title_set)
    private val minusItem = DropdownItem(R.string.item_title_minus)

    /** The action being configured by the user. */
    private val configuredChangeCounter = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Action.ChangeCounter>()

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

    /** The name of the counter. */
    val valueText: Flow<String?> = configuredChangeCounter
        .map { it.operationValue.toString() }
        .take(1)

    val operatorDropdownItems = listOf(plusItem, setItem, minusItem)
    val operatorDropdownState: Flow<DropdownItem> = configuredChangeCounter
        .map { condition ->
            when (condition.operation) {
                Action.ChangeCounter.OperationType.ADD -> plusItem
                Action.ChangeCounter.OperationType.MINUS -> minusItem
                Action.ChangeCounter.OperationType.SET -> setItem
            }
        }

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

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
                    plusItem -> Action.ChangeCounter.OperationType.ADD
                    minusItem -> Action.ChangeCounter.OperationType.MINUS
                    setItem -> Action.ChangeCounter.OperationType.SET
                    else -> Action.ChangeCounter.OperationType.ADD
                }
            )
        }
    }

    fun setOperationValue(value: Int?) {
        updateEditedChangeCounter { old -> old.copy(operationValue = value ?: -1) }
    }

    private fun updateEditedChangeCounter(closure: (old: Action.ChangeCounter) -> Action.ChangeCounter) {
        editionRepository.editionState.getEditedAction<Action.ChangeCounter>()?.let { old ->
            editionRepository.updateEditedAction(closure(old))
        }
    }
}

internal const val BUTTON_ADD = 0
internal const val BUTTON_MINUS = 1
internal const val BUTTON_SET = 2