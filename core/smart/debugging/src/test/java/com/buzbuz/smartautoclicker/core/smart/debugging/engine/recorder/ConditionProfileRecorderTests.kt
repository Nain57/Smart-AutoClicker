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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionProfileRecorderTests {

    @Test
    fun `record aggregates count outcomes and durations`() {
        val recorder = ConditionProfileRecorder()
        recorder.start(longArrayOf(42L, 7L, 42L))

        recorder.record(conditionId = 42L, durationNs = 100L, fulfilled = false)
        recorder.record(conditionId = 42L, durationNs = 300L, fulfilled = true)
        recorder.record(conditionId = 7L, durationNs = 50L, fulfilled = true)
        recorder.record(conditionId = 999L, durationNs = 1L, fulfilled = true)

        val profiles = recorder.snapshot()
        assertEquals(listOf(7L, 42L), profiles.map { it.conditionId })

        with(profiles.first { it.conditionId == 42L }) {
            assertEquals(2L, checkCount)
            assertEquals(1L, fulfilledCount)
            assertEquals(400L, totalDurationNs)
            assertEquals(100L, minDurationNs)
            assertEquals(300L, maxDurationNs)
        }
    }

    @Test
    fun `snapshot includes unchecked conditions with zero durations`() {
        val recorder = ConditionProfileRecorder()
        recorder.start(longArrayOf(5L))

        val profile = recorder.snapshot().single()
        assertEquals(0L, profile.checkCount)
        assertEquals(0L, profile.minDurationNs)
        assertEquals(0L, profile.maxDurationNs)
    }

    @Test
    fun `starting a new session discards the previous session`() {
        val recorder = ConditionProfileRecorder()
        recorder.start(longArrayOf(1L))
        recorder.record(conditionId = 1L, durationNs = 100L, fulfilled = true)

        recorder.start(longArrayOf(2L))
        recorder.record(conditionId = 2L, durationNs = 25L, fulfilled = false)

        val profile = recorder.snapshot().single()
        assertEquals(2L, profile.conditionId)
        assertEquals(1L, profile.checkCount)
        assertEquals(25L, profile.totalDurationNs)
    }
}
