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
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import com.buzbuz.smartautoclicker.core.base.interfaces.sortedByPriority
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

interface IEventsState {

    fun isEventEnabled(eventId: Long): Boolean
    fun areAllEventsDisabled(): Boolean
    fun areAllImageEventsDisabled(): Boolean
    fun areAllTriggerEventsDisabled(): Boolean

    fun getEnabledImageEvents(): Collection<ScreenEvent>
    fun getEnabledTriggerEvents(): Collection<TriggerEvent>

    fun enableAll()
    fun enableEvent(eventId: Long)
    fun disableAll()
    fun disableEvent(eventId: Long)
    fun toggleAll()
    fun toggleEvent(eventId: Long)
    fun setEventStateListener(listener: EventStateListener)
}

interface EventStateListener {
    fun onEventEnabled(event: Event)
    fun onEventDisabled(event: Event)
}

/**
 * Handle the state of the scenario events.
 *
 * Maintains two maps:
 *  - the enabled events map: those are the events that will be processed.
 *  - the disabled events map: those are the events that will be skipped.
 * Handles the ToggleEvent actions and move the events between those maps accordingly.
 */
internal class EventsState(
    screenEvents: List<ScreenEvent>,
    triggerEvents: List<TriggerEvent>,
) : IEventsState {

    /** Monitor the state of all image events. */
    private val screenEventList: EventList<ScreenEvent> = EventList(screenEvents)
    /** Monitor the state of all trigger events. */
    private val triggerEventList: EventList<TriggerEvent> = EventList(triggerEvents)

    override fun setEventStateListener(listener: EventStateListener) {
        triggerEventList.eventEnabledListener = listener
        screenEventList.eventEnabledListener = listener
    }

    override fun isEventEnabled(eventId: Long): Boolean =
        triggerEventList.isEventEnabled(eventId) || screenEventList.isEventEnabled(eventId)

    override fun areAllEventsDisabled(): Boolean =
        screenEventList.areAllEventsDisabled() && triggerEventList.areAllEventsDisabled()

    override fun areAllImageEventsDisabled(): Boolean =
        screenEventList.areAllEventsDisabled()

    override fun getEnabledImageEvents(): Collection<ScreenEvent> =
        screenEventList.getEnabledEvents().sortedByPriority()

    override fun areAllTriggerEventsDisabled(): Boolean =
        triggerEventList.areAllEventsDisabled()

    override fun getEnabledTriggerEvents(): Collection<TriggerEvent> =
        triggerEventList.getEnabledEvents().toList()

    override fun enableEvent(eventId: Long) {
        screenEventList.enableEvent(eventId)
        triggerEventList.enableEvent(eventId)
    }

    override fun disableEvent(eventId: Long) {
        screenEventList.disableEvent(eventId)
        triggerEventList.disableEvent(eventId)
    }

    override fun toggleEvent(eventId: Long) {
        screenEventList.toggleEvent(eventId)
        triggerEventList.toggleEvent(eventId)
    }

    override fun enableAll() {
        screenEventList.enableAll()
        triggerEventList.enableAll()
    }

    override fun disableAll() {
        screenEventList.disableAll()
        triggerEventList.disableAll()
    }

    override fun toggleAll() {
        screenEventList.toggleAll()
        triggerEventList.toggleAll()
    }
}

private class EventList<T : Event>(events: List<T>) {

    /** Set of enabled events ids. */
    private val enabledEventsMap: MutableMap<Long, T> = mutableMapOf()
    /** Map of the all events. */
    private val eventsMap: Map<Long, T> = buildMap {
        events.forEach { event ->
            if (event.enabledOnStart) enabledEventsMap[event.getValidId()] = event
            put(event.getValidId(), event)
        }
    }

    var eventEnabledListener: EventStateListener? = null

    fun isEventEnabled(eventDbId: Long): Boolean =
        enabledEventsMap.containsKey(eventDbId)

    fun areAllEventsDisabled(): Boolean =
        enabledEventsMap.isEmpty()

    fun getEnabledEvents(): Collection<T> =
        enabledEventsMap.values

    fun enableEvent(eventId: Long) {
        if (enabledEventsMap.containsKey(eventId)) return
        val event = eventsMap[eventId] ?: return

        enabledEventsMap[eventId] = event
        eventEnabledListener?.onEventEnabled(event)
    }

    fun disableEvent(eventId: Long) {
        if (!enabledEventsMap.containsKey(eventId)) return
        val event = eventsMap[eventId] ?: return

        enabledEventsMap.remove(eventId)
        eventEnabledListener?.onEventDisabled(event)
    }

    fun toggleEvent(eventId: Long) {
        if (enabledEventsMap.containsKey(eventId)) disableEvent(eventId)
        else enableEvent(eventId)
    }

    fun enableAll() {
        eventsMap.keys.forEach(::enableEvent)
    }

    fun disableAll() {
        eventsMap.keys.forEach(::disableEvent)
    }

    fun toggleAll() {
        eventsMap.keys.forEach(::toggleEvent)
    }
}
