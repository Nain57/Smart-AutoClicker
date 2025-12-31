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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugEventOccurrenceEventState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugOccurrenceAllImageEventStateUseCase
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugOccurrenceAllTriggerEventStateUseCase
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.findWithId

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class DebugEventsStateContentViewModel @Inject constructor(
    private val smartRepository: IRepository,
    private val getAllImgEvtStateUseCase: GetDebugOccurrenceAllImageEventStateUseCase,
    private val getAllTrigEvtStateUseCase: GetDebugOccurrenceAllTriggerEventStateUseCase,
) : ViewModel() {

    private val eventOccurrence: MutableStateFlow<Pair<Event?, DebugReportEventOccurrence?>> =
        MutableStateFlow(Pair(null, null))


    private val imageEventsState: Flow<List<DebugEventStateItem.EventState>?> = eventOccurrence
        .flatMapLatest { (event, occurrence) ->
            if (event == null || occurrence == null) flowOf(null)
            else getAllImgEvtStateUseCase(occurrence).map { states -> states.toUiItems() }
        }

    private val triggerEventsState: Flow<List<DebugEventStateItem.EventState>?> = eventOccurrence
        .flatMapLatest { (event, occurrence) ->
            if (event == null || occurrence == null) flowOf(null)
            else getAllTrigEvtStateUseCase(occurrence).map { states -> states.toUiItems() }
        }

    val uiState: StateFlow<DebugEventsStateContentUiState> =
        combine(imageEventsState, triggerEventsState) { imgEvents, trigEvents ->
            if (imgEvents.isNullOrEmpty() || trigEvents.isNullOrEmpty())
                return@combine DebugEventsStateContentUiState.Empty

            DebugEventsStateContentUiState.Available(
                eventsState = buildEventsStateItems(
                    imgItems = imgEvents,
                    trigItems = trigEvents,
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugEventsStateContentUiState.Loading,
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

    private fun List<DebugEventOccurrenceEventState>.toUiItems(): List<DebugEventStateItem.EventState> =
        map { state ->
            DebugEventStateItem.EventState(
                eventId = state.eventId.toString(),
                eventName = state.eventName,
                isEnabled = state.currentValue,
                haveChanged = state.previousValue != null,
            )
        }

    private fun buildEventsStateItems(
        imgItems: List<DebugEventStateItem>,
        trigItems: List<DebugEventStateItem>,
    ): List<DebugEventStateItem> = buildList {

        if (imgItems.isNotEmpty()) {
            add(getImageEventsHeader())
            addAll(imgItems)
        }

        if (trigItems.isNotEmpty()) {
            add(getTriggerEventsHeader())
            addAll(trigItems)
        }
    }

    private fun getImageEventsHeader(): DebugEventStateItem.Header =
        DebugEventStateItem.Header(
            title = R.string.item_event_state_header_image,
            icon = R.drawable.ic_condition,
        )

    private fun getTriggerEventsHeader(): DebugEventStateItem.Header =
        DebugEventStateItem.Header(
            title = R.string.item_event_state_header_trigger,
            icon = R.drawable.ic_trigger_event,
        )
}