
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