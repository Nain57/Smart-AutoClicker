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
package com.buzbuz.smartautoclicker.overlays.config.scenario.eventlist

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultEvent
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class EventListViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository of the application. */
    private val repository: Repository = Repository.getRepository(application)

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Currently configured scenario. */
    private val configuredScenario = editionRepository.editedScenario
        .filterNotNull()

    /** List of events for the scenario specified in [configuredScenario]. */
    val eventsItems: Flow<List<EditedEvent>?> = configuredScenario.map { it.events }
    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> = repository.getAllEvents().map { it.isNotEmpty() }

    /**
     * Creates a new event item.
     * @param context the Android context.
     * @return the new event item.
     */
    fun createNewEvent(context: Context, event: Event? = null): EditedEvent {
        editionRepository.editedScenario.value?.let { confScenario ->
            return editionRepository.createNewEvent(
                event = event ?: newDefaultEvent(
                    context = context,
                    scenarioId = confScenario.scenario.id,
                    scenarioEventsSize = confScenario.events.size,
                )
            )
        } ?: throw IllegalStateException("No scenario defined !")
    }

    fun startEventEdition(event: EditedEvent) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.commitEditedEventToEditedScenario()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEventFromEditedScenario()

    /** Update the priority of the events in the scenario. */
    fun updateEventsPriority(events: List<EditedEvent>) = editionRepository.updateEventsPriority(events)
}
