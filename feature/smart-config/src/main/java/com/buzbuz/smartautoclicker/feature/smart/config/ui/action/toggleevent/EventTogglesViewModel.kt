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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class EventTogglesViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val userModifications: MutableStateFlow<Map<Event, Pair<Identifier?, ToggleEvent.ToggleType?>>> =
        MutableStateFlow(emptyMap())

    /** Final items list, with all events and the user modifications applied. */
    val currentItems: Flow<List<EventTogglesListItem>> =
        userModifications.map { modifications ->
            buildList {
                val imageEvents = mutableListOf<EventTogglesListItem>().apply {
                    add(EventTogglesListItem.Header(context.getString(R.string.list_header_image_events)))
                }
                val triggerEvents = mutableListOf<EventTogglesListItem>().apply {
                    add(EventTogglesListItem.Header(context.getString(R.string.list_header_trigger_events)))
                }

                modifications.keys
                    .sortedBy { event ->
                        when (event) {
                            is ScreenEvent -> event.priority
                            is TriggerEvent -> -1
                        }
                    }
                    .forEach { event ->
                        val item = event.toEventTogglesListItems(
                            toggleState = modifications[event]?.second,
                        )

                        when (event) {
                            is ScreenEvent -> imageEvents.add(item)
                            is TriggerEvent -> triggerEvents.add(item)
                        }
                    }

                when (editionRepository.editionState.getEditedEvent()) {
                    is TriggerEvent -> {
                        if (triggerEvents.size > 1) addAll(triggerEvents)
                        if (imageEvents.size > 1) addAll(imageEvents)
                    }

                    else -> {
                        if (imageEvents.size > 1) addAll(imageEvents)
                        if (triggerEvents.size > 1) addAll(triggerEvents)
                    }
                }
            }
        }

    fun setDialogArgs(toggleEventAction: ToggleEvent, events: List<Event>) {
        userModifications.update {
            buildMap {
                events.forEach { event ->
                    findAndPutToggleState(event, toggleEventAction.eventToggles)
                }
            }
        }
    }

    fun changeEventToggleState(event: Event, newState: ToggleEvent.ToggleType?) {
        userModifications.value = userModifications.value.toMutableMap().apply {
            get(event)?.let { (toggleId, _) ->
                put(event, toggleId to newState)
            }
        }
    }

    fun getEditedEventToggleList(): List<EventToggle> =
        userModifications.value.mapNotNull { (event, eventToggleIdToNewType) ->
            val (editedToggleId, newType) = eventToggleIdToNewType
            if (newType == null) return@mapNotNull null

            if (editedToggleId != null) {
                editionRepository.editedItemsBuilder.createNewEventToggle(
                    id = editedToggleId,
                    targetEventId = event.id,
                    toggleType = newType,
                )
            } else {
                editionRepository.editedItemsBuilder.createNewEventToggle(
                    targetEventId = event.id,
                    toggleType = newType,
                )
            }
        }

    private fun Event.toEventTogglesListItems(toggleState: ToggleEvent.ToggleType?) =
        EventTogglesListItem.Item(
            event = this,
            actionsCount = actions.size,
            conditionsCount = conditions.size,
            toggleState = toggleState,
        )

    private fun MutableMap<Event, Pair<Identifier?, ToggleEvent.ToggleType?>>.findAndPutToggleState(
        event: Event,
        toggles: List<EventToggle>,
    ) {
        val eventToggle = toggles.find { eventToggle -> eventToggle.targetEventId == event.id }
        put(event, eventToggle?.id to eventToggle?.toggleType)
    }
}

sealed class EventTogglesListItem {

    data class Header(
        val title: String,
    ) : EventTogglesListItem()

    data class Item(
        val event: Event,
        val actionsCount: Int,
        val conditionsCount: Int,
        val toggleState: ToggleEvent.ToggleType?,
    ) : EventTogglesListItem()
}