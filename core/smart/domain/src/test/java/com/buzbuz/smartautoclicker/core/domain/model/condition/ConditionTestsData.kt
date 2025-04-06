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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.event.EventTestsData
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

internal object ConditionTestsData {

    const val CONDITION_ID = 25L
    const val CONDITION_EVENT_ID = EventTestsData.EVENT_ID
    const val CONDITION_NAME = "Condition name"

    const val CONDITION_PATH = "/toto/tutu/tata"
    const val CONDITION_LEFT = 0
    const val CONDITION_TOP = 45
    const val CONDITION_RIGHT = 69
    const val CONDITION_BOTTOM = 89
    const val CONDITION_THRESHOLD = 25
    const val CONDITION_DETECTION_TYPE = EXACT

    const val CONDITION_BROADCAST_ACTION = "com.buzbuz.broadcast"

    const val CONDITION_COUNTER_NAME = "tototutu"
    val CONDITION_COUNTER_OPERATION = ComparisonOperation.GREATER_OR_EQUALS
    const val CONDITION_COUNTER_VALUE = 10

    const val CONDITION_TIMER_MS = 500L

    fun getNewImageConditionEntity(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        path: String = CONDITION_PATH,
        priority: Int = 0,
        area: Rect = Rect(CONDITION_LEFT, CONDITION_TOP, CONDITION_RIGHT, CONDITION_BOTTOM),
        detectionArea: Rect? = null,
        threshold: Int = CONDITION_THRESHOLD,
        detectionType: Int = CONDITION_DETECTION_TYPE,
        shouldBeDetected: Boolean = true,
        eventId: Long
    ) = ConditionEntity(id, eventId, name, ConditionType.ON_IMAGE_DETECTED, priority, detectionType, shouldBeDetected,
        detectionArea?.left, detectionArea?.top, detectionArea?.right, detectionArea?.bottom, threshold, path,
        area.left, area.top, area.right, area.bottom)

    fun getNewImageCondition(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        priority: Int = 0,
        path: String = CONDITION_PATH,
        area: Rect = Rect(CONDITION_LEFT, CONDITION_TOP, CONDITION_RIGHT, CONDITION_BOTTOM),
        detectionArea: Rect? = null,
        threshold: Int = CONDITION_THRESHOLD,
        detectionType: Int = CONDITION_DETECTION_TYPE,
        shouldBeDetected: Boolean = true,
        eventId: Long
    ) = ImageCondition(id.asIdentifier(), eventId.asIdentifier(), name, priority, threshold, shouldBeDetected, detectionType, detectionArea, path, area)

    fun getNewBroadcastReceivedConditionEntity(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        broadcastAction: String = CONDITION_BROADCAST_ACTION,
        eventId: Long
    ) = ConditionEntity(id, eventId, name, ConditionType.ON_BROADCAST_RECEIVED, 0, broadcastAction = broadcastAction)

    fun getNewBroadcastReceivedCondition(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        broadcastAction: String = CONDITION_BROADCAST_ACTION,
        eventId: Long
    ) = TriggerCondition.OnBroadcastReceived(id.asIdentifier(), eventId.asIdentifier(), name, broadcastAction)

    fun getNewCounterReachedConditionEntity(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        counterName: String = CONDITION_COUNTER_NAME,
        counterOperator: ComparisonOperation = CONDITION_COUNTER_OPERATION,
        counterValue: Int = CONDITION_COUNTER_VALUE,
        eventId: Long
    ) = ConditionEntity(id, eventId, name, ConditionType.ON_COUNTER_REACHED, counterName = counterName,
        counterComparisonOperation = counterOperator.toEntity(), priority = 0, counterValue = counterValue,
        counterOperationValueType = CounterOperationValueType.NUMBER,
    )

    fun getNewCounterReachedCondition(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        counterName: String = CONDITION_COUNTER_NAME,
        counterOperator: ComparisonOperation = CONDITION_COUNTER_OPERATION,
        counterValue: Int = CONDITION_COUNTER_VALUE,
        eventId: Long
    ) = TriggerCondition.OnCounterCountReached(id.asIdentifier(), eventId.asIdentifier(), name, counterName,
        counterOperator, CounterOperationValue.Number(counterValue))

    fun getNewTimerReachedConditionEntity(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        timerValueMs: Long = CONDITION_TIMER_MS,
        restartWhenReached: Boolean = true,
        eventId: Long
    ) = ConditionEntity(id, eventId, name, ConditionType.ON_TIMER_REACHED, 0, timerValueMs = timerValueMs, restartWhenReached = restartWhenReached)

    fun getNewTimerReachedCondition(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        timerValueMs: Long = CONDITION_TIMER_MS,
        restartWhenReached: Boolean = true,
        eventId: Long
    ) = TriggerCondition.OnTimerReached(id.asIdentifier(), eventId.asIdentifier(), name, timerValueMs, restartWhenReached)
}