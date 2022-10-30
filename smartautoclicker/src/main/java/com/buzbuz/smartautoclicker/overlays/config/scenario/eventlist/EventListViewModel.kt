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
package com.buzbuz.smartautoclicker.overlays.config.scenario.eventlist

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultEvent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EventListViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)

    /** Backing property for [scenarioId]. */
    private val _scenarioId = MutableStateFlow<Long?>(null)
    /** Currently selected scenario via [setScenarioId]. */
    val scenarioId: StateFlow<Long?> = _scenarioId
    /** The currently selected scenario. */
    private val scenario: StateFlow<Scenario?> = scenarioId
        .flatMapLatest { id ->
            id?.let { repository.getScenario(it) } ?: emptyFlow()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /** List of events for the scenario specified in [scenario]. */
    val eventsItems: StateFlow<List<Event>?> = scenario
        .filterNotNull()
        .flatMapLatest { scenario ->
            repository.getCompleteEventList(scenario.id)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible = repository.getEventCount()
        .map { eventCount ->
            eventCount > 0
        }

    /**
     * Set a scenario for this [EventListViewModel].
     * This will modify the content of [eventsItems].
     *
     * @param scenarioId the scenario value.
     */
    fun setScenarioId(scenarioId: Long) {
        _scenarioId.value = scenarioId
    }

    /**
     * Creates a new event.
     *
     * @param context the Android context.
     * @return the new event.
     */
    fun getNewEvent(context: Context) = newDefaultEvent(
        context = context,
        scenarioId = scenario.value!!.id,
        scenarioEventsSize = eventsItems.value!!.size,
    )

    /**
     * Update the priority of the events in the scenario.
     *
     * @param events the events, ordered by their new priorities. They must be in the current scenario and have a
     *               defined id.
     */
    fun updateEventsPriority(events: List<Event>) {
        if (scenario.value == null || events.isEmpty()) {
            Log.e(TAG, "Can't update click priorities, scenario is not matching.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEventsPriority(events)
        }
    }

    /**
     * Add or update an event.
     * If the event id is unset, it will be added. If not, updated.
     *
     * @param event the event to add/update.
     */
    fun addOrUpdateEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            if (event.id == 0L) {
                repository.addEvent(event)
            } else {
                repository.updateEvent(event)
            }
        }
    }

    /**
     * Delete an event.
     *
     * @param event the event to delete.
     */
    fun deleteEvent(event: Event) {
        if (scenario.value == null) {
            Log.e(
                TAG, "Can't delete click with scenario id $event.scenarioId, " +
                    "invalid model scenario ${scenario.value}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) { repository.removeEvent(event) }
    }
}

private const val TAG = "EventListViewModel"