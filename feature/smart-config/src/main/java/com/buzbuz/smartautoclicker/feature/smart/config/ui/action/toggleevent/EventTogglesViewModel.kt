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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent

import android.content.Context
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class EventTogglesViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /**
     * Contains the changes for the events toggle made by the user.
     * Initialized with the values from the edition repository.
     */
    private val userModifications: MutableStateFlow<Map<Identifier, Pair<Identifier?, ToggleEvent.ToggleType?>>> = MutableStateFlow(
        buildMap {
            val currentEditedEvent = editionRepository.editionState.getEditedEvent<Event>() ?: return@buildMap
            val allEditedEvents = editionRepository.editionState.getAllEditedEvents()
            val toggles = editionRepository.editionState.getEditedActionEventToggles() ?: emptyList()

            findAndPutToggleState(currentEditedEvent.id, toggles)
            allEditedEvents.forEach { event -> findAndPutToggleState(event.id, toggles) }
        }
    )

    /** Final items list, with all events and the user modifications applied. */
    val currentItems: Flow<List<EventTogglesListItem>> =
        combine(editionRepository.editionState.allEditedEvents, userModifications) { editedEvents, modifications ->
            buildList {
                val imageEvents = mutableListOf<EventTogglesListItem>().apply {
                    add(EventTogglesListItem.Header(context.getString(R.string.list_header_image_events)))
                }
                val triggerEvents = mutableListOf<EventTogglesListItem>().apply {
                    add(EventTogglesListItem.Header(context.getString(R.string.list_header_trigger_events)))
                }

                editedEvents
                    .sortedBy { event ->
                        when (event) {
                            is ScreenEvent -> event.priority
                            is TriggerEvent -> -1
                        }
                    }
                    .forEach { event ->
                        val item = event.toEventTogglesListItems(
                            toggleState = modifications[event.id]?.second,
                        )

                        when (event) {
                            is ScreenEvent -> imageEvents.add(item)
                            is TriggerEvent -> triggerEvents.add(item)
                        }
                    }

                when (editionRepository.editionState.getEditedEvent<Event>()) {
                    is ScreenEvent -> {
                        if (imageEvents.size > 1) addAll(imageEvents)
                        if (triggerEvents.size > 1) addAll(triggerEvents)
                    }

                    is TriggerEvent -> {
                        if (triggerEvents.size > 1) addAll(triggerEvents)
                        if (imageEvents.size > 1) addAll(imageEvents)
                    }

                    null -> Unit
                }
            }
        }

    fun changeEventToggleState(eventId: Identifier, newState: ToggleEvent.ToggleType?) {
        userModifications.value = userModifications.value.toMutableMap().apply {
            get(eventId)?.let { (toggleId, _) ->
                put(eventId, toggleId to newState)
            }
        }
    }

    fun getEditedEventToggleList(): List<EventToggle> =
        userModifications.value.mapNotNull { (eventId, eventToggleIdToNewType) ->
            val (editedToggleId, newType) = eventToggleIdToNewType
            if (newType == null) return@mapNotNull null

            if (editedToggleId != null) {
                editionRepository.editedItemsBuilder.createNewEventToggle(
                    id = editedToggleId,
                    targetEventId = eventId,
                    toggleType = newType,
                )
            } else {
                editionRepository.editedItemsBuilder.createNewEventToggle(
                    targetEventId = eventId,
                    toggleType = newType,
                )
            }
        }

    private fun Event.toEventTogglesListItems(toggleState: ToggleEvent.ToggleType?) =
        EventTogglesListItem.Item(
            eventId = id,
            eventName = name,
            actionsCount = actions.size,
            conditionsCount = conditions.size,
            toggleState = toggleState,
        )

    private fun MutableMap<Identifier, Pair<Identifier?, ToggleEvent.ToggleType?>>.findAndPutToggleState(
        eventId: Identifier,
        toggles: List<EventToggle>,
    ) {
        val eventToggle = toggles.find { eventToggle -> eventToggle.targetEventId == eventId }
        put(eventId, eventToggle?.id to eventToggle?.toggleType)
    }
}

sealed class EventTogglesListItem {

    data class Header(
        val title: String,
    ) : EventTogglesListItem()

    data class Item(
        val eventId: Identifier,
        val eventName: String,
        val actionsCount: Int,
        val conditionsCount: Int,
        val toggleState: ToggleEvent.ToggleType?,
    ) : EventTogglesListItem()
}