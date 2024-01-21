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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.copy

import android.app.Application

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getIconRes

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * View model for the [EventCopyDialog].
 *
 * @param application the Android application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventCopyModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    private val requestTriggerEvents: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val searchQuery = MutableStateFlow<String?>(null)

    private val allImageEventsCopyItems: Flow<List<EventCopyItem>> = combine(
        editionRepository.editionState.copyImageEventsFromEditedScenario,
        editionRepository.editionState.copyImageEventsFromOtherScenarios,
    ) { eventsFromThisScenario, eventsFromOtherScenario ->
        buildCopyItemsList(eventsFromThisScenario, eventsFromOtherScenario)
    }

    private val allTriggerEventsCopyItems: Flow<List<EventCopyItem>> = combine(
        editionRepository.editionState.copyTriggerEventsFromEditedScenario,
        editionRepository.editionState.copyTriggerEventsFromOtherScenarios,
    ) { eventsFromThisScenario, eventsFromOtherScenario ->
        buildCopyItemsList(eventsFromThisScenario, eventsFromOtherScenario)
    }

    private val requestedCopyItems: Flow<List<EventCopyItem>> = requestTriggerEvents
        .flatMapLatest { isRequestingTriggerEvents ->
            if (isRequestingTriggerEvents == true) allTriggerEventsCopyItems
            else allImageEventsCopyItems
        }

    val eventList: Flow<List<EventCopyItem>?> = requestedCopyItems.combine(searchQuery) { allItems, query ->
            if (query.isNullOrEmpty()) allItems
            else allItems
                .filterIsInstance<EventCopyItem.EventItem>()
                .filter { item -> item.name.contains(query, true) }
        }

    private fun buildCopyItemsList(editedEvents: List<Event>, allOtherEvents: List<Event>): List<EventCopyItem> =
        buildList {
            // First, add the actions from the current scenario
            if (editedEvents.isNotEmpty()) {
                add(EventCopyItem.Header(R.string.list_header_copy_event_this))
                addAll(editedEvents
                    .toCopyItems()
                    .sortedBy { it.name }
                )
            }

            if (allOtherEvents.isNotEmpty()) {
                add(EventCopyItem.Header(R.string.list_header_copy_event_all))
                addAll(allOtherEvents
                    .toCopyItems()
                    .sortedBy { it.name }
                )
            }
        }

    fun setCopyListType(triggerEvents: Boolean) {
        viewModelScope.launch {
            requestTriggerEvents.emit(triggerEvents)
        }
    }

    fun updateSearchQuery(query: String?) {
        viewModelScope.launch {
            searchQuery.emit(query)
        }
    }

    fun eventCopyShouldWarnUser(event: Event): Boolean =
        !event.isFromEditedScenario() && event.actions.find { action ->
            action is Action.ToggleEvent && !action.toggleAll
        } != null

    private fun Event.isFromEditedScenario(): Boolean =
        editionRepository.editionState.getScenario()?.id == scenarioId

    private fun List<Event>.toCopyItems(): List<EventCopyItem.EventItem> = map { event ->
        when (event) {
            is ImageEvent -> EventCopyItem.EventItem.Image(
                name = event.name,
                event = event,
                actionsIcons = event.actions.map { it.getIconRes() },
            )

            is TriggerEvent -> EventCopyItem.EventItem.Trigger(
                name = event.name,
                event = event,
            )
        }
    }

    /** Types of items in the event copy list. */
    sealed class EventCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class Header(
            @StringRes val title: Int,
        ) : EventCopyItem()

        sealed class EventItem : EventCopyItem() {

            abstract val name: String
            abstract val event: Event

            data class Image (
                override val name: String,
                override val event: ImageEvent,
                val actionsIcons: List<Int>,
            ) : EventItem()

            data class Trigger (
                override val name: String,
                override val event: TriggerEvent,
            ) : EventItem()
        }

    }
}
