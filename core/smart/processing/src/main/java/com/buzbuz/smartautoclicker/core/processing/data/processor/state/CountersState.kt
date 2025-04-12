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

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent


interface ICountersState {
    fun getCounterValue(counterName: String): Int?
    fun setCounterValue(counterName: String, value: Int)
}

internal class CountersState(
    screenEvents: List<ScreenEvent>,
    triggerEvent: List<TriggerEvent>,
) : ICountersState {

    /** Parse the whole event list to get all the counters names. */
    private val counterMap: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
        (screenEvents + triggerEvent).forEach { event ->
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