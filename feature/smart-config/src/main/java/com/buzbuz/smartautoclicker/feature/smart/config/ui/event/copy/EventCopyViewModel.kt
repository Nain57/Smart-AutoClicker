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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.copy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetScreenEventsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetTriggerEventsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.EventsForCopy
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiTriggerEvent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject


/** View model for the [EventCopyDialog]. */
@OptIn(ExperimentalCoroutinesApi::class)
class EventCopyViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    getScreenEventsForCopyUseCase: GetScreenEventsForCopyUseCase,
    getTriggerEventsForCopyUseCase: GetTriggerEventsForCopyUseCase,
) : ViewModel() {

    private val requestTriggerEvents: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    private val searchQuery = MutableStateFlow<String?>(null)
    private val checkedForCopy: MutableStateFlow<Map<Identifier, Event>> = MutableStateFlow(emptyMap())

    private val eventsToCopy: Flow<EventsForCopy<out Event>> = requestTriggerEvents
        .flatMapLatest { isRequestingTriggerEvents ->
            if (isRequestingTriggerEvents == true) getTriggerEventsForCopyUseCase()
            else getScreenEventsForCopyUseCase()
        }

    private val allCopyItems: Flow<List<EventCopyItem>> =
        combine(eventsToCopy, checkedForCopy) { events, checked ->
            buildList {
                if (events.thisScenario.isNotEmpty()) {
                    add(EventCopyItem.Header(R.string.list_header_copy_event_this))
                    addAll(events.thisScenario.toCopyItems(checked).sortedBy { it.name })
                }
                if (events.otherScenario.isNotEmpty()) {
                    add(EventCopyItem.Header(R.string.list_header_copy_event_all))
                    addAll(events.otherScenario.toCopyItems(checked).sortedBy { it.name })
                }
            }
        }

    val uiState: StateFlow<List<EventCopyItem>?> = allCopyItems
        .combine(searchQuery) { allItems, query ->
            if (query.isNullOrEmpty()) allItems
            else allItems
                .filterIsInstance<EventCopyItem.EventItem>()
                .filter { item -> item.name.contains(query, true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setCopyListType(triggerEvents: Boolean) {
        requestTriggerEvents.update { triggerEvents }
    }

    fun toggleCheckedForCopy(event: Event) {
        checkedForCopy.update { old ->
            if (old.contains(event.id)) old - event.id
            else old + (event.id to event)
        }
    }

    fun updateSearchQuery(query: String?) {
        searchQuery.update { query }
    }

    fun eventsCopyShouldWarnUser(): Boolean {
        uiState.value?.forEach { eventItem ->
            if (eventItem is EventCopyItem.EventItem && eventItem.uiEvent.event.isReferencingUnreachableEvent()) {
                return true
            }

        }

        return false
    }

    fun getEventsToCopy(): List<Event> =
        uiState.value?.mapNotNull { item ->
            if (item !is EventCopyItem.EventItem || !item.checked) return@mapNotNull null
            item.uiEvent.event
        } ?: emptyList()


    private fun Event.isReferencingUnreachableEvent(): Boolean =
        editionRepository.editionState.getScenario()?.id == scenarioId
                && actions.find { action -> action is ToggleEvent && !action.toggleAll } != null

    private fun List<Event>.toCopyItems(checked: Map<Identifier, Event>): List<EventCopyItem.EventItem> = map { event ->
        when (event) {
            is ScreenEvent -> EventCopyItem.EventItem.Image(
                name = event.name,
                uiEvent = event.toUiImageEvent(inError = !event.isComplete()),
                actionsIcons = event.actions.map { it.getIconRes() },
                checked = checked.contains(event.id),
            )

            is TriggerEvent -> EventCopyItem.EventItem.Trigger(
                name = event.name,
                uiEvent = event.toUiTriggerEvent(inError = !event.isComplete()),
                checked = checked.contains(event.id),
            )
        }
    }
}
