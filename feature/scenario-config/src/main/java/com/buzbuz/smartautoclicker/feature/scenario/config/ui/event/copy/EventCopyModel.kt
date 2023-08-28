/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getIconRes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [EventCopyDialog].
 *
 * @param application the Android application.
 */
class EventCopyModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /**
     * List of displayed event items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val eventList: Flow<List<EventCopyItem>?> =
        combine(repository.getAllEventsFlow(), editionRepository.editionState.eventsState, searchQuery) { dbEvents, editedEvents, query ->
            if (query.isNullOrEmpty()) getAllItems(dbEvents, editedEvents.value)
            else getSearchedItems(dbEvents, query)
        }

    /**
     * Get all items with the headers.
     * @param dbEvents all actions in the database.
     * @param scenarioEvents all actions in the current event.
     * @return the complete list of action items.
     */
    private fun getAllItems(dbEvents: List<Event>, scenarioEvents: List<Event>?): List<EventCopyItem> {
        val allItems = mutableListOf<EventCopyItem>()

        // First, add the events from the current scenario
        val eventItems = scenarioEvents?.sortedBy { it.name }?.map { it.toEventItem() }?.distinct()
            ?: emptyList()
        if (eventItems.isNotEmpty()) allItems.add(EventCopyItem.HeaderItem(R.string.list_header_copy_event_this))
        allItems.addAll(eventItems)

        // Then, add all other events. Remove the one already in this scenario.
        val events = dbEvents
            .map { it.toEventItem() }
            .toMutableList()
            .apply {
                removeIf { allItem ->
                    eventItems.find { allItem.event.id == it.event.id || allItem == it } != null
                }
            }
            .distinct()
        if (events.isNotEmpty()) allItems.add(EventCopyItem.HeaderItem(R.string.list_header_copy_event_all))
        allItems.addAll(events)

        return allItems
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Get the result of the search query.
     * @param dbEvents all actions in the database.
     * @param query the current search query.
     */
    private fun getSearchedItems(dbEvents: List<Event>, query: String): List<EventCopyItem> = dbEvents
        .filter { event ->
            event.name.contains(query, true)
        }
        .map { it.toEventItem() }
        .distinct()

    /** If that's not the same scenario and we are copying an event with a toggle event action, warn the user */
    fun eventCopyShouldWarnUser(event: Event): Boolean =
        !event.isFromEditedScenario() && event.actions.firstOrNull { it is Action.ToggleEvent } != null

    private fun Event.isFromEditedScenario(): Boolean =
        editionRepository.editionState.getScenario()?.id == scenarioId

    /** @return the [EventCopyItem.EventItem] corresponding to this event. */
    private fun Event.toEventItem(): EventCopyItem.EventItem =
        EventCopyItem.EventItem(
            name = name,
            actionsIcons = actions.map { it.getIconRes() },
            event = this,
        )

    /** Types of items in the event copy list. */
    sealed class EventCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(
            @StringRes val title: Int,
        ) : EventCopyItem()

        /**
         * Event item.
         * @param name the name of the event.
         * @param actionsIcons the icon resources for the actions of the event.
         * @param event event represented by this item.
         */
        data class EventItem (
            val name: String,
            val actionsIcons: List<Int>,
            val event: Event,
        ) : EventCopyItem()
    }
}
