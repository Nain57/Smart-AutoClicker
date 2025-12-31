/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugEventOccurrenceEventState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Get the state of all image events at the given event occurrence.
 * The output will be ordered by priority.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetDebugOccurrenceAllImageEventStateUseCase @Inject constructor(
    private val smartRepository: IRepository,
    debuggingRepository: DebuggingRepository,
) {

    private val events: Flow<List<ImageEvent>?> = debuggingRepository.getLastReportOverview()
        .map { overview ->
            overview ?: return@map null
            smartRepository.getImageEvents(overview.scenarioId)
        }

    private val eventOccurrences: Flow<List<DebugReportEventOccurrence>?> =
        debuggingRepository.getLastReportEventsOccurrences()

    operator fun invoke(eventOccurrence: DebugReportEventOccurrence): Flow<List<DebugEventOccurrenceEventState.Image>> =
        combine(events, eventOccurrences) { events, occurrences ->
            if (events == null || occurrences == null) return@combine emptyList()

            // Initialize all events state
            val eventsStateMap: MutableMap<Long, DebugEventOccurrenceEventState.Image> = mutableMapOf()
            events.forEach { event ->
                eventsStateMap.put(
                    key = event.id.databaseId,
                    value = DebugEventOccurrenceEventState.Image(
                        eventId = event.id.databaseId,
                        eventName = event.name,
                        eventPriority = event.priority,
                        currentValue = event.enabledOnStart,
                    )
                )
            }

            // Update events state up to requested event occurrence
            var isRequestedOccurrence: Boolean
            for (occurrence in occurrences) {
                isRequestedOccurrence = occurrence == eventOccurrence

                occurrence.eventStateChanges.forEach { stateChange ->
                    val previousState = eventsStateMap[stateChange.eventId] ?: return@forEach
                    eventsStateMap[stateChange.eventId] = previousState.copy(
                        currentValue = stateChange.newValue,
                        previousValue = if (isRequestedOccurrence) !stateChange.newValue else null,
                    )
                }

                if (isRequestedOccurrence) break
            }

            // Convert to list and ordered it by priority
            eventsStateMap.values.sortedBy { state -> state.eventPriority }
        }
}