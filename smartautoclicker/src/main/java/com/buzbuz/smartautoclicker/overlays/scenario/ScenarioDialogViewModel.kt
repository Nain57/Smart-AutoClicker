/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.scenario

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import com.buzbuz.smartautoclicker.baseui.OverlayController

import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.overlays.base.NavigationViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioDialogViewModel(application: Application) : NavigationViewModel(application) {

    /** */
    private val repository: Repository = Repository.getRepository(application)
    /** */
    private val configuredScenarioId: MutableStateFlow<Long?> = MutableStateFlow(null)

    private val contentSaveState: MutableMap<Int, Boolean> = mutableMapOf()

    private val _saveEnabledState = MutableStateFlow(true)
    val saveEnabledState: StateFlow<Boolean> = _saveEnabledState

    val scenarioName: Flow<String> = configuredScenarioId
        .filterNotNull()
        .flatMapLatest { repository.getScenario(it) }
        .map { it.name }

    /** */
    fun setConfiguredScenario(scenarioId: Long) {
        configuredScenarioId.value = scenarioId
    }

    fun setSaveButtonState(contentId: Int, newState: Boolean) {
        if (contentSaveState[contentId] == newState) return

        contentSaveState[contentId] = newState

        var isEnabled = true
        contentSaveState.values.forEach { enabledForContent ->
            isEnabled = isEnabled && enabledForContent
        }

        _saveEnabledState.value = isEnabled
    }
}