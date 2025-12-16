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
package com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping

import android.util.Log

import com.buzbuz.smartautoclicker.core.smart.debugging.ImageEventMessageKt.imageConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.TriggerEventMessageKt.triggerConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.debugReportMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.imageEventMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.triggerEventMessage

import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportMessage as ProtoDebugReportMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.ImageEventMessage as ProtoImageEventMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.TriggerEventMessage as ProtoTriggerEventMessage


internal fun DebugReportEventOccurrence.toProtobuf(): ProtoDebugReportMessage =
    debugReportMessage {
        relativeTimestampMs = this@toProtobuf.relativeTimestampMs
        when (this@toProtobuf) {
            is DebugReportEventOccurrence.ImageEvent -> imageEventMessage = this@toProtobuf.toImageEventProtobuf()
            is DebugReportEventOccurrence.TriggerEvent -> triggerEventMessage = this@toProtobuf.toTriggerEventProtobuf()
        }
    }

private fun DebugReportEventOccurrence.ImageEvent.toImageEventProtobuf(): ProtoImageEventMessage =
    imageEventMessage {
        eventId = this@toImageEventProtobuf.eventId
        results.addAll(
            values = this@toImageEventProtobuf.conditionsResults.map { result ->
                imageConditionResult {
                    conditionId = result.conditionId
                    isFulfilled = result.isFulFilled
                    durationMs = result.detectionDurationMs
                    confidenceRate = result.confidenceRate
                }
            }
        )
    }

private fun DebugReportEventOccurrence.TriggerEvent.toTriggerEventProtobuf(): ProtoTriggerEventMessage =
    triggerEventMessage {
        eventId = this@toTriggerEventProtobuf.eventId
        results.addAll(
            values = this@toTriggerEventProtobuf.conditionsResults.map { result ->
                triggerConditionResult {
                    conditionId = result.conditionId
                    isFulfilled = result.isFulFilled
                }
            }
        )
    }


internal fun ProtoDebugReportMessage.toDomain(): DebugReportEventOccurrence? =
    when (messageTypeCase) {
        ProtoDebugReportMessage.MessageTypeCase.IMAGEEVENTMESSAGE ->
            imageEventMessage.toDomain(relativeTimestampMs)
        ProtoDebugReportMessage.MessageTypeCase.TRIGGEREVENTMESSAGE ->
            triggerEventMessage.toDomain(relativeTimestampMs)
        ProtoDebugReportMessage.MessageTypeCase.MESSAGETYPE_NOT_SET -> {
            Log.e(LOG_TAG, "Can't read DebugReportEventOccurrence from protobuf")
            null
        }
    }

private fun ProtoImageEventMessage.toDomain(relativeTimestamp: Long): DebugReportEventOccurrence.ImageEvent =
    DebugReportEventOccurrence.ImageEvent(
        eventId = eventId,
        relativeTimestampMs = relativeTimestamp,
        conditionsResults = resultsList.map { result ->
            DebugReportConditionResult.ImageCondition(
                conditionId = result.conditionId,
                isFulFilled = result.isFulfilled,
                detectionDurationMs = result.durationMs,
                confidenceRate = result.confidenceRate,
            )
        }
    )

private fun ProtoTriggerEventMessage.toDomain(relativeTimestamp: Long): DebugReportEventOccurrence.TriggerEvent =
    DebugReportEventOccurrence.TriggerEvent(
        eventId = eventId,
        relativeTimestampMs = relativeTimestamp,
        conditionsResults = resultsList.map { result ->
            DebugReportConditionResult.TriggerCondition(
                conditionId = result.conditionId,
                isFulFilled = result.isFulfilled,
            )
        }
    )

private const val LOG_TAG = "DebugReportEventOccurrenceMapping"