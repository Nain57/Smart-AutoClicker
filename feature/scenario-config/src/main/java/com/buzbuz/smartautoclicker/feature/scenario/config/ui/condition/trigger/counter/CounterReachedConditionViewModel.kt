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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.trigger.counter

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

@OptIn(FlowPreview::class)
class CounterReachedConditionViewModel(application: Application) : AndroidViewModel(application) {

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

    /** The duration of the pause in milliseconds. */
    val duration: Flow<String?> = configuredCondition
        .map { it.durationMs.toString() }
        .take(1)
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
     * @param durationMs the new duration in milliseconds.
     */
    fun setDuration(durationMs: Long?) {
        updateEditedCondition { it.copy(durationMs = durationMs ?: -1) }
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