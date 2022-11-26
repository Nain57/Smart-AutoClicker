/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.scenario

import android.app.Application

import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavigationViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** ViewModel for the [ScenarioDialog] and its content. */
class ScenarioDialogViewModel(application: Application) : NavigationViewModel(application) {

    /** The repository providing access to the database. */
    private val repository: Repository = Repository.getRepository(application)

    /** The scenario currently configured. Shared with all contents view models. */
    val configuredScenario: MutableStateFlow<ConfiguredScenario?> = MutableStateFlow(null)

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = configuredScenario
        .filterNotNull()
        .map { configuredItem ->
            buildMap {
                put(R.id.page_events, configuredItem.events.isNotEmpty())
                put(R.id.page_config, configuredItem.scenario.name.isNotEmpty())
                put(R.id.page_debug, true)
            }
        }

    /** Tells if the configured scenario is valid and can be saved in database. */
    val scenarioCanBeSaved: Flow<Boolean> = navItemsValidity
        .map { itemsValidity ->
            var allValid = true
            itemsValidity.values.forEach { validity ->
                allValid = allValid && validity
            }
            allValid
        }

    /** Set the scenario to be configured by this viewModel. */
    fun setConfiguredScenario(scenario: Scenario) {
        if (configuredScenario.value != null) return

        viewModelScope.launch(Dispatchers.IO) {
            configuredScenario.value = ConfiguredScenario(
                scenario = scenario,
                events = repository.getCompleteEventList(scenario.id).mapIndexed { index, event ->
                    ConfiguredEvent(event, index)
                },
                endConditions = repository.getScenarioWithEndConditions(scenario.id)?.second ?: emptyList(),
            )
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            configuredScenario.value?.let { conf ->
                repository.updateScenario(conf.scenario)

                val toBeRemoved = repository.getCompleteEventList(conf.scenario.id).toMutableList()
                conf.events.forEachIndexed { index, configuredEvent ->
                    configuredEvent.event.priority = index

                    if (configuredEvent.event.id == 0L) {
                        repository.addEvent(configuredEvent.event)
                    } else {
                        repository.updateEvent(configuredEvent.event)
                        toBeRemoved.removeIf { it.id == configuredEvent.event.id }
                    }
                }
                toBeRemoved.forEach { event -> repository.removeEvent(event) }

                repository.updateEndConditions(conf.scenario.id, conf.endConditions)
            }
        }
    }
}

/** Represents the scenario currently configured. */
data class ConfiguredScenario(
    val scenario: Scenario,
    val events: List<ConfiguredEvent>,
    val endConditions: List<EndCondition>,
)

/** Represents the events of the scenario currently configured. */
data class ConfiguredEvent(val event: Event, val itemId: Int = INVALID_CONFIGURED_EVENT_ITEM_ID)

/** Invalid [ConfiguredEvent] id. The event item object is created but not yet in the list. */
const val INVALID_CONFIGURED_EVENT_ITEM_ID = -1