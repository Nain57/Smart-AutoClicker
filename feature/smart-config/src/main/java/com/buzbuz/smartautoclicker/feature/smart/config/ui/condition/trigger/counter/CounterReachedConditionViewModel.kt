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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.*
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
class CounterReachedConditionViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val greaterItem = DropdownItem(R.string.item_title_greater)
    private val greaterOrEqualsItem = DropdownItem(R.string.item_title_greater_or_equals)
    private val equalsItem = DropdownItem(R.string.item_title_equals)
    private val lowerOrEqualsItem = DropdownItem(R.string.item_title_lower_or_equals)
    private val lowerItem = DropdownItem(R.string.item_title_lower)

    /** The condition being configured by the user. */
    private val configuredCondition: Flow<TriggerCondition.OnCounterCountReached> =
        editionRepository.editionState.editedTriggerConditionState
            .mapNotNull { it.value }
            .filterIsInstance<TriggerCondition.OnCounterCountReached>()

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    val counterName: Flow<String?> = configuredCondition
        .map { it.counterName }
        .take(1)
    val counterNameError: Flow<Boolean> = configuredCondition.map { it.counterName.isEmpty() }

    val comparisonValueText: Flow<String?> = configuredCondition
        .map { it.counterValue.toString() }
        .take(1)

    val operatorDropdownItems = listOf(greaterItem, greaterOrEqualsItem, equalsItem, lowerOrEqualsItem, lowerItem)
    val operatorDropdownState: Flow<DropdownItem> = configuredCondition
        .map { condition ->
            when (condition.comparisonOperation) {
                GREATER -> greaterItem
                GREATER_OR_EQUALS -> greaterOrEqualsItem
                EQUALS -> equalsItem
                LOWER_OR_EQUALS -> lowerOrEqualsItem
                LOWER -> lowerItem
            }
        }

    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedTriggerConditionState.map { condition ->
        condition.canBeSaved
    }

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setCounterName(counterName: String) {
        updateEditedCondition { old -> old.copy(counterName = "" + counterName) }
    }

    fun setComparisonOperator(item: DropdownItem) {
        updateEditedCondition { old ->
            old.copy(
                comparisonOperation = when (item) {
                    greaterItem -> GREATER
                    greaterOrEqualsItem -> GREATER_OR_EQUALS
                    equalsItem -> EQUALS
                    lowerOrEqualsItem -> LOWER_OR_EQUALS
                    lowerItem -> LOWER
                    else -> return@updateEditedCondition null
                }
            )
        }
    }

    fun setComparisonValue(value: Int?) {
        updateEditedCondition { old -> old.copy(counterValue = value ?: -1) }
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
}