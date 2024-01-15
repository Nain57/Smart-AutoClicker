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

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

internal class ProcessingState(
    imageEvents: List<ImageEvent>,
    triggerEvents: List<TriggerEvent>,
    private val broadcastsState: BroadcastsState = BroadcastsState(triggerEvents),
    private val countersState: CountersState = CountersState(imageEvents, triggerEvents),
    private val eventsState: EventsState = EventsState(imageEvents, triggerEvents),
) : IBroadcastsState by broadcastsState, ICountersState by countersState, IEventsState by eventsState {

    var processingStartTsMs: Long = -1
        private set

    fun onProcessingStarted(context: Context) {
        processingStartTsMs = System.currentTimeMillis()
        broadcastsState.onProcessingStarted(context)
    }

    fun onProcessingStopped() {
        broadcastsState.onProcessingStopped()
        processingStartTsMs = -1
    }
}