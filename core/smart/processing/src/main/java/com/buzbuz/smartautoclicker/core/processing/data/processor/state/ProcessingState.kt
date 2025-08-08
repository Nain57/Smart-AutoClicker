
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import android.content.Context
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

internal class ProcessingState(
    imageEvents: List<ImageEvent>,
    triggerEvents: List<TriggerEvent>,
    private val eventsState: EventsState = EventsState(imageEvents, triggerEvents),
    private val broadcastsState: BroadcastsState = BroadcastsState(triggerEvents),
    private val countersState: CountersState = CountersState(imageEvents, triggerEvents),
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