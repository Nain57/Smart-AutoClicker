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

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugEventOccurrenceCounterState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.collections.toList

/**
 * Get the state of all counters for the given event occurrence.
 * The output will be ordered by name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetDebugOccurrenceAllCounterStateUseCase @Inject constructor(debuggingRepository: DebuggingRepository) {

    private val overview: Flow<DebugReportOverview?> = debuggingRepository.getLastReportOverview()
    private val eventOccurrences: Flow<List<DebugReportEventOccurrence>?> =
        debuggingRepository.getLastReportEventsOccurrences()

    operator fun invoke(eventOccurrence: DebugReportEventOccurrence): Flow<List<DebugEventOccurrenceCounterState>> =
        combine(overview, eventOccurrences) { overview, occurrences ->
            if (overview == null || occurrences == null) return@combine emptyList()

            // Initialize all counters state
            val countersStateMap = mutableMapOf<String, DebugEventOccurrenceCounterState>()
            overview.counterNames.forEach { counterName ->
                countersStateMap.put(
                    counterName,
                    DebugEventOccurrenceCounterState(counterName = counterName, currentValue = 0)
                )
            }

            // Update counters state up to requested event occurrence
            var isRequestedOccurrence: Boolean
            for (occurrence in occurrences) {
                isRequestedOccurrence = occurrence == eventOccurrence

                // If several action changes the same counter, we need to merge them
                val occurrenceChanges = mutableMapOf<String, DebugEventOccurrenceCounterState>()
                occurrence.counterChanges.forEach { counterChange ->

                    // Only provide a previous value if this event occurrence changed it
                    // If we already have a change for this occurrence, keep the previous value.
                    val previousChange = occurrenceChanges[counterChange.counterName]
                    occurrenceChanges[counterChange.counterName] = DebugEventOccurrenceCounterState(
                        counterName = counterChange.counterName,
                        currentValue = counterChange.newValue,
                        previousValue = when {
                            isRequestedOccurrence && previousChange != null -> previousChange.previousValue
                            isRequestedOccurrence -> counterChange.previousValue
                            else -> null
                        }
                    )
                }

                // Report merged changes in global state
                countersStateMap.putAll(occurrenceChanges)

                if (isRequestedOccurrence) break
            }

            // Convert to list and ordered it by name
            countersStateMap.values
                .toList()
                .sortedBy { state -> state.counterName }
        }

}
