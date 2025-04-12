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
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

internal class ProcessingState(
    screenEvents: List<ScreenEvent>,
    triggerEvents: List<TriggerEvent>,
    private val eventsState: EventsState = EventsState(screenEvents, triggerEvents),
    private val broadcastsState: BroadcastsState = BroadcastsState(triggerEvents),
    private val countersState: CountersState = CountersState(screenEvents, triggerEvents),
    private val timersState: TimersState = TimersState(triggerEvents),
) : IBroadcastsState by broadcastsState, ICountersState by countersState, ITimersState by timersState, IEventsState by eventsState {

    init {
        eventsState.setEventStateListener(object : EventStateListener {
            override fun onEventEnabled(event: Event): Unit = this@ProcessingState.onEventEnabled(event)
            override fun onEventDisabled(event: Event): Unit = this@ProcessingState.onEventDisabled(event)
        })
    }

    fun onProcessingStarted(context: Context) {
        broadcastsState.onProcessingStarted(context)
        timersState.onProcessingStarted()
    }

    fun onProcessingStopped() {
        broadcastsState.onProcessingStopped()
        timersState.onProcessingStopped()
    }

    fun clearIterationState() {
        broadcastsState.clearReceivedBroadcast()
    }

    private fun onEventEnabled(event: Event) {
        event.conditions.forEach { condition ->
            if (condition is TriggerCondition.OnTimerReached) timersState.setTimerStartToNow(condition)
        }
    }

    private fun onEventDisabled(event: Event) {
        event.conditions.forEach { condition ->
            if (condition is TriggerCondition.OnTimerReached) timersState.setTimerToDisabled(condition.getValidId())
        }
    }
}