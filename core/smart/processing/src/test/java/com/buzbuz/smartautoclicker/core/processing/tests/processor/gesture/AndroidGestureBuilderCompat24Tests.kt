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
package com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilderCompat24
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.TAP_DURATION_MS
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.move
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newGesture
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newProcessingState
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newScreenEvent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.press
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.release
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.wait

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests the single stroke per finger description of [AndroidGestureBuilderCompat24]. The builder only uses APIs
 * available before Android O, so it is tested on a recent SDK where the stroke paths can be inspected.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidGestureBuilderCompat24Tests {

    private lateinit var builder: AndroidGestureBuilderCompat24

    @Before
    fun setUp() {
        builder = AndroidGestureBuilderCompat24(newProcessingState(), random = null)
    }

    private fun build(gesture: Gesture): List<List<GestureDescription>>? =
        builder.build(gesture, newScreenEvent(), null)

    @Test
    fun tap_singleHoldStroke() {
        val chains = build(newGesture(newTapFinger(10, 20)))

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertStrokesCount(1, chain[0])
        assertHoldStroke(chain[0].getStroke(0), 0L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun tap_delayedStart_delaysTheStroke() {
        val chains = build(newGesture(newTapFinger(10, 20, startTimeMs = 50L)))

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 50L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun swipe_singleLineStroke() {
        val chains = build(newGesture(newFinger(0L, press(0, 0), move(100, 200, 500L), release())))

        assertChainsCount(1, chains)
        assertStroke(chains!!.first().single().getStroke(0), 0L, 500L, from = Point(0, 0), to = Point(100, 200))
    }

    @Test
    fun multipleMoves_singleStrokeFollowingAllPositions() {
        val chains = build(
            newGesture(newFinger(0L, press(0, 0), move(100, 0, 200L), move(100, 100, 300L), release()))
        )

        assertChainsCount(1, chains)
        val stroke = chains!!.first().single().getStroke(0)
        assertStroke(stroke, 0L, 500L, from = Point(0, 0), to = Point(100, 100))
        assertEquals(listOf(Point(0, 0), Point(100, 0), Point(100, 100)), stroke.points)
    }

    @Test
    fun wait_isSkipped() {
        val chains = build(newGesture(newFinger(0L, press(10, 10), wait(300L), move(50, 50, 200L), release())))

        assertChainsCount(1, chains)
        val stroke = chains!!.first().single().getStroke(0)
        assertStroke(stroke, 0L, 200L, from = Point(10, 10), to = Point(50, 50))
        assertEquals(listOf(Point(10, 10), Point(50, 50)), stroke.points)
    }

    @Test
    fun onlyWait_fallsBackToATap() {
        val chains = build(newGesture(newFinger(0L, press(10, 10), wait(300L), release())))

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(10, 10))
    }

    @Test
    fun multipleFingers_singleGestureWithOneStrokePerFinger() {
        val chains = build(
            newGesture(
                newFinger(0L, press(0, 0), move(100, 0, 400L), release()),
                newTapFinger(300, 300, startTimeMs = 200L),
            )
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertStrokesCount(2, chain[0])
        assertStroke(chain[0].getStroke(0), 0L, 400L, from = Point(0, 0), to = Point(100, 0))
        assertHoldStroke(chain[0].getStroke(1), 200L, TAP_DURATION_MS, Point(300, 300))
    }

    @Test
    fun tooManyFingers_returnsNull() {
        val fingers = Array(GestureDescription.getMaxStrokeCount() + 1) { index -> newTapFinger(index, index) }

        assertNull(build(newGesture(*fingers)))
    }
}
