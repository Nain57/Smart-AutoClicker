/* Copyright (C) 2026 Kevin Buzeau */
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
