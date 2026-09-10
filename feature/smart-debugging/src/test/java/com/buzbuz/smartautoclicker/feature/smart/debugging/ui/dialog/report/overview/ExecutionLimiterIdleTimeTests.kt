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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.overview

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import kotlin.time.Duration.Companion.nanoseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionLimiterIdleTimeTests {

    @Test
    fun `no limiter waiting is always zero including legacy reports`() {
        val result = buildExecutionLimiterIdleTime(
            overview = overview(activeNs = 1_000L, waitNs = 0L),
            occurrences = listOf(occurrence(null, null)),
        )

        assertEquals("0%", result?.formatPercentage())
    }

    @Test
    fun `action time is excluded from the denominator`() {
        val result = buildExecutionLimiterIdleTime(
            overview = overview(activeNs = 1_000L, waitNs = 100L),
            occurrences = listOf(occurrence(detectedAtNs = 50L, actionsCompletedAtNs = 950L)),
        )

        assertEquals(ExecutionLimiterIdleTime(waitDurationNs = 100L, measuredDurationNs = 200L), result)
        assertEquals("50.0%", result?.formatPercentage())
    }

    @Test
    fun `percentage formatting keeps meaningful endpoints`() {
        assertEquals("<0.1%", ExecutionLimiterIdleTime(1L, 2_000L).formatPercentage())
        assertEquals("99.9%", ExecutionLimiterIdleTime(9_999L, 10_000L).formatPercentage())
        assertEquals("100%", ExecutionLimiterIdleTime(10_000L, 10_000L).formatPercentage())
    }

    @Test
    fun `action duration beyond active duration is safely clamped`() {
        val result = buildExecutionLimiterIdleTime(
            overview = overview(activeNs = 100L, waitNs = 50L),
            occurrences = listOf(occurrence(detectedAtNs = 10L, actionsCompletedAtNs = 210L)),
        )

        assertEquals("100%", result?.formatPercentage())
    }

    @Test
    fun `legacy or malformed occurrences cannot estimate a nonzero report`() {
        assertNull(buildExecutionLimiterIdleTime(overview(1_000L, 100L), listOf(occurrence(null, null))))
        assertNull(buildExecutionLimiterIdleTime(overview(1_000L, 100L), listOf(occurrence(200L, 199L))))
    }

    private fun overview(activeNs: Long, waitNs: Long) = DebugReportOverview(
        scenarioId = 1L,
        duration = 0.nanoseconds,
        frameCount = 0L,
        averageFrameProcessingDuration = 0.nanoseconds,
        imageEventFulfilledCount = 0,
        triggerEventFulfilledCount = 0,
        counterNames = emptySet(),
        activeDetectionDuration = activeNs.nanoseconds,
        executionLimiterWaitDuration = waitNs.nanoseconds,
    )

    private fun occurrence(
        detectedAtNs: Long?,
        actionsCompletedAtNs: Long?,
    ) = DebugReportEventOccurrence.TriggerEvent(
        eventId = 1L,
        relativeTimestampMs = 0L,
        detectedAtNs = detectedAtNs,
        actionsCompletedAtNs = actionsCompletedAtNs,
        conditionsResults = emptyList(),
        counterChanges = emptyList(),
        eventStateChanges = emptyList(),
    )
}
