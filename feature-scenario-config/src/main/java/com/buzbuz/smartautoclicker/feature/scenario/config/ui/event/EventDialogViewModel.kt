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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event

import android.app.Application

import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.NavigationViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class EventDialogViewModel(application: Application) : NavigationViewModel(application) {

    /** Repository containing the user editions. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The event currently configured. */
    private val configuredEvent: Flow<Event> = editionRepository.editedEvent
        .filterNotNull()

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = configuredEvent
        .map { configuredItem ->
            buildMap {
                put(R.id.page_event, configuredItem.name.isNotEmpty())
                put(R.id.page_conditions, configuredItem.conditions.isValidConditionList())
                put(R.id.page_actions, configuredItem.actions.isValidActionList())
            }
        }

    /** Tells if the configured event is valid and can be saved. */
    val eventCanBeSaved: Flow<Boolean> = navItemsValidity
        .map { itemsValidity ->
            var allValid = true
            itemsValidity.values.forEach { validity ->
                allValid = allValid && validity
            }
            allValid
        }

    /** Tells if this event have associated end conditions. */
    fun isEventHaveRelatedEndConditions(): Boolean = editionRepository.isEditedEventUsedByEndCondition()

    /** Tells if this event have associated actions. */
    fun isEventHaveRelatedActions(): Boolean = editionRepository.isEditedEventUsedByAction()

    private fun List<Action>?.isValidActionList(): Boolean {
        if (this == null) return false
        forEach { if (!it.isComplete()) return false }
        return true
    }

    private fun List<Condition>?.isValidConditionList(): Boolean {
        if (this == null) return false
        forEach { if (!it.isComplete()) return false }
        return true
    }
}