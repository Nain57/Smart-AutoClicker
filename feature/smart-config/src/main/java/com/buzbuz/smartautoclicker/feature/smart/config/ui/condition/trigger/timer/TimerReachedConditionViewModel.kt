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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.findAppropriateTimeUnit
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.formatDuration
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toDurationMs
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TimerReachedConditionViewModel@Inject constructor(
    private val editionRepository: EditionRepository,
): ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition: Flow<TriggerCondition.OnTimerReached> =
        editionRepository.editionState.editedTriggerConditionState
            .mapNotNull { it.value }
            .filterIsInstance<TriggerCondition.OnTimerReached>()

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    /** Tells if the condition name is valid or not. */
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    private val _selectedUnitItem: MutableStateFlow<TimeUnitDropDownItem> = MutableStateFlow(
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnTimerReached>()?.let { condition ->
            condition.durationMs.findAppropriateTimeUnit()
        } ?: TimeUnitDropDownItem.Milliseconds
    )
    val selectedUnitItem: Flow<TimeUnitDropDownItem> = _selectedUnitItem

    /** The display duration of the pause. */
    val duration: Flow<String> = _selectedUnitItem
        .flatMapLatest { unitItem ->
            configuredCondition
                .map { unitItem.formatDuration(it.durationMs) }
                .take(1)
        }

    /** Tells if the pause duration value is valid or not. */
    val durationError: Flow<Boolean> = configuredCondition.map { it.durationMs <= 0 }

    val restartWhenReached: Flow<Boolean> = configuredCondition.map { it.restartWhenReached }

    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedTriggerConditionState.map { condition ->
        condition.canBeSaved
    }

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    /**
     * Set the duration of the pause.
     * @param duration the new duration in milliseconds.
     */
    fun setDuration(duration: Long?) {
        updateEditedCondition { oldValue ->
            val newDurationMs = duration.toDurationMs(_selectedUnitItem.value)

            if (oldValue.durationMs == newDurationMs) null
            else oldValue.copy(
                durationMs = newDurationMs
            )
        }
    }

    fun setTimeUnit(unit: DropdownItem) {
        _selectedUnitItem.value = unit as? TimeUnitDropDownItem ?: TimeUnitDropDownItem.Milliseconds
    }

    fun toggleRestartWhenReached() {
        updateEditedCondition { oldValue ->
            oldValue.copy(restartWhenReached = !oldValue.restartWhenReached)
        }
    }

    private fun updateEditedCondition(
        closure: (oldValue: TriggerCondition.OnTimerReached) -> TriggerCondition.OnTimerReached?,
    ) {
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnTimerReached>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }
}