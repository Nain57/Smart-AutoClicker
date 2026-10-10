/*
 * Copyright (C) 2026 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.heldEventCheckMessage

import com.buzbuz.smartautoclicker.core.smart.debugging.HeldEventCheckMessage as ProtoHeldEventCheckMessage


internal fun DebugReportEventOccurrence.HeldEventCheck.toHeldEventCheckProtobuf(): ProtoHeldEventCheckMessage =
    heldEventCheckMessage {
        val check = this@toHeldEventCheckProtobuf

        eventId = check.eventId
        holdState =
            if (check.isHeld) ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_HELD
            else ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED
        holdDurationMs = check.holdDurationMs

        when (check) {
            is DebugReportEventOccurrence.HeldEventCheck.Screen -> {
                eventType = ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_SCREEN
                frameNumber = check.frameNumber
                imageConditionResults.addAll(check.conditionsResults.map { result -> result.toProtobuf() })
            }

            is DebugReportEventOccurrence.HeldEventCheck.Trigger -> {
                eventType = ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_TRIGGER
                triggerConditionResults.addAll(check.conditionsResults.map { result -> result.toProtobuf() })
            }
        }
    }

internal fun ProtoHeldEventCheckMessage.toDomain(relativeTimestamp: Long): DebugReportEventOccurrence.HeldEventCheck? {
    val isHeld = when (holdState) {
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_HELD -> true
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED -> false
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_UNSPECIFIED,
        ProtoHeldEventCheckMessage.HoldState.UNRECOGNIZED -> return null
    }

    return when (eventType) {
        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_SCREEN ->
            DebugReportEventOccurrence.HeldEventCheck.Screen(
                eventId = eventId,
                relativeTimestampMs = relativeTimestamp,
                conditionsResults = imageConditionResultsList.map { result -> result.toDomain() },
                isHeld = isHeld,
                holdDurationMs = holdDurationMs,
                frameNumber = frameNumber,
            )

        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_TRIGGER ->
            DebugReportEventOccurrence.HeldEventCheck.Trigger(
                eventId = eventId,
                relativeTimestampMs = relativeTimestamp,
                conditionsResults = triggerConditionResultsList.map { result -> result.toDomain() },
                isHeld = isHeld,
                holdDurationMs = holdDurationMs,
            )

        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_UNSPECIFIED,
        ProtoHeldEventCheckMessage.EventType.UNRECOGNIZED -> null
    }
}
