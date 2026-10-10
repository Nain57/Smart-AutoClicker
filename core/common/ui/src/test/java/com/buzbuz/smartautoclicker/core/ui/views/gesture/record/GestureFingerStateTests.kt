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
package com.buzbuz.smartautoclicker.core.ui.views.gesture.record

import android.graphics.PointF
import android.os.Build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [GestureFingerState].
 *
 * Unless stated otherwise, tests use a display density of 1 so that the dp thresholds translate directly to pixels:
 * a finger is still within 5px of its reference, a move direction is locked past 16px from the segment start, a
 * pause must last 200ms to become a wait and a direction change must exceed 10°.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureFingerStateTests {

    // --- Initial state ---

    @Test
    fun init_hasOnlyThePressMotion() {
        val state = newState(x = 10f, y = 20f, timeMs = 100L)

        assertEquals(listOf(press(10f, 20f)), state.motions)
    }

    @Test
    fun init_isStationaryWithNoPressDuration() {
        val state = newState(x = 10f, y = 20f, timeMs = 100L)

        assertTrue(state.isStationary)
        assertEquals(0L, state.pressDurationMs)
        assertEquals(PointF(10f, 20f), state.pressPosition)
    }

    // --- Still detection ---

    @Test
    fun jitterWithinStillThreshold_addsNothingAndStaysStationary() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(13f, 10f, timeMs = 20L)
        state.move(10f, 13f, timeMs = 40L)
        state.move(7f, 7f, timeMs = 60L)

        assertEquals(listOf(press(10f, 10f)), state.motions)
        assertTrue(state.isStationary)
        assertEquals(PointF(10f, 10f), state.pressPosition)
    }

    @Test
    fun jitterWithinStillThreshold_updatesPressDuration() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(12f, 10f, timeMs = 150L)

        assertEquals(150L, state.pressDurationMs)
    }

    @Test
    fun moveExactlyAtStillThreshold_isStill() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)

        state.move(5f, 0f, timeMs = 20L)

        assertEquals(listOf(press(0f, 0f)), state.motions)
    }

    @Test
    fun moveJustPastStillThreshold_isMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)

        state.move(6f, 0f, timeMs = 20L)

        assertEquals(listOf(press(0f, 0f), moveTo(6f, 0f, durationMs = 20L)), state.motions)
        assertFalse(state.isStationary)
    }

    @Test
    fun stillThreshold_scalesWithDisplayDensity() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L, displayDensity = 2f)

        state.move(8f, 0f, timeMs = 20L)
        assertEquals(listOf(press(0f, 0f)), state.motions)

        state.move(11f, 0f, timeMs = 40L)
        assertEquals(listOf(press(0f, 0f), moveTo(11f, 0f, durationMs = 40L)), state.motions)
    }

    @Test
    fun jitterBeforeMove_absorbedIntoMoveDuration() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(13f, 10f, timeMs = 20L)
        state.move(10f, 13f, timeMs = 40L)
        state.move(100f, 10f, timeMs = 60L)

        assertEquals(listOf(press(10f, 10f), moveTo(100f, 10f, durationMs = 60L)), state.motions)
    }

    // --- Wait ---

    @Test
    fun stillBelowMinWaitDuration_addsNoWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(12f, 10f, timeMs = 199L)

        assertEquals(listOf(press(10f, 10f)), state.motions)
    }

    @Test
    fun stillForMinWaitDuration_addsWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(12f, 10f, timeMs = 200L)

        assertEquals(listOf(press(10f, 10f), wait(durationMs = 200L)), state.motions)
        assertTrue(state.isStationary)
    }

    @Test
    fun stillAfterWait_extendsExistingWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)
        state.move(12f, 10f, timeMs = 200L)

        state.move(11f, 11f, timeMs = 250L)
        state.move(10f, 12f, timeMs = 300L)

        assertEquals(listOf(press(10f, 10f), wait(durationMs = 300L)), state.motions)
        assertEquals(300L, state.pressDurationMs)
    }

    @Test
    fun releaseAfterWait_waitCoversWholeStillPeriod() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)
        state.move(11f, 10f, timeMs = 250L)

        state.addRelease(eventTime = 400L)

        assertEquals(listOf(press(10f, 10f), wait(durationMs = 400L), release()), state.motions)
    }

    @Test
    fun moveAfterWait_waitEndsAtLastStillEventAndMoveStartsFromThere() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)
        state.move(11f, 10f, timeMs = 250L)

        state.move(100f, 10f, timeMs = 300L)

        assertEquals(
            listOf(press(10f, 10f), wait(durationMs = 250L), moveTo(100f, 10f, durationMs = 50L)),
            state.motions,
        )
    }

    @Test
    fun releaseAfterLongStillWithoutPendingWait_recordsWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)
        state.move(11f, 10f, timeMs = 150L)

        state.addRelease(eventTime = 500L)

        assertEquals(listOf(press(10f, 10f), wait(durationMs = 500L), release()), state.motions)
    }

    @Test
    fun releaseAfterShortStill_addsNoWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)
        state.move(11f, 10f, timeMs = 100L)

        state.addRelease(eventTime = 150L)

        assertEquals(listOf(press(10f, 10f), release()), state.motions)
    }

    @Test
    fun waitAfterMove_isAppendedToTheMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(101f, 0f, timeMs = 100L)
        state.move(100f, 1f, timeMs = 300L)

        assertEquals(
            listOf(press(0f, 0f), moveTo(100f, 0f, durationMs = 50L), wait(durationMs = 250L)),
            state.motions,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun stillForLong_belowOreo_addsNoWait() {
        val state = newState(x = 10f, y = 10f, timeMs = 0L)

        state.move(12f, 10f, timeMs = 1000L)
        state.move(100f, 10f, timeMs = 1050L)

        assertEquals(listOf(press(10f, 10f), moveTo(100f, 10f, durationMs = 1050L)), state.motions)
    }

    // --- Moves ---

    @Test
    fun moveBeyondStillThreshold_addsMoveWithDurationSincePress() {
        val state = newState(x = 10f, y = 10f, timeMs = 100L)

        state.move(100f, 10f, timeMs = 150L)

        assertEquals(listOf(press(10f, 10f), moveTo(100f, 10f, durationMs = 50L)), state.motions)
        assertFalse(state.isStationary)
    }

    @Test
    fun consecutiveMovesInSameDirection_mergeIntoOne() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)

        state.move(30f, 0f, timeMs = 20L)
        state.move(60f, 0f, timeMs = 40L)
        state.move(100f, 1f, timeMs = 60L)

        assertEquals(listOf(press(0f, 0f), moveTo(100f, 1f, durationMs = 60L)), state.motions)
    }

    @Test
    fun turnBeforeAngleLockDistance_merges() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)

        state.move(10f, 0f, timeMs = 20L)
        state.move(10f, 10f, timeMs = 40L)

        assertEquals(listOf(press(0f, 0f), moveTo(10f, 10f, durationMs = 40L)), state.motions)
    }

    @Test
    fun turnAfterAngleLockDistance_startsNewMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)

        state.move(20f, 0f, timeMs = 20L)
        state.move(20f, 20f, timeMs = 40L)

        assertEquals(
            listOf(press(0f, 0f), moveTo(20f, 0f, durationMs = 20L), moveTo(20f, 20f, durationMs = 20L)),
            state.motions,
        )
    }

    @Test
    fun angleLockDistance_scalesWithDisplayDensity() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L, displayDensity = 2f)

        state.move(20f, 0f, timeMs = 20L)
        state.move(20f, 20f, timeMs = 40L)

        assertEquals(listOf(press(0f, 0f), moveTo(20f, 20f, durationMs = 40L)), state.motions)
    }

    @Test
    fun deviationWithinTenDegrees_merges() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(200f, 15f, timeMs = 100L)

        assertEquals(listOf(press(0f, 0f), moveTo(200f, 15f, durationMs = 100L)), state.motions)
    }

    @Test
    fun deviationBeyondTenDegrees_startsNewMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(200f, 60f, timeMs = 100L)

        assertEquals(
            listOf(press(0f, 0f), moveTo(100f, 0f, durationMs = 50L), moveTo(200f, 60f, durationMs = 50L)),
            state.motions,
        )
    }

    @Test
    fun arcDeviation_measuredFromSegmentStartNotLastStep() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        // Each step turns more than 10° from the previous one, but stays within 10° of the segment start.
        state.move(200f, 15f, timeMs = 100L)
        state.move(250f, 30f, timeMs = 150L)
        assertEquals(listOf(press(0f, 0f), moveTo(250f, 30f, durationMs = 150L)), state.motions)

        // Cumulative turn from the segment start finally exceeds 10°.
        state.move(300f, 60f, timeMs = 200L)
        assertEquals(
            listOf(press(0f, 0f), moveTo(250f, 30f, durationMs = 150L), moveTo(300f, 60f, durationMs = 50L)),
            state.motions,
        )
    }

    @Test
    fun slightBackwardsMove_merges() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(96f, 4f, timeMs = 100L)

        assertEquals(listOf(press(0f, 0f), moveTo(96f, 4f, durationMs = 100L)), state.motions)
    }

    @Test
    fun backwardsMoveBeyondStillThreshold_startsNewMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(90f, 0f, timeMs = 100L)

        assertEquals(
            listOf(press(0f, 0f), moveTo(100f, 0f, durationMs = 50L), moveTo(90f, 0f, durationMs = 50L)),
            state.motions,
        )
    }

    @Test
    fun newSegment_locksItsOwnDirectionFromItsStart() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)
        state.move(100f, 100f, timeMs = 100L)

        state.move(100f, 200f, timeMs = 150L)
        state.move(200f, 200f, timeMs = 200L)

        assertEquals(
            listOf(
                press(0f, 0f),
                moveTo(100f, 0f, durationMs = 50L),
                moveTo(100f, 200f, durationMs = 100L),
                moveTo(200f, 200f, durationMs = 50L),
            ),
            state.motions,
        )
    }

    @Test
    fun shortStillBetweenMoves_absorbedIntoFollowingMove() {
        val state = newState(x = 0f, y = 0f, timeMs = 0L)
        state.move(100f, 0f, timeMs = 50L)

        state.move(101f, 0f, timeMs = 100L)
        state.move(100f, 0f, timeMs = 150L)
        state.move(200f, 0f, timeMs = 200L)

        assertEquals(listOf(press(0f, 0f), moveTo(200f, 0f, durationMs = 200L)), state.motions)
    }

    // --- Release & conversion ---

    @Test
    fun release_appendsReleaseAndUpdatesPressDuration() {
        val state = newState(x = 10f, y = 10f, timeMs = 100L)
        state.move(100f, 10f, timeMs = 150L)

        state.addRelease(eventTime = 200L)

        assertEquals(listOf(press(10f, 10f), moveTo(100f, 10f, durationMs = 50L), release()), state.motions)
        assertEquals(100L, state.pressDurationMs)
    }

    @Test
    fun toGestureFinger_usesPressTimeAndSnapshotsMotions() {
        val state = newState(x = 10f, y = 10f, timeMs = 100L)
        state.move(100f, 10f, timeMs = 150L)

        val finger = state.toGestureFinger()
        state.addRelease(eventTime = 200L)

        assertEquals(
            RecordedGestureFinger(
                pressedAtMs = 100L,
                motions = listOf(press(10f, 10f), moveTo(100f, 10f, durationMs = 50L)),
            ),
            finger,
        )
    }

    // --- Helpers ---

    private fun newState(x: Float, y: Float, timeMs: Long, displayDensity: Float = 1f) =
        GestureFingerState(initialPosition = PointF(x, y), initialEventTime = timeMs, displayDensity = displayDensity)

    private fun GestureFingerState.move(x: Float, y: Float, timeMs: Long) =
        addMoveOrWait(newPosition = PointF(x, y), eventTime = timeMs)

    private fun press(x: Float, y: Float) =
        RecordedGestureFingerMotion.Press(PointF(x, y))

    private fun moveTo(x: Float, y: Float, durationMs: Long) =
        RecordedGestureFingerMotion.Move(moveToPosition = PointF(x, y), moveToDurationMs = durationMs)

    private fun wait(durationMs: Long) =
        RecordedGestureFingerMotion.Wait(waitDurationMs = durationMs)

    private fun release() =
        RecordedGestureFingerMotion.Release
}
