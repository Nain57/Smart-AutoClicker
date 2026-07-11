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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.number

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.ViewPositioningType

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiNumberFormatDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toNumberFormatType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toAreaDisplayText
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toEffectDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toComparisonOperation
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.toCounterOperatorDropdownItem

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

class NumberConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Number>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<NumberConditionUiState?> = configuredCondition
        .map { numberCondition -> numberCondition.toUiState(context) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)


    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setComparisonOperator(item: UiCounterOperatorDropdownItem) {
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

    fun setDetectionArea(area: Rect) {
        updateEditedCondition {
            it.copy(detectionArea = area)
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun setNumberFormat(item: UiNumberFormatDropdownItem) {
        updateEditedCondition { it.copy(numberFormatType = item.toNumberFormatType()) }
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.NUMBER_CONDITION_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorValueToDetectField(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT, view)
    }

    fun monitorOperatorField(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN, view)
    }

    fun monitorDetectionAreaField(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_AREA_SELECTOR, view)
    }

    fun monitorDropdownItem(item: UiCounterOperatorDropdownItem, view: View?) {
        if (item !is UiCounterOperatorDropdownItem.Comparison.GreaterItem) return

        if (view != null) {
            monitoredViewsManager.attach(
                type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN_ITEM_GREATER,
                monitoredView = view,
            )
        } else {
            monitoredViewsManager.detach(
                MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN_ITEM_GREATER,
            )
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.NUMBER_CONDITION_DIALOG_BUTTON_SAVE)
        monitoredViewsManager.detach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT)
        monitoredViewsManager.detach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN)
        monitoredViewsManager.detach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN_ITEM_GREATER)
        monitoredViewsManager.detach(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_AREA_SELECTOR)
    }

    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Number) -> ScreenCondition.Number?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    private fun ScreenCondition.Number.toUiState(context: Context): NumberConditionUiState =
        NumberConditionUiState(
            canBeSaved = isComplete(),
            name = name,
            nameError = name.isEmpty(),
            detectionAreaDescription = detectionArea.toAreaDisplayText(context),
            detectionAreaError = detectionArea.isEmpty,
            detectionThreshold = threshold,
            selectorOperatorDropdownItem = comparisonOperation.toCounterOperatorDropdownItem(),
            operandValue = counterValue.toUiStaticOrCounterSelection(),
            conditionEffectDesc = counterValue.toEffectDescription(context, comparisonOperation),
            numberFormatDropdownItem = numberFormatType.toDropdownItem(),
        )

    private fun CounterOperationValue.toUiStaticOrCounterSelection(): UiStaticOrCounterSelection =
        when (this) {
            is CounterOperationValue.Counter ->
                UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(value))

            is CounterOperationValue.Number ->
                UiStaticOrCounterSelection.StaticValue(value)
        }
}