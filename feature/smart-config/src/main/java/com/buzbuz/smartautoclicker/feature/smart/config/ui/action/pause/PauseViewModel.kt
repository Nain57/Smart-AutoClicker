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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause

import android.content.Context
import android.content.SharedPreferences

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.findAppropriateTimeUnit
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.formatDuration
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toDurationMs
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putPauseDurationConfig
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PauseViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The action being configured by the user. */
    private val configuredPause = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Action.Pause>()
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the pause. */
    val name: Flow<String?> = configuredPause
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredPause.map { it.name?.isEmpty() ?: true }

    private val _selectedUnitItem: MutableStateFlow<TimeUnitDropDownItem> = MutableStateFlow(
        editionRepository.editionState.getEditedAction<Action.Pause>()?.let { action ->
            action.pauseDuration.findAppropriateTimeUnit()
        } ?: TimeUnitDropDownItem.Milliseconds
    )
    val selectedUnitItem: Flow<TimeUnitDropDownItem> = _selectedUnitItem

    /** The duration of the pause in milliseconds. */
    val pauseDuration: Flow<String?> = _selectedUnitItem
        .flatMapLatest { unitItem ->
            configuredPause
                .map { unitItem.formatDuration(it.pauseDuration ?: 0) }
                .take(1)
        }
    /** Tells if the pause duration value is valid or not. */
    val pauseDurationError: Flow<Boolean> = configuredPause.map { (it.pauseDuration ?: -1) <= 0 }

    /** Tells if the configured pause is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    /**
     * Set the name of the pause.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Action.Pause>()?.let { pause ->
            editionRepository.updateEditedAction(pause.copy(name = "" + name))
        }
    }

    /**
     * Set the duration of the pause.
     * @param duration the new duration.
     */
    fun setPauseDuration(duration: Long?) {
        editionRepository.editionState.getEditedAction<Action.Pause>()?.let { oldPause ->
            val newDurationMs = duration.toDurationMs(_selectedUnitItem.value)

            if (oldPause.pauseDuration != newDurationMs) {
                editionRepository.updateEditedAction(oldPause.copy(pauseDuration = newDurationMs))
            }
        }
    }

    fun setTimeUnit(unit: DropdownItem) {
        _selectedUnitItem.value = unit as? TimeUnitDropDownItem ?: TimeUnitDropDownItem.Milliseconds
    }

    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Action.Pause>()?.let { pause ->
            sharedPreferences.edit().putPauseDurationConfig(pause.pauseDuration ?: 0).apply()
        }
    }
}