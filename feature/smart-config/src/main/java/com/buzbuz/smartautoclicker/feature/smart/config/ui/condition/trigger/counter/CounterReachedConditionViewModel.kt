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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toComparisonOperation
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toDisplayValue

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject

@OptIn(FlowPreview::class)
class CounterReachedConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition: Flow<TriggerCondition.OnCounterCountReached> =
        editionRepository.editionState.editedTriggerConditionState
            .mapNotNull { it.value }
            .filterIsInstance<TriggerCondition.OnCounterCountReached>()

    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    val uiState: StateFlow<CounterReachedConditionUiState?> = combine(
        configuredCondition,
        editionRepository.editionState.editedTriggerConditionState.map { it.hasChanged },
        editionRepository.editionState.editedTriggerConditionState.map { it.canBeSaved },
    ) { condition, hasChanged, canBeSaved ->
        condition.toUiState(context, canBeSaved = canBeSaved, hasChanged = hasChanged)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedCondition { old -> old.copy(name = "" + name) }
    }

    fun setCounterName(counterName: String) {
        updateEditedCondition { old -> old.copy(counterName = "" + counterName) }
    }

    fun setOperationItem(item: UiCounterOperatorDropdownItem) {
        updateEditedCondition { old -> old.copy(comparisonOperation = item.toComparisonOperation()) }
    }

    fun setOperandType(type: UiOperandType) {
        // Do nothing if this is the same operand
        val currentOperand = uiState.value?.operandValue
        if (currentOperand is UiStaticOrCounterSelection.CounterValue && type == UiOperandType.COUNTER) return
        if (currentOperand is UiStaticOrCounterSelection.StaticValue && type == UiOperandType.STATIC) return

        // Change operand and use default value
        setOperationValue(
            when (type) {
                UiOperandType.STATIC -> CounterOperationValue.Number(0.0)
                UiOperandType.COUNTER -> CounterOperationValue.Counter("")
            }
        )
    }

    fun setOperationValue(value: CounterOperationValue) {
        updateEditedCondition { old ->
            old.copy(counterValue = value)
        }
    }

    private fun updateEditedCondition(
        closure: (oldValue: TriggerCondition.OnCounterCountReached) -> TriggerCondition.OnCounterCountReached?,
    ) {
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnCounterCountReached>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }


    private fun TriggerCondition.OnCounterCountReached.toUiState(context: Context, canBeSaved: Boolean, hasChanged: Boolean): CounterReachedConditionUiState {
        val counterToChange = UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(counterName))
        val operand = counterValue.toUiStaticOrCounterSelection()

        return CounterReachedConditionUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasChanged,
            name = name,
            nameError = name.isEmpty(),
            counter = counterToChange,
            operator = comparisonOperation.toCounterOperatorDropdownItem(),
            operandValue = operand,
            conditionEffectText = comparisonOperation.toEffectDescription(
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
