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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.trigger.timer

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TimerReachedConditionViewModel(application: Application) : AndroidViewModel(application) {

    private val msItem = DropdownItem(R.string.item_title_time_unit_ms)
    private val sItem = DropdownItem(R.string.item_title_time_unit_s)
    private val minItem = DropdownItem(R.string.item_title_time_unit_min)
    private val hItem = DropdownItem(R.string.item_title_time_unit_h)

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)

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

    private val _selectedUnitItem: MutableStateFlow<DropdownItem> =
        MutableStateFlow(findAppropriateTimeUnit())
    val selectedUnitItem: Flow<DropdownItem> = _selectedUnitItem
    val unitDropdownItems = listOf(msItem, sItem, minItem, hItem)

    /** The display duration of the pause. */
    val duration: Flow<String> = _selectedUnitItem
        .flatMapLatest { unitItem ->
            configuredCondition
                .map { unitItem.formatDuration(it.durationMs) }
                .take(1)
        }

    /** Tells if the pause duration value is valid or not. */
    val durationError: Flow<Boolean> = configuredCondition.map { it.durationMs <= 0 }

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
            val newDurationMs = when {
                duration == null -> -1
                _selectedUnitItem.value == sItem -> duration * 1.seconds.inWholeMilliseconds
                _selectedUnitItem.value == minItem -> duration * 1.minutes.inWholeMilliseconds
                _selectedUnitItem.value == hItem -> duration * 1.hours.inWholeMilliseconds
                else -> duration
            }

            if (oldValue.durationMs == newDurationMs) null
            else oldValue.copy(
                durationMs = newDurationMs
            )
        }
    }

    fun setTimeUnit(unit: DropdownItem) {
        _selectedUnitItem.value = unit
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

    private fun findAppropriateTimeUnit(): DropdownItem =
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnTimerReached>()?.let { condition ->
            when {
                condition.durationMs <= 0L -> msItem
                condition.durationMs % 1.hours.inWholeMilliseconds == 0L -> hItem
                condition.durationMs % 1.minutes.inWholeMilliseconds == 0L -> minItem
                condition.durationMs % 1.seconds.inWholeMilliseconds == 0L -> sItem
                else -> msItem
            }
        } ?: msItem

    private fun DropdownItem.formatDuration(durationMs: Long): String =
        when (this) {
            sItem -> durationMs / 1.seconds.inWholeMilliseconds
            minItem -> durationMs / 1.minutes.inWholeMilliseconds
            hItem -> durationMs / 1.hours.inWholeMilliseconds
            else -> durationMs
        }.toString()
}