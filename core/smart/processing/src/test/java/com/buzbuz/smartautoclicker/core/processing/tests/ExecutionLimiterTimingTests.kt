/* Copyright (C) 2026 Kevin Buzeau */
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
