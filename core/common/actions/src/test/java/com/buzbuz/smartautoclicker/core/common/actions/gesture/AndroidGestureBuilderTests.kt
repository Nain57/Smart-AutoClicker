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

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.TAP_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.assertDurationNear
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.assertPointNear
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.build
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.move
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newGesture
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.press
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.release
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.wait
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilderCompat24
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilderCurrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import kotlin.random.Random

/**
 * Tests the behaviour shared by all [AndroidGestureBuilder] implementations: builder selection, error handling and
 * the randomization applied to the fingers it creates.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidGestureBuilderTests {

    private fun newBuilder(random: Random? = null): AndroidGestureBuilder =
        AndroidGestureBuilderCurrent(random)

    @Test
    fun createNewBuilder_currentSdk_returnsCurrent() {
        assertTrue(AndroidGestureBuilder.createNewBuilder(null) is AndroidGestureBuilderCurrent)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun createNewBuilder_belowOreo_returnsCompat24() {
        assertTrue(AndroidGestureBuilder.createNewBuilder(null) is AndroidGestureBuilderCompat24)
    }

    @Test
    fun build_noFingers_returnsNull() {
        assertNull(newBuilder().build(newGesture()))
    }

    @Test
    fun build_oneFingerInvalid_skipsOnlyThatFinger() {
        val gesture = newGesture(
            newFinger(0L, press(10, 20), press(30, 40), release()),
            newTapFinger(50, 60),
        )

        val chains = newBuilder().build(gesture)

        assertChainsCount(1, chains)
        val description = chains!!.first().single()
        assertStrokesCount(1, description)
        assertHoldStroke(description.getStroke(0), 0L, TAP_DURATION_MS, Point(50, 60))
    }

    @Test
    fun build_tooManyStrokes_returnsNull() {
        val fingers = Array(GestureDescription.getMaxStrokeCount() + 1) { index -> newTapFinger(index * 10, index * 10) }

        assertNull(newBuilder().build(newGesture(*fingers)))
    }

    @Test
    fun randomize_swipe_positionsAndDurationStayWithinOffsets() {
        val gesture = newGesture(newFinger(0L, press(100, 200), move(300, 400, 500L), release()))

        val chains = newBuilder(Random(42)).build(gesture)

        assertChainsCount(1, chains)
        val stroke = chains!!.first().single().getStroke(0)
        assertDurationNear(500L, stroke.duration)
        assertPointNear(Point(100, 200), stroke.points.first())
        assertPointNear(Point(300, 400), stroke.points.last())
    }

    @Test
    fun randomize_negativeCoordinates_clampedToZero() {
        val gesture = newGesture(newTapFinger(0, 0))

        val chains = newBuilder(Random(42)).build(gesture)

        assertChainsCount(1, chains)
        val position = chains!!.first().single().getStroke(0).points.single()
        assertTrue("Position should be clamped to the screen", position.x >= 0 && position.y >= 0)
    }

    @Test
    fun randomize_continuedStroke_startsWhereThePreviousOneEnded() {
        val gesture = newGesture(newFinger(0L, press(100, 200), move(300, 400, 500L), wait(300L), release()))

        val chains = newBuilder(Random(42)).build(gesture)

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)
        val moveStroke = chain[0].getStroke(0)
        val holdStroke = chain[1].getStroke(0)
        assertEquals("Continuation should start at the randomized end of the move", moveStroke.points.last(), holdStroke.points.single())
        assertEquals(moveStroke.strokeId, holdStroke.continuedStrokeId)
    }
}
