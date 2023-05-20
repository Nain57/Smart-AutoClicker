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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.scenario

import android.app.Application

import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.NavigationViewModel

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** ViewModel for the [ScenarioDialog] and its content. */
class ScenarioDialogViewModel(application: Application) : NavigationViewModel(application) {

    /** */
    private val editionRepository: EditionRepository = EditionRepository.getInstance(application)

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = combine(
        editionRepository.isEventListValid.filterNotNull(),
        editionRepository.editedScenario.filterNotNull(),
    ) { isEventListValid, scenario ->
        buildMap {
            put(R.id.page_events, isEventListValid)
            put(R.id.page_config, scenario.name.isNotEmpty())
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
}
