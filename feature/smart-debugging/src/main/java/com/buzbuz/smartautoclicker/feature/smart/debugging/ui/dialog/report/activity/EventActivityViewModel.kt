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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class EventActivityViewModel @Inject constructor(
    debuggingRepository: DebuggingRepository,
    smartRepository: IRepository,
) : ViewModel() {

    private val sort = MutableStateFlow(EventActivitySort.SCENARIO_ORDER)

    val uiState: StateFlow<EventActivityUiState> = debuggingRepository.getLastReportOverview()
        .flatMapLatest { overview ->
            if (overview == null) return@flatMapLatest flowOf(EventActivityUiState.NotAvailable)

            combine(
                smartRepository.getScreenEventsFlow(overview.scenarioId),
                smartRepository.getTriggerEventsFlow(overview.scenarioId),
                debuggingRepository.getLastReportEventsOccurrences(),
                sort,
            ) { screenEvents, triggerEvents, occurrences, selectedSort ->
                if (occurrences == null) return@combine EventActivityUiState.NotAvailable

                val report = buildEventActivityReport(
                    screenEvents = screenEvents.toScreenEventActivitySources(),
                    triggerEvents = triggerEvents.toTriggerEventActivitySources(),
                    occurrences = occurrences,
                    sort = selectedSort,
                )
                report.toUiState(selectedSort)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EventActivityUiState.Loading,
        )

    fun setSort(selectedSort: EventActivitySort) {
        sort.update { selectedSort }
    }

    fun getSort(): EventActivitySort = sort.value

    private fun EventActivityReport.toUiState(sort: EventActivitySort): EventActivityUiState {
        if (screenEvents.isEmpty() && triggerEvents.isEmpty()) return EventActivityUiState.Empty

        return EventActivityUiState.Available(
            items = buildList {
                if (screenEvents.isNotEmpty()) {
                    add(EventActivityListItem.Header(EventActivityType.SCREEN))
                    addAll(screenEvents.map(EventActivityListItem::Event))
                }
                if (triggerEvents.isNotEmpty()) {
                    add(EventActivityListItem.Header(EventActivityType.TRIGGER))
                    addAll(triggerEvents.map(EventActivityListItem::Event))
                }
            },
            sort = sort,
        )
    }
}
