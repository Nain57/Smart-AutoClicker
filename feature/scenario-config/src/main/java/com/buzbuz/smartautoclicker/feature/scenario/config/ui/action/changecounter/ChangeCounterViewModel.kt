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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.changecounter

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

@OptIn(FlowPreview::class)
class ChangeCounterViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)

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

    val counterOperationCheckedId: Flow<Int> = configuredChangeCounter
        .map { action ->
            when (action.operation) {
                Action.ChangeCounter.OperationType.ADD -> BUTTON_ADD
                Action.ChangeCounter.OperationType.MINUS -> BUTTON_MINUS
                Action.ChangeCounter.OperationType.SET -> BUTTON_SET
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

    fun setOperationCheckedButtonId(checkedButton: Int?) {
        updateEditedChangeCounter { old ->
            old.copy(
                operation = when (checkedButton) {
                    BUTTON_ADD -> Action.ChangeCounter.OperationType.ADD
                    BUTTON_MINUS -> Action.ChangeCounter.OperationType.MINUS
                    BUTTON_SET -> Action.ChangeCounter.OperationType.SET
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