/*
 * Copyright (C) 2025 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.domain.ext.getAllCounterNames
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportActionResult

internal class CounterValuesRecorder {

    private val _counterNames: MutableSet<String> = mutableSetOf()
    val counterNames: Set<String> = _counterNames

    private val _eventCounterChanges: MutableList<DebugReportActionResult.CounterChange> = mutableListOf()
    val eventCounterChanges: List<DebugReportActionResult.CounterChange> = _eventCounterChanges


    fun onSessionStarted(imageEvents: List<ImageEvent>, triggerEvents: List<TriggerEvent>) {
        reset()
        _counterNames.addAll((imageEvents + triggerEvents).getAllCounterNames())
    }

    fun onEventProcessingStarted() {
        _eventCounterChanges.clear()
    }

    fun onCounterValueChanged(counterName: String, previousValue: Int, newValue: Int) {
        _eventCounterChanges.add(
            DebugReportActionResult.CounterChange(
                counterName = counterName,
                previousValue = previousValue,
                newValue = newValue,
            )
        )
    }

    fun reset() {
        _counterNames.clear()
        _eventCounterChanges.clear()
    }
}