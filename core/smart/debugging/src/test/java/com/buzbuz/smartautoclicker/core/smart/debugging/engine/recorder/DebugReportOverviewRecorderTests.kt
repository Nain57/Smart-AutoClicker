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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import com.buzbuz.smartautoclicker.core.processing.domain.EventType

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DebugReportOverviewRecorderTests {

    private lateinit var recorder: DebugReportOverviewRecorder

    @Before
    fun setUp() {
        recorder = DebugReportOverviewRecorder()
    }

    private fun processList(eventType: EventType) {
        recorder.onFrameProcessingStarted(eventType)
        recorder.onFrameProcessingStopped(eventType)
    }

    @Test
    fun `trigger and screen lists on the same frame count once`() {
        repeat(3) {
            processList(EventType.Trigger)
            processList(EventType.Screen)
        }

        assertEquals(3L, recorder.frameCount)
    }

    @Test
    fun `screen only frames are counted`() {
        repeat(3) { processList(EventType.Screen) }

        assertEquals(3L, recorder.frameCount)
    }

    @Test
    fun `trigger only frames are counted`() {
        repeat(3) { processList(EventType.Trigger) }

        assertEquals(3L, recorder.frameCount)
    }

    @Test
    fun `screen list after a cancelled trigger list is a new frame`() {
        recorder.onFrameProcessingStarted(EventType.Trigger)
        recorder.onFrameProcessingStopped(null)
        processList(EventType.Screen)

        assertEquals(2L, recorder.frameCount)
    }

    @Test
    fun `frame number is the same during trigger and screen processing`() {
        recorder.onFrameProcessingStarted(EventType.Trigger)
        val triggerFrame = recorder.frameCount
        recorder.onFrameProcessingStopped(EventType.Trigger)
        recorder.onFrameProcessingStarted(EventType.Screen)

        assertEquals(triggerFrame, recorder.frameCount)
    }
}
