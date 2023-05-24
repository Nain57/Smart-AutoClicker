/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data

import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

/**
 * Handle the state of the scenario events.
 *
 * Maintains two maps:
 *  - the enabled events map: those are the events that will be processed.
 *  - the disabled events map: those are the events that will be skipped.
 * Handles the ToggleEvent actions and move the events between those maps accordingly.
 */
internal class ScenarioState(events: List<Event>) : ScenarioEditor {

    /** Map of the enabled events. */
    private val enabledEventsMap: MutableMap<Long, Event> = mutableMapOf()
    /** Map of the disabled events. */
    private val disabledEvents: MutableMap<Long, Event> = mutableMapOf()

    init {
        events.forEach { event ->
            if (event.enabledOnStart) enabledEventsMap[event.id.databaseId] = event
            else disabledEvents[event.id.databaseId] = event
        }
    }

    /** Get the list of currently enabled events. */
    fun getEnabledEvents(): Collection<Event> = enabledEventsMap.values

    /** Tells if all events are disabled. */
    fun areAllEventsDisabled(): Boolean = enabledEventsMap.isEmpty()

    override fun changeEventState(eventId: Long, toggleType: Action.ToggleEvent.ToggleType) {
        when (toggleType) {
            Action.ToggleEvent.ToggleType.ENABLE -> enableEvent(eventId)
            Action.ToggleEvent.ToggleType.DISABLE -> disableEvent(eventId)
            Action.ToggleEvent.ToggleType.TOGGLE -> {
                when {
                    enabledEventsMap.containsKey(eventId) -> disableEvent(eventId)
                    disabledEvents.containsKey(eventId) -> enableEvent(eventId)
                    else -> Log.w(TAG, "Trying to change the state of an unknown event.")
                }
            }
        }
    }

    /** Move an event from the disabled map to the enabled one. */
    private fun enableEvent(eventId: Long) {
        disabledEvents.remove(eventId)?.let { event -> enabledEventsMap[eventId] = event }
    }

    /** Move an event from the enabled map to the disabled one. */
    private fun disableEvent(eventId: Long) {
        enabledEventsMap.remove(eventId)?.let { event -> disabledEvents[eventId] = event }
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioState"