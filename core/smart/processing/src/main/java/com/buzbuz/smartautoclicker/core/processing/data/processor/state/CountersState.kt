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

import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener


interface ICountersState {
    fun getCounterValue(counterName: String): Double?
    fun setCounterValue(counterName: String, value: Double)
}

internal class CountersState(
    counters: List<Counter>,
    private val listener: SmartProcessingListener?,
) : ICountersState {

    private val counterMap: MutableMap<String, Double> = mutableMapOf<String, Double>().apply {
        counters.forEach { counter ->
            put(counter.counterName, counter.defaultValue)
        }
    }

    override fun getCounterValue(counterName: String): Double? =
        counterMap[counterName]

    override fun setCounterValue(counterName: String, value: Double) {
        if (!counterMap.containsKey(counterName)) return

        // Values are initialized with starting value, previous should never be null
        val previous = counterMap.put(counterName, value) ?: return
        listener?.onCounterValueChanged(counterName, previous, value)
    }
}