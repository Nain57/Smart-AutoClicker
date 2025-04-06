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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage


internal fun Condition.toEntity(): ConditionEntity = when (this) {
    is ImageCondition -> toImageConditionEntity()
    is TextCondition -> toTextConditionEntity()
    is TriggerCondition.OnBroadcastReceived -> toBroadcastReceivedEntity()
    is TriggerCondition.OnCounterCountReached -> toCounterReachedEntity()
    is TriggerCondition.OnTimerReached -> toTimerReachedEntity()
}

/** @return the entity equivalent of this condition. */
private fun ImageCondition.toImageConditionEntity() = ConditionEntity(
    id = id.databaseId,
    eventId = eventId.databaseId,
    name = name,
    priority = priority,
    type = ConditionType.ON_IMAGE_DETECTED,
    path = path,
    areaLeft = captureArea.left,
    areaTop = captureArea.top,
    areaRight = captureArea.right,
    areaBottom = captureArea.bottom,
    threshold = threshold,
    detectionType = detectionType,
    shouldBeDetected = shouldBeDetected,
    detectionAreaLeft = detectionArea?.left,
    detectionAreaTop = detectionArea?.top,
    detectionAreaRight = detectionArea?.right,
    detectionAreaBottom = detectionArea?.bottom,
)

/** @return the entity equivalent of this condition. */
private fun TextCondition.toTextConditionEntity() = ConditionEntity(
    id = id.databaseId,
    eventId = eventId.databaseId,
    name = name,
    priority = priority,
    type = ConditionType.ON_IMAGE_DETECTED,
    threshold = threshold,
    detectionType = detectionType,
    shouldBeDetected = shouldBeDetected,
    detectionAreaLeft = detectionArea?.left,
    detectionAreaTop = detectionArea?.top,
    detectionAreaRight = detectionArea?.right,
    detectionAreaBottom = detectionArea?.bottom,
    textToDetect = textToDetect,
    textLanguage = textLanguage.name,
)

private fun TriggerCondition.OnBroadcastReceived.toBroadcastReceivedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_BROADCAST_RECEIVED,
        broadcastAction = intentAction,
        priority = 0,
    )

private fun TriggerCondition.OnCounterCountReached.toCounterReachedEntity(): ConditionEntity {
    val isNumberValue = counterValue is CounterOperationValue.Number

    return ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_COUNTER_REACHED,
        counterName = counterName,
        counterComparisonOperation = comparisonOperation.toEntity(),
        counterOperationValueType = if (isNumberValue) CounterOperationValueType.NUMBER else CounterOperationValueType.COUNTER,
        counterValue = if (isNumberValue) counterValue.value as Int else null,
        counterOperationCounterName = if (isNumberValue) null else counterValue.value as String,
        priority = 0,
    )
}

private fun TriggerCondition.OnTimerReached.toTimerReachedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_TIMER_REACHED,
        timerValueMs = durationMs,
        restartWhenReached = restartWhenReached,
        priority = 0,
    )


internal fun ConditionEntity.toDomain(cleanIds: Boolean = false): Condition =
    when (type) {
        ConditionType.ON_IMAGE_DETECTED -> toDomainImageCondition(cleanIds)
        ConditionType.ON_BROADCAST_RECEIVED -> toDomainBroadcastReceived(cleanIds)
        ConditionType.ON_COUNTER_REACHED -> toDomainCounterReached(cleanIds)
        ConditionType.ON_TEXT_DETECTED -> toDomainTextCondition(cleanIds)
        ConditionType.ON_TIMER_REACHED -> toDomainTimerReached(cleanIds)
        else -> throw IllegalArgumentException("Unsupported condition type for a TriggerCondition")
    }

private fun ConditionEntity.toDomainImageCondition(cleanIds: Boolean = false): ImageCondition =
    ImageCondition(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        priority = priority,
        path = path!!,
        captureArea = Rect(areaLeft!!, areaTop!!, areaRight!!, areaBottom!!),
        threshold = threshold!!,
        detectionType = detectionType!!,
        detectionArea = getDetectionArea(),
        shouldBeDetected = shouldBeDetected ?: true,
    )

private fun ConditionEntity.toDomainTextCondition(cleanIds: Boolean = false): TextCondition =
    TextCondition(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        priority = priority,
        threshold = threshold!!,
        detectionType = detectionType!!,
        detectionArea = getDetectionArea(),
        shouldBeDetected = shouldBeDetected ?: true,
        textToDetect = textToDetect!!,
        textLanguage = textLanguage.toTextLanguage(),
    )

private fun ConditionEntity.toDomainBroadcastReceived(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnBroadcastReceived(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        intentAction = broadcastAction!!,
    )

private fun ConditionEntity.toDomainCounterReached(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnCounterCountReached(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        counterName = counterName!!,
        comparisonOperation = counterComparisonOperation!!.toDomain(),
        counterValue = CounterOperationValue.getCounterOperationValue(
            type = counterOperationValueType,
            numberValue = counterValue,
            counterName = counterOperationCounterName,
        ),
    )


private fun ConditionEntity.toDomainTimerReached(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnTimerReached(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        durationMs = timerValueMs!!,
        restartWhenReached = restartWhenReached!!,
    )

private fun CounterComparisonOperation.toDomain(): TriggerCondition.OnCounterCountReached.ComparisonOperation =
    TriggerCondition.OnCounterCountReached.ComparisonOperation.valueOf(name)

private fun ConditionEntity.getDetectionArea(): Rect? =
    if (detectionAreaLeft != null && detectionAreaTop != null && detectionAreaRight != null && detectionAreaBottom != null)
        Rect(detectionAreaLeft!!, detectionAreaTop!!, detectionAreaRight!!, detectionAreaBottom!!)
    else
        null

private fun String?.toTextLanguage(): TrainedTextLanguage {
    if (this == null) return TrainedTextLanguage.ENGLISH

    return try {
        TrainedTextLanguage.valueOf(this)
    } catch (ex: IllegalArgumentException) {
        TrainedTextLanguage.ENGLISH
    }
}
