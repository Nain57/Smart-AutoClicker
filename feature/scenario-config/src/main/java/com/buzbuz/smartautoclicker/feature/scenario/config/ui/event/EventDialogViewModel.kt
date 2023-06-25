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
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class EventDialogViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository containing the user editions. */
    private val editionRepository = EditionRepository.getInstance(application)

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = combine(
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedEventConditionsState,
        editionRepository.editionState.editedEventActionsState,
    ) { editedEvent, conditions, actions, ->
        buildMap {
            put(R.id.page_event, editedEvent.value?.name?.isNotEmpty() ?: false)
            put(R.id.page_conditions, conditions.canBeSaved)
            put(R.id.page_actions, actions.canBeSaved)
        }
    }

    /** Tells if the configured event is valid and can be saved. */
    val eventCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedEventState
        .map { it.canBeSaved }

    /** Tells if this event have associated end conditions. */
    fun isEventHaveRelatedEndConditions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByEndCondition()

    /** Tells if this event have associated actions. */
    fun isEventHaveRelatedActions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByAction()
}