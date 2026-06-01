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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.CounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.allCounterOperatorDropdownItems
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toAreaDisplayText
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toFullNameRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.toComparisonOperation
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.toCounterOperatorDropdownItem

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

    val operatorDropdownItems = allCounterOperatorDropdownItems()


    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setComparisonOperator(item: CounterOperatorDropdownItem) {
        updateEditedCondition { old -> old.copy(comparisonOperation = item.toComparisonOperation()) }
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
            isNumberValue = counterValue is CounterOperationValue.Number,
            valueText = counterValue.value.toString(),
            conditionEffectDesc = counterValue.toEffectDescription(context, comparisonOperation),
        )

    private fun CounterOperationValue.toEffectDescription(context: Context, operation: ComparisonOperation): String =
        when (this) {
            is CounterOperationValue.Counter -> context.getString(
                R.string.message_number_condition_counter_value_desc,
                context.getString(operation.toFullNameRes()),
                value,
            )
            is CounterOperationValue.Number -> context.getString(
                R.string.message_number_condition_static_value_desc,
                context.getString(operation.toFullNameRes()),
                value.toString(),
            )
        }
}