
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent


interface ICountersState {
    fun getCounterValue(counterName: String): Int?
    fun setCounterValue(counterName: String, value: Int)
}

internal class CountersState(
    imageEvents: List<ImageEvent>,
    triggerEvent: List<TriggerEvent>,
) : ICountersState {

    /** Parse the whole event list to get all the counters names. */
    private val counterMap: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
        (imageEvents + triggerEvent).forEach { event ->
            event.conditions.forEach { condition ->
                if (condition is TriggerCondition.OnCounterCountReached) put(condition.counterName, 0)
            }

            event.actions.forEach { action ->
                if (action is ChangeCounter) put(action.counterName, 0)
                if (action is Notification && action.messageType == Notification.MessageType.COUNTER_VALUE)
                    put(action.messageCounterName, 0)
            }
        }
    }

    override fun getCounterValue(counterName: String): Int? =
        counterMap[counterName]

    override fun setCounterValue(counterName: String, value: Int) {
        if (!counterMap.containsKey(counterName)) return
        counterMap[counterName] = value
    }
}