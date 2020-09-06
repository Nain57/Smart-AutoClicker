/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.clicks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AndroidViewModel for create/delete/list click scenarios from an LifecycleOwner. */
class ScenarioViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository providing access to the click database. */
    private val clickRepository = ClickRepository.getRepository(application)

    /** LiveData upon the list of scenarios. */
    val clickScenario = clickRepository.scenarios

    /**
     * Create a new click scenario.
     *
     * @param name the name of this new scenario.
     */
    fun createScenario(name: String) {
        viewModelScope.launch(Dispatchers.IO) { clickRepository.createScenario(name) }
    }

    /**
     * Delete a click scenario.
     *
     * This will also delete all clicks associated with the scenario.
     *
     * @param scenario the scenario to be deleted.
     */
    fun deleteScenario(scenario: ScenarioEntity) {
        viewModelScope.launch(Dispatchers.IO) { clickRepository.deleteScenario(scenario) }
    }
}