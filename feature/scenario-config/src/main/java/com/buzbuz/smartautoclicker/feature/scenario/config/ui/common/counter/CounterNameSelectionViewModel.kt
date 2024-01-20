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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.common.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CounterNameSelectionViewModel (application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)

    val counterNames: Flow<Set<String>> = editionRepository.editionState.allEditedEvents
        .combine(editionRepository.editionState.editedEventActionsState) { allEditedEvents, currentActions ->
            buildSet {
                allEditedEvents.forEach { event ->
                    event.conditions.forEach { condition ->
                        if (condition is TriggerCondition.OnCounterCountReached) add(condition.counterName)
                    }
                    event.actions.forEach { action ->
                        if (action is Action.ChangeCounter) add(action.counterName)
                    }
                }

                currentActions.value?.forEach { action ->
                    if (action is Action.ChangeCounter) add(action.counterName)
                }
            }
        }
}
