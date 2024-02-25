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

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
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

    /**
     * Parse the whole event list to get all the counters name.
     * If a condition listen to a counter but nothing (or the opposite), we skip it to avoid unnecessary processing.
     */
    private val counterMap: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
        val allEvents = imageEvents + triggerEvent
        val conditionCounterSet = mutableSetOf<String>()

        allEvents.forEach { event ->
            event.conditions.forEach { condition ->
                if (condition is TriggerCondition.OnCounterCountReached) {
                    conditionCounterSet.add(condition.counterName)
                }
            }
        }

        allEvents.forEach { event ->
            event.actions.forEach { action ->
                if (action is Action.ChangeCounter) {
                    if (conditionCounterSet.remove(action.counterName)) put(action.counterName, 0)
                }
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