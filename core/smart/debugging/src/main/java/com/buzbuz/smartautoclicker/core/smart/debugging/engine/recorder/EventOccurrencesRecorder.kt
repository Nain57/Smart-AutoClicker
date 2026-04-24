/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import javax.inject.Inject

internal class EventOccurrencesRecorder @Inject constructor() {

    private val eventDurationRecorder: DurationRecorder = DurationRecorder()
    private val eventOccurrences: MutableMap<Long, Int> = mutableMapOf()
    private var lastEventDurationMs: Long = 0

    fun onEventProcessingStarted() {
        eventDurationRecorder.start()
    }

    fun onEventFulfilled(event: Event) {
        eventOccurrences[event.id.databaseId] = (eventOccurrences[event.id.databaseId] ?: 0) + 1
        lastEventDurationMs = eventDurationRecorder.durationMs()
    }

    fun reset() {
        lastEventDurationMs = 0L
        eventOccurrences.clear()
        eventDurationRecorder.reset()
    }

    fun getEventOccurrences(id: Long): Int = eventOccurrences[id] ?: 0
    fun getLastEventDurationMs(): Long = lastEventDurationMs
}