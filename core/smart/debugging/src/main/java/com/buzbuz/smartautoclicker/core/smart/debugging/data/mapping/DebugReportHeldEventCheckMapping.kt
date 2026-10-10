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
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportHoldReleaseReason
import com.buzbuz.smartautoclicker.core.smart.debugging.heldEventCheckMessage

import com.buzbuz.smartautoclicker.core.smart.debugging.HeldEventCheckMessage as ProtoHeldEventCheckMessage


internal fun DebugReportEventOccurrence.HeldEventCheck.toHeldEventCheckProtobuf(): ProtoHeldEventCheckMessage =
    heldEventCheckMessage {
        val check = this@toHeldEventCheckProtobuf

        eventId = check.eventId
        holdState = check.releaseReason.toProtoHoldState()
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
    val releaseReason = when (holdState) {
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_HELD -> null
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_UNFULFILLED -> DebugReportHoldReleaseReason.UNFULFILLED
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_INTERRUPTED -> DebugReportHoldReleaseReason.INTERRUPTED
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_EVENT_DISABLED -> DebugReportHoldReleaseReason.EVENT_DISABLED
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_OTHER_EVENT -> DebugReportHoldReleaseReason.OTHER_EVENT
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_SCREEN_ROTATION -> DebugReportHoldReleaseReason.SCREEN_ROTATION
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_SCENARIO_STOPPED -> DebugReportHoldReleaseReason.SCENARIO_STOPPED
        ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_UNSPECIFIED,
        ProtoHeldEventCheckMessage.HoldState.UNRECOGNIZED -> return null
    }

    return when (eventType) {
        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_SCREEN ->
            DebugReportEventOccurrence.HeldEventCheck.Screen(
                eventId = eventId,
                relativeTimestampMs = relativeTimestamp,
                conditionsResults = imageConditionResultsList.map { result -> result.toDomain() },
                releaseReason = releaseReason,
                holdDurationMs = holdDurationMs,
                frameNumber = frameNumber,
            )

        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_TRIGGER ->
            DebugReportEventOccurrence.HeldEventCheck.Trigger(
                eventId = eventId,
                relativeTimestampMs = relativeTimestamp,
                conditionsResults = triggerConditionResultsList.map { result -> result.toDomain() },
                releaseReason = releaseReason,
                holdDurationMs = holdDurationMs,
            )

        ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_UNSPECIFIED,
        ProtoHeldEventCheckMessage.EventType.UNRECOGNIZED -> null
    }
}

private fun DebugReportHoldReleaseReason?.toProtoHoldState(): ProtoHeldEventCheckMessage.HoldState =
    when (this) {
        null -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_HELD
        DebugReportHoldReleaseReason.UNFULFILLED -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_UNFULFILLED
        DebugReportHoldReleaseReason.INTERRUPTED -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_INTERRUPTED
        DebugReportHoldReleaseReason.EVENT_DISABLED -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_EVENT_DISABLED
        DebugReportHoldReleaseReason.OTHER_EVENT -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_OTHER_EVENT
        DebugReportHoldReleaseReason.SCREEN_ROTATION -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_SCREEN_ROTATION
        DebugReportHoldReleaseReason.SCENARIO_STOPPED -> ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_RELEASED_SCENARIO_STOPPED
    }
