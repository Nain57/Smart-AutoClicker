/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent
import com.buzbuz.smartautoclicker.overlays.base.NavigationViewModel
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

/** ViewModel for the [ScenarioDialog] and its content. */
class ScenarioDialogViewModel(application: Application) : NavigationViewModel(application) {

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = EditionRepository.getInstance(application).editedScenario
        .filterNotNull()
        .map { configuredItem ->
            buildMap {
                put(R.id.page_events, configuredItem.events.isEventListValid())
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

    /**
     * Check the validity of the event list.
     * It must be not empty, and all events must have at least one action and one condition.
     *
     * @return true if valid, false if not.
     */
    private fun List<EditedEvent>.isEventListValid(): Boolean {
        if (isEmpty()) return false
        forEach { if (!it.event.isComplete()) return false }
        return true
    }
}
