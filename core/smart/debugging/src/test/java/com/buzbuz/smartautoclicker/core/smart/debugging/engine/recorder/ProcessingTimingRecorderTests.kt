/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessingTimingRecorderTests {

    @Test
    fun `processing and limiter durations remain separate and reset together`() {
        val recorder = ProcessingTimingRecorder()

        recorder.recordDetectionLoop(100L)
        recorder.recordDetectionLoop(250L)
        recorder.recordExecutionLimiterWait(75L)

        assertEquals(350L, recorder.activeDetectionDurationNs)
        assertEquals(75L, recorder.executionLimiterWaitDurationNs)

        recorder.reset()

        assertEquals(0L, recorder.activeDetectionDurationNs)
        assertEquals(0L, recorder.executionLimiterWaitDurationNs)
    }
}
