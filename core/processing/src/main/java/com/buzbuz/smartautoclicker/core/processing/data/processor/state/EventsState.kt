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

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

interface IEventsState {

    val startEvent: TriggerEvent?
    val endEvent: TriggerEvent?

    fun isEventEnabled(event: Event): Boolean
    fun areAllEventsDisabled(): Boolean
    fun areAllImageEventsDisabled(): Boolean
    fun areAllTriggerEventsDisabled(): Boolean

    fun getEnabledImageEvents(): Collection<ImageEvent>
    fun getEnabledTriggerEvents(): Collection<TriggerEvent>

    fun enableAll()
    fun enableEvent(eventId: Long)
    fun disableAll()
    fun disableEvent(eventId: Long)
    fun toggleAll()
    fun toggleEvent(eventId: Long)
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
    imageEvents: List<ImageEvent>,
    triggerEvents: List<TriggerEvent>,
) : IEventsState {

    /** Monitor the state of all image events. */
    private val imageEventList: EventList<ImageEvent> = EventList(imageEvents)
    /** Monitor the state of all trigger events. */
    private val triggerEventList: EventList<TriggerEvent> = EventList(triggerEvents)

    override val startEvent: TriggerEvent? =
        triggerEvents.find { event -> event.conditions.find { it is TriggerCondition.OnScenarioStart } != null }
    override val endEvent: TriggerEvent? =
        triggerEvents.find { event -> event.conditions.find { it is TriggerCondition.OnScenarioEnd } != null }

    override fun isEventEnabled(event: Event): Boolean =
        triggerEventList.isEventEnabled(event) || imageEventList.isEventEnabled(event)

    override fun areAllEventsDisabled(): Boolean =
        imageEventList.areAllEventsDisabled() && triggerEventList.areAllEventsDisabled()

    override fun areAllImageEventsDisabled(): Boolean =
        imageEventList.areAllEventsDisabled()

    override fun getEnabledImageEvents(): Collection<ImageEvent> =
        imageEventList.getEnabledEvents()

    override fun areAllTriggerEventsDisabled(): Boolean =
        triggerEventList.areAllEventsDisabled()

    override fun getEnabledTriggerEvents(): Collection<TriggerEvent> =
        triggerEventList.getEnabledEvents()

    override fun enableEvent(eventId: Long) {
        imageEventList.enableEvent(eventId)
        triggerEventList.enableEvent(eventId)
    }

    override fun disableEvent(eventId: Long) {
        imageEventList.disableEvent(eventId)
        triggerEventList.disableEvent(eventId)
    }

    override fun toggleEvent(eventId: Long) {
        imageEventList.toggleEvent(eventId)
        triggerEventList.toggleEvent(eventId)
    }

    override fun enableAll() {
        imageEventList.enableAll()
        triggerEventList.enableAll()
    }

    override fun disableAll() {
        imageEventList.disableAll()
        triggerEventList.disableAll()
    }

    override fun toggleAll() {
        imageEventList.toggleAll()
        triggerEventList.toggleAll()
    }
}

private class EventList<T : Event>(events: List<T>) {

    /** Set of enabled events ids. */
    private val enabledEventsMap: MutableMap<Long, T> = mutableMapOf()
    /** Map of the all events. */
    private val eventsMap: Map<Long, T> = buildMap {
        events.forEach { event ->
            if (event.enabledOnStart) enabledEventsMap[event.getDatabaseId()] = event
            put(event.getDatabaseId(), event)
        }
    }

    fun isEventEnabled(event: Event): Boolean =
        enabledEventsMap.containsKey(event.getDatabaseId())

    fun areAllEventsDisabled(): Boolean =
        enabledEventsMap.isEmpty()

    fun getEnabledEvents(): Collection<T> =
        enabledEventsMap.values

    fun enableEvent(eventId: Long) {
        val event = eventsMap[eventId] ?: return
        enabledEventsMap[eventId] = event
    }

    fun disableEvent(eventId: Long) {
        if (!eventsMap.containsKey(eventId)) return
        enabledEventsMap.remove(eventId)
    }

    fun toggleEvent(eventId: Long) {
        val event = eventsMap[eventId] ?: return

        if (enabledEventsMap.containsKey(eventId)) enabledEventsMap.remove(eventId)
        else enabledEventsMap[eventId] = event
    }

    fun enableAll() {
        enabledEventsMap.putAll(eventsMap)
    }

    fun disableAll() {
        enabledEventsMap.clear()
    }

    fun toggleAll() {
        eventsMap.keys.forEach(::toggleEvent)
    }
}
