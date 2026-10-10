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
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.condition.GetImageConditionBitmapJobUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiConditionPositionTypeDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiStaticOrCounterSelection
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.toAffectationOperation
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.toCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.toDisplayValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.toDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.toPositionType
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    private val monitoredViewsManager: MonitoredViewsManager,
    private val getImageConditionBitmapJobUseCase: GetImageConditionBitmapJobUseCase,
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
        editionRepository.editionState.editedEventState.map { it.value is ScreenEvent },
        editionRepository.editionState.editedEventScreenConditionsState,
    ) { action, hasChanged, canBeSaved, isScreenEvent, conditionsState ->
        action.toUiState(
            context = context,
            canBeSaved = canBeSaved,
            hasChanged = hasChanged,
            isScreenEvent = isScreenEvent,
            availableConditions = conditionsState.value?.filter { it.shouldBeDetected } ?: emptyList(),
        )
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

    fun setOperandType(type: UiOperandType) {
        // Do nothing if this is the same operand
        val currentOperand = uiState.value?.operandValue
        if (currentOperand is UiStaticOrCounterSelection.CounterValue && type == UiOperandType.COUNTER) return
        if (currentOperand is UiStaticOrCounterSelection.StaticValue && type == UiOperandType.STATIC) return
        if (currentOperand is UiStaticOrCounterSelection.ConditionValue && type == UiOperandType.CONDITION) return

        // Change operand and use default value
        setOperationValue(
            when (type) {
                UiOperandType.STATIC -> CounterOperationValue.Number(0.0)
                UiOperandType.COUNTER -> CounterOperationValue.Counter("")
                UiOperandType.CONDITION -> CounterOperationValue.Condition(
                    conditionId = null,
                    positionType = CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X,
                )
            }
        )
    }

    fun setOperandCondition(condition: ScreenCondition) {
        updateEditedChangeCounter { old ->
            val positionType = (old.operationValue as? CounterOperationValue.Condition)?.positionType
                ?: CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X

            old.copy(operationValue = CounterOperationValue.Condition(condition.id, positionType))
        }
    }

    fun setOperandConditionPositionType(item: UiConditionPositionTypeDropdownItem) {
        updateEditedChangeCounter { old ->
            val value = old.operationValue as? CounterOperationValue.Condition ?: return@updateEditedChangeCounter old
            old.copy(operationValue = value.copy(positionType = item.toPositionType()))
        }
    }

    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmapJobUseCase(viewModelScope, condition, onBitmapLoaded)

    fun setOperationValue(value: CounterOperationValue) {
        updateEditedChangeCounter { old ->
            old.copy(operationValue = value)
        }
    }

    fun monitorSelectCounterView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.COUNTER_ACTION_DIALOG_FIELD_SELECT_COUNTER, view)
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.COUNTER_ACTION_DIALOG_BUTTON_SAVE, view)
    }

    fun detachMonitoredViews() {
        monitoredViewsManager.detach(MonitoredViewType.COUNTER_ACTION_DIALOG_FIELD_SELECT_COUNTER)
        monitoredViewsManager.detach(MonitoredViewType.COUNTER_ACTION_DIALOG_BUTTON_SAVE)
    }

    private fun updateEditedChangeCounter(closure: (old: ChangeCounter) -> ChangeCounter) {
        editionRepository.editionState.getEditedAction<ChangeCounter>()?.let { old ->
            editionRepository.updateEditedAction(closure(old))
        }
    }

    private fun ChangeCounter.toUiState(
        context: Context,
        canBeSaved: Boolean,
        hasChanged: Boolean,
        isScreenEvent: Boolean,
        availableConditions: List<ScreenCondition>,
    ): ChangeCounterUiState {
        val counterToChange = UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(counterName))
        val operand = operationValue.toUiStaticOrCounterSelection(availableConditions)

        return ChangeCounterUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasChanged,
            name = name,
            nameError = name?.isEmpty() ?: true,
            counter = counterToChange,
            operator = operation.toCounterOperatorDropdownItem(),
            operandValue = operand,
            isConditionOperandAvailable = isScreenEvent,
            availableConditions = availableConditions,
            actionEffectText = operation.toEffectDescription(
                context = context,
                counterName = counterName,
                operand = operand.toDisplayValue(context),
            )
        )
    }

    private fun CounterOperationValue.toUiStaticOrCounterSelection(
        availableConditions: List<ScreenCondition>,
    ): UiStaticOrCounterSelection =
        when (this) {
            is CounterOperationValue.Counter ->
                UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(counterName))

            is CounterOperationValue.Number ->
                UiStaticOrCounterSelection.StaticValue(value)

            is CounterOperationValue.Condition ->
                UiStaticOrCounterSelection.ConditionValue(
                    condition = conditionId?.let { id -> availableConditions.find { it.id == id } },
                    positionType = positionType.toDropdownItem(),
                    hasAvailableConditions = availableConditions.isNotEmpty(),
                )
        }
}
