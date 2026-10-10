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

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.heldEventCheckMessage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportMessage as ProtoDebugReportMessage
import com.buzbuz.smartautoclicker.core.smart.debugging.HeldEventCheckMessage as ProtoHeldEventCheckMessage

class DebugReportHeldEventCheckMappingTests {

    private fun DebugReportEventOccurrence.roundTrip(): DebugReportEventOccurrence? =
        ProtoDebugReportMessage.parseFrom(toProtobuf().toByteArray()).toDomain()

    @Test
    fun screenEvent_held_roundTrip() {
        val occurrence = DebugReportEventOccurrence.HeldEventCheck.Screen(
            eventId = 12L,
            relativeTimestampMs = 4_500L,
            conditionsResults = listOf(
                DebugReportConditionResult.ScreenCondition(
                    conditionId = 3L,
                    isFulFilled = true,
                    detectionDurationMs = 7L,
                    confidenceRate = 98.5,
                ),
            ),
            isHeld = true,
            holdDurationMs = 1_250L,
            frameNumber = 42L,
        )

        assertEquals(occurrence, occurrence.roundTrip())
    }

    @Test
    fun screenEvent_released_roundTrip() {
        val occurrence = DebugReportEventOccurrence.HeldEventCheck.Screen(
            eventId = 12L,
            relativeTimestampMs = 4_500L,
            conditionsResults = listOf(
                DebugReportConditionResult.ScreenCondition(
                    conditionId = 3L,
                    isFulFilled = false,
                    detectionDurationMs = 7L,
                    confidenceRate = 12.0,
                ),
            ),
            isHeld = false,
            holdDurationMs = 3_000L,
            frameNumber = 43L,
        )

        assertEquals(occurrence, occurrence.roundTrip())
    }

    @Test
    fun triggerEvent_held_roundTrip() {
        val occurrence = DebugReportEventOccurrence.HeldEventCheck.Trigger(
            eventId = 13L,
            relativeTimestampMs = 9_000L,
            conditionsResults = listOf(
                DebugReportConditionResult.TriggerCondition(conditionId = 5L, isFulFilled = true),
            ),
            isHeld = true,
            holdDurationMs = 6_000L,
        )

        assertEquals(occurrence, occurrence.roundTrip())
    }

    @Test
    fun triggerEvent_released_roundTrip() {
        val occurrence = DebugReportEventOccurrence.HeldEventCheck.Trigger(
            eventId = 13L,
            relativeTimestampMs = 9_000L,
            conditionsResults = listOf(
                DebugReportConditionResult.TriggerCondition(conditionId = 5L, isFulFilled = false),
            ),
            isHeld = false,
            holdDurationMs = 6_000L,
        )

        assertEquals(occurrence, occurrence.roundTrip())
    }

    @Test
    fun heldEventCheck_hasNoActionsResults() {
        val occurrence = DebugReportEventOccurrence.HeldEventCheck.Trigger(
            eventId = 13L,
            relativeTimestampMs = 0L,
            conditionsResults = emptyList(),
            isHeld = true,
            holdDurationMs = 0L,
        )

        assertEquals(emptyList<Any>(), occurrence.counterChanges)
        assertEquals(emptyList<Any>(), occurrence.eventStateChanges)
    }

    @Test
    fun unspecifiedHoldState_isIgnored() {
        val message = heldEventCheckMessage {
            eventId = 1L
            eventType = ProtoHeldEventCheckMessage.EventType.EVENT_TYPE_SCREEN
        }

        assertNull(message.toDomain(0L))
    }

    @Test
    fun unspecifiedEventType_isIgnored() {
        val message = heldEventCheckMessage {
            eventId = 1L
            holdState = ProtoHeldEventCheckMessage.HoldState.HOLD_STATE_HELD
        }

        assertNull(message.toDomain(0L))
    }
}
