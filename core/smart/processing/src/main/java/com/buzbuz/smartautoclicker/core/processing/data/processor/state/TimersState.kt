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

    fun getTimerEndMs(conditionId: Long): Long?
    fun setTimerStartToNow(condition: TriggerCondition.OnTimerReached)

    fun setTimerToDisabled(conditionId: Long)
}

internal class TimersState(
    private val triggerEvents: List<TriggerEvent>,
): ITimersState {

    private val idToTimerEnd: MutableMap<Long, Long> = mutableMapOf()

    fun onProcessingStarted() {
        val startTimeMs = System.currentTimeMillis()

        triggerEvents.forEach { triggerEvent ->
            if (!triggerEvent.enabledOnStart) return@forEach

            triggerEvent.conditions.forEach { triggerCondition ->
                if (triggerCondition is TriggerCondition.OnTimerReached) {
                    idToTimerEnd[triggerCondition.getValidId()] = triggerCondition.getEndTimeMs(startTimeMs)
                }
            }
        }
    }

    fun onProcessingStopped() {
        idToTimerEnd.clear()
    }

    override fun getTimerEndMs(conditionId: Long): Long? =
        idToTimerEnd[conditionId]

    override fun setTimerStartToNow(condition: TriggerCondition.OnTimerReached) {
        idToTimerEnd[condition.getValidId()] = condition.getEndTimeMs(System.currentTimeMillis())
    }

    override fun setTimerToDisabled(conditionId: Long) {
        idToTimerEnd.remove(conditionId)
    }

    private fun TriggerCondition.OnTimerReached.getEndTimeMs(startTimeMs: Long): Long =
        if (Long.MAX_VALUE - durationMs < startTimeMs) Long.MAX_VALUE
        else startTimeMs + durationMs
}