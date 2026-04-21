/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.events

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.DebugReportTimelineFilter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class FilteredEventsSelectorViewModel @Inject constructor(
    debuggingRepository: DebuggingRepository,
    smartRepository: IRepository,
) : ViewModel() {

    /** The filter defined by the user when opening the dialog. */
    private val userFilter: MutableStateFlow<DebugReportTimelineFilter.Events?> = MutableStateFlow(null)
    /** Contains the ids of the events to be filtered. Basically the inverse of the checkbox state. */
    private val filteredIds: MutableStateFlow<Set<Long>> = MutableStateFlow(emptySet())

    /** The scenario referenced by the last debug report. Null if not found or no reports. */
    private val scenario: Flow<Scenario?> = debuggingRepository.getLastReportOverview()
        .map { overview ->
            overview?.scenarioId?.let { id -> smartRepository.getScenario(id) }
        }

    /** The events of the scenario referenced by the last debug report. Null if not found or no reports. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allEvents: Flow<List<Event>?> = scenario
        .combine(userFilter) { scenario, filter -> scenario to filter }
        .flatMapLatest { (scenario, filter) ->
            scenario?.id?.databaseId?.let { dbId ->
                when (filter) {
                    is DebugReportTimelineFilter.Events.Image -> smartRepository.getImageEventsFlow(dbId)
                    is DebugReportTimelineFilter.Events.Trigger -> smartRepository.getTriggerEventsFlow(dbId)
                    null -> flowOf(null)
                }
            } ?: flowOf(null)
        }

    val eventsItems: Flow<List<FilteredEventsSelectorItem>> = combine(allEvents, filteredIds) { events, ids ->
        events?.map { event ->
            FilteredEventsSelectorItem(
                eventId = event.id.databaseId,
                eventName = event.name,
                eventState = !ids.contains(event.id.databaseId),
            )
        } ?: emptyList()
    }

    fun setEventFilter(filter: DebugReportTimelineFilter.Events) {
        userFilter.update { filter }
        filteredIds.update { filter.filteredIds }
    }

    fun setFilteredState(eventId: Long, state: Boolean) {
        filteredIds.update { ids ->
             ids.toMutableSet().apply {
                 if (state) remove(eventId)
                 else add(eventId)
             }
        }
    }

    fun getFilter(): DebugReportTimelineFilter.Events =
        userFilter.value?.copyFilter(filteredIds = filteredIds.value)
            ?: throw IllegalStateException("Can't save a null filter")
}