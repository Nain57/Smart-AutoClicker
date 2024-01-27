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
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

interface ITimersState {

    fun getTimerValue(conditionId: Long): Long?
    fun setTimerToNow(conditionId: Long)

    fun setTimerToDisabled(conditionId: Long)
}

internal class TimersState(
    triggerEvents: List<TriggerEvent>,
): ITimersState {

    private val idToTimerStart: MutableMap<Long, Long> = mutableMapOf<Long, Long>().apply {
        triggerEvents.forEach { triggerEvent ->
            triggerEvent.conditions.forEach { triggerCondition ->
                if (triggerCondition is TriggerCondition.OnTimerReached) {
                    put(triggerCondition.getDatabaseId(), Long.MAX_VALUE)
                }
            }
        }
    }

    fun onProcessingStarted() {
        if (idToTimerStart.isEmpty()) return

        val timerValueMs = System.currentTimeMillis()
        idToTimerStart.keys.forEach { identifier ->
            idToTimerStart[identifier] = timerValueMs
        }
    }

    fun onProcessingStopped() {
        if (idToTimerStart.isEmpty()) return
        idToTimerStart.keys.forEach(::setTimerToDisabled)
    }

    override fun getTimerValue(conditionId: Long): Long? =
        idToTimerStart[conditionId]

    override fun setTimerToNow(conditionId: Long) {
        idToTimerStart[conditionId] = System.currentTimeMillis()
    }

    override fun setTimerToDisabled(conditionId: Long) {
        idToTimerStart[conditionId] = Long.MAX_VALUE
    }
}