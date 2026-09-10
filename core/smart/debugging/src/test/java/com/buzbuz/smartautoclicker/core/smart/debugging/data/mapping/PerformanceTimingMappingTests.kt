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

import com.buzbuz.smartautoclicker.core.smart.debugging.debugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.ConditionProfile
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class PerformanceTimingMappingTests {

    @Test
    fun `condition profile survives protobuf mapping`() {
        val expected = listOf(
            ConditionProfile(
                conditionId = 42L,
                checkCount = 10L,
                fulfilledCount = 3L,
                totalDurationNs = 1_000L,
                minDurationNs = 25L,
                maxDurationNs = 300L,
            )
        )

        val actual = expected.toProtobuf().conditionProfileMessage.toDomain()

        assertEquals(expected, actual)
    }

    @Test
    fun `empty condition profile remains a present report message`() {
        val message = emptyList<ConditionProfile>().toProtobuf()

        assertEquals(true, message.hasConditionProfileMessage())
        assertEquals(emptyList<ConditionProfile>(), message.conditionProfileMessage.toDomain())
    }

    @Test
    fun `overview performance durations survive protobuf mapping`() {
        val expected = DebugReportOverview(
            scenarioId = 7L,
            duration = 12_000.milliseconds,
            frameCount = 50L,
            averageFrameProcessingDuration = 4.milliseconds,
            imageEventFulfilledCount = 2,
            triggerEventFulfilledCount = 1,
            counterNames = setOf("counter"),
            activeDetectionDuration = 987_654_321.nanoseconds,
            executionLimiterWaitDuration = 123_456_789.nanoseconds,
        )

        assertEquals(expected, expected.toProtobuf().toDomain())
    }

    @Test
    fun `older overview without performance fields maps them to zero`() {
        val oldOverview = debugReportOverview {
            scenarioId = 7L
            durationMs = 1_000L
        }

        val mapped = oldOverview.toDomain()

        assertEquals(0.nanoseconds, mapped.activeDetectionDuration)
        assertEquals(0.nanoseconds, mapped.executionLimiterWaitDuration)
    }
}
