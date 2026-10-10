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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.Finger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.TAP_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.assertDurationNear
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.assertPointNear
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.move
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.press
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.release
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.resolve
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.wait
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilderCompat24
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilderCurrent
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.ResolvedFingerBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedSegment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import kotlin.random.Random

/** Tests the placement of a finger on the gesture timeline by [ResolvedFingerBuilder]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ResolvedFingerBuilderTests {

    private fun resolve(finger: Finger, random: Random? = null, supportsWait: Boolean = true): ResolvedFinger? =
        (if (supportsWait) AndroidGestureBuilderCurrent(random) else AndroidGestureBuilderCompat24(random))
            .resolve(0, finger)

    private fun assertSegment(segment: ResolvedSegment, startMs: Long, endMs: Long, from: Point, to: Point = from) {
        assertEquals("Invalid segment start", startMs, segment.startMs)
        assertEquals("Invalid segment end", endMs, segment.endMs)
        assertEquals("Invalid segment start position", from, segment.from)
        assertEquals("Invalid segment end position", to, segment.to)
    }

    @Test
    fun build_noMotions_returnsNull() {
        assertNull(resolve(newFinger(0L)))
    }

    @Test
    fun build_pressOnly_returnsNull() {
        assertNull(resolve(newFinger(0L, press(10, 20))))
    }

    @Test
    fun build_moveBeforePress_returnsNull() {
        assertNull(resolve(newFinger(0L, move(10, 20, 100L), press(10, 20), release())))
    }

    @Test
    fun build_releaseBeforePress_returnsNull() {
        assertNull(resolve(newFinger(0L, release(), press(10, 20), release())))
    }

    @Test
    fun build_pressingTwice_returnsNull() {
        assertNull(resolve(newFinger(0L, press(10, 20), press(30, 40), release())))
    }

    @Test
    fun build_releasingTwice_returnsNull() {
        assertNull(resolve(newFinger(0L, press(10, 20), release(), release())))
    }

    @Test
    fun build_moveAfterRelease_returnsNull() {
        assertNull(resolve(newFinger(0L, press(10, 20), release(), move(30, 40, 100L))))
    }

    @Test
    fun build_waitAfterRelease_returnsNull() {
        assertNull(resolve(newFinger(0L, press(10, 20), release(), wait(100L))))
    }

    @Test
    fun tap_singleSegmentOfTheTapDuration() {
        val finger = resolve(newTapFinger(10, 20))

        assertNotNull(finger)
        assertSegment(finger!!.segments.single(), 0L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun tap_delayedStart_shiftsTheTimeline() {
        val finger = resolve(newTapFinger(10, 20, startTimeMs = 50L))

        assertSegment(finger!!.segments.single(), 50L, 50L + TAP_DURATION_MS, Point(10, 20))
        assertEquals(50L, finger.downMs)
        assertEquals(50L + TAP_DURATION_MS, finger.upMs)
    }

    @Test
    fun negativeStartTime_coercedToZero() {
        val finger = resolve(newTapFinger(10, 20, startTimeMs = -50L))

        assertSegment(finger!!.segments.single(), 0L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun negativeCoordinates_clampedToZero() {
        val finger = resolve(newTapFinger(-10, -20))

        assertSegment(finger!!.segments.single(), 0L, TAP_DURATION_MS, Point(0, 0))
    }

    @Test
    fun moves_areChainedOnTheTimeline() {
        val finger = resolve(newFinger(0L, press(0, 0), move(100, 0, 200L), move(100, 100, 300L), release()))

        assertEquals(2, finger!!.segments.size)
        assertSegment(finger.segments[0], 0L, 200L, Point(0, 0), Point(100, 0))
        assertSegment(finger.segments[1], 200L, 500L, Point(100, 0), Point(100, 100))
    }

    @Test
    fun wait_isAStillSegment() {
        val finger = resolve(newFinger(0L, press(10, 10), wait(300L), move(50, 50, 200L), release()))

        assertEquals(2, finger!!.segments.size)
        assertSegment(finger.segments[0], 0L, 300L, Point(10, 10))
        assertSegment(finger.segments[1], 300L, 500L, Point(10, 10), Point(50, 50))
    }

    @Test
    fun wait_notSupported_isSkipped() {
        val finger = resolve(newFinger(0L, press(10, 10), wait(300L), move(50, 50, 200L), release()), supportsWait = false)

        assertSegment(finger!!.segments.single(), 0L, 200L, Point(10, 10), Point(50, 50))
    }

    @Test
    fun onlyWait_notSupported_fallsBackToATap() {
        val finger = resolve(newFinger(0L, press(10, 10), wait(300L), release()), supportsWait = false)

        assertSegment(finger!!.segments.single(), 0L, TAP_DURATION_MS, Point(10, 10))
    }

    @Test
    fun zeroDuration_coercedToMinimum() {
        val finger = resolve(newFinger(0L, press(0, 0), move(10, 10, 0L), release()))

        assertSegment(finger!!.segments.single(), 0L, 1L, Point(0, 0), Point(10, 10))
    }

    @Test
    fun randomize_positionsAndDurationsStayWithinOffsets() {
        val finger = resolve(newFinger(0L, press(100, 200), move(300, 400, 500L), release()), Random(42))

        val segment = finger!!.segments.single()
        assertPointNear(Point(100, 200), segment.from)
        assertPointNear(Point(300, 400), segment.to)
        assertDurationNear(500L, segment.endMs - segment.startMs)
    }

    @Test
    fun randomize_consecutiveSegments_shareTheirJunction() {
        val finger = resolve(
            newFinger(0L, press(0, 0), move(100, 0, 200L), wait(100L), move(100, 100, 300L), release()),
            Random(42),
        )

        assertEquals(3, finger!!.segments.size)
        assertEquals(finger.segments[0].to, finger.segments[1].from)
        assertEquals(finger.segments[1].from, finger.segments[1].to)
        assertEquals(finger.segments[1].to, finger.segments[2].from)
        assertEquals(finger.segments[0].endMs, finger.segments[1].startMs)
        assertEquals(finger.segments[1].endMs, finger.segments[2].startMs)
    }

    @Test
    fun randomize_negativeCoordinates_clampedToZero() {
        val finger = resolve(newTapFinger(0, 0), Random(42))

        val position = finger!!.segments.single().from
        assertTrue("Position should be clamped to the screen", position.x >= 0 && position.y >= 0)
    }
}
