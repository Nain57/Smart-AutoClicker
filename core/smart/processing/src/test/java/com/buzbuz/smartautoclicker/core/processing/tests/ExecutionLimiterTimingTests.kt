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
package com.buzbuz.smartautoclicker.core.processing.tests

import com.buzbuz.smartautoclicker.core.processing.data.delayProcessingLoop
import com.buzbuz.smartautoclicker.core.processing.domain.DebugReportTimingListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class ExecutionLimiterTimingTests {

    @Test
    fun `unlimited mode safety delay is not reported as limiter time`() = runTest {
        val listener = mock<DebugReportTimingListener>()
        var delayedMs = 0L

        delayProcessingLoop(
            delayMs = 1L,
            timingListener = listener,
            isExecutionLimiterEnabled = false,
            elapsedRealtimeNanos = { error("Clock must not be read") },
            delayBlock = { delayedMs = it },
        )

        assertEquals(1L, delayedMs)
        verifyNoInteractions(listener)
    }

    @Test
    fun `limiter records elapsed wait including partial wait on cancellation`() = runTest {
        val listener = mock<DebugReportTimingListener>()
        val timestamps = ArrayDeque(listOf(100L, 275L))

        try {
            delayProcessingLoop(
                delayMs = 10L,
                timingListener = listener,
                isExecutionLimiterEnabled = true,
                elapsedRealtimeNanos = { timestamps.removeFirst() },
                delayBlock = { throw CancellationException("stop") },
            )
            fail("Expected limiter wait to be cancelled")
        } catch (_: CancellationException) {
            // Expected: the finally block must still retain the partial elapsed wait.
        }

        verify(listener).onExecutionLimiterWaited(175L)
    }
}
