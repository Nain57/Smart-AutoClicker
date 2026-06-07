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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toAffectationOperation
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toDisplayValue
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject

import kotlinx.coroutines.flow.combine

@OptIn(FlowPreview::class)
class ChangeCounterViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The action being configured by the user. */
    private val configuredChangeCounter = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<ChangeCounter>()

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    val uiState: StateFlow<ChangeCounterUiState?> = combine(
        configuredChangeCounter,
        editionRepository.editionState.editedActionState.map { it.hasChanged },
        editionRepository.editionState.editedActionState.map { it.canBeSaved },
    ) { action, hasChanged, canBeSaved ->
        action.toUiState(context, canBeSaved = canBeSaved, hasChanged = hasChanged)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedChangeCounter { old -> old.copy(name = "" + name) }
    }

    fun setCounterName(counterName: String) {
        updateEditedChangeCounter { old -> old.copy(counterName = "" + counterName) }
    }

    fun setOperationItem(item: UiCounterOperatorDropdownItem) {
        updateEditedChangeCounter { old -> old.copy(operation = item.toAffectationOperation()) }
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

    private fun ChangeCounter.toUiState(context: Context, canBeSaved: Boolean, hasChanged: Boolean): ChangeCounterUiState {
        val counterToChange = UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(counterName))
        val operand = operationValue.toUiStaticOrCounterSelection()

        return ChangeCounterUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasChanged,
            name = name,
            nameError = name?.isEmpty() ?: true,
            counter = counterToChange,
            operator = operation.toCounterOperatorDropdownItem(),
            operandValue = operand,
            actionEffectText = operation.toEffectDescription(
                context = context,
                counterName = counterName,
                operand = operand.toDisplayValue(),
            )
        )
    }

    private fun CounterOperationValue.toUiStaticOrCounterSelection(): UiStaticOrCounterSelection =
        when (this) {
            is CounterOperationValue.Counter ->
                UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(value))

            is CounterOperationValue.Number ->
                UiStaticOrCounterSelection.StaticValue(value)
        }
}
