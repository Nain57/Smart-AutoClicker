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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugEventOccurrenceCounterState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugOccurrenceAllCounterStateUseCase
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.findWithId

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class DebugCounterStateContentViewModel @Inject constructor(
    private val smartRepository: IRepository,
    private val getAllCounterStateUseCase: GetDebugOccurrenceAllCounterStateUseCase,
) : ViewModel() {

    private val eventOccurrence: MutableStateFlow<Pair<Event?, DebugReportEventOccurrence?>> =
        MutableStateFlow(Pair(null, null))

    private val countersState: Flow<List<DebugEventOccurrenceCounterState>?> =
        eventOccurrence.flatMapLatest { (event, occurrence) ->
            if (event == null || occurrence == null) flowOf(null)
            else getAllCounterStateUseCase(occurrence)
        }

    val uiState: StateFlow<DebugCounterStateContentUiState> = countersState
        .map { state ->
            when {
                state == null -> DebugCounterStateContentUiState.Loading
                state.isEmpty() -> DebugCounterStateContentUiState.Empty
                else -> DebugCounterStateContentUiState.Available(state.toUiItems())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugCounterStateContentUiState.Loading,
        )

    fun setOccurrence(scenarioId: Long, occurrence: DebugReportEventOccurrence) {
        viewModelScope.launch {
            val event = when (occurrence) {
                is DebugReportEventOccurrence.ImageEvent -> smartRepository.getImageEvents(scenarioId)
                is DebugReportEventOccurrence.TriggerEvent -> smartRepository.getTriggerEvents(scenarioId)
            }.findWithId(occurrence.eventId) ?: return@launch

            eventOccurrence.update { event to occurrence }
        }
    }

    private fun List<DebugEventOccurrenceCounterState>.toUiItems(): List<CounterStateItem> =
        map { state ->
            CounterStateItem(
                counterName = state.counterName,
                currentCounterValue = state.currentValue,
                oldCounterValue = state.previousValue,
            )
        }
}