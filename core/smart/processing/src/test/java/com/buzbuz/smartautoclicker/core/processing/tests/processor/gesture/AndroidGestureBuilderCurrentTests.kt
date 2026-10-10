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
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilderCurrent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.NEW_FINGER_PRESS_DELAY_MS
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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the slicing of a gesture into continued strokes by [AndroidGestureBuilderCurrent]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidGestureBuilderCurrentTests {

    private lateinit var builder: AndroidGestureBuilderCurrent

    @Before
    fun setUp() {
        builder = AndroidGestureBuilderCurrent(newProcessingState(), random = null)
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
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertHoldStroke(chain[0].getStroke(0), 50L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun longPress_singleHoldStrokeOfTheWaitDuration() {
        val chains = build(newGesture(newFinger(0L, press(10, 20), wait(300L), release())))

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertHoldStroke(chain[0].getStroke(0), 0L, 300L, Point(10, 20))
    }

    @Test
    fun swipe_singleLineStroke() {
        val chains = build(newGesture(newFinger(0L, press(0, 0), move(100, 200, 500L), release())))

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertStroke(chain[0].getStroke(0), 0L, 500L, from = Point(0, 0), to = Point(100, 200))
    }

    @Test
    fun moveThenMove_secondStrokeContinuesTheFirst() {
        val chains = build(
            newGesture(newFinger(0L, press(0, 0), move(100, 0, 200L), move(100, 100, 300L), release()))
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        val firstStroke = chain[0].getStroke(0)
        assertStroke(firstStroke, 0L, 200L, from = Point(0, 0), to = Point(100, 0), willContinue = true)
        assertStroke(chain[1].getStroke(0), 0L, 300L, from = Point(100, 0), to = Point(100, 100), continues = firstStroke)
    }

    @Test
    fun moveThenWait_waitIsAHeldContinuation() {
        val chains = build(newGesture(newFinger(0L, press(0, 0), move(100, 100, 200L), wait(300L), release())))

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        val moveStroke = chain[0].getStroke(0)
        assertStroke(moveStroke, 0L, 200L, from = Point(0, 0), to = Point(100, 100), willContinue = true)
        assertHoldStroke(chain[1].getStroke(0), 0L, 300L, Point(100, 100), continues = moveStroke)
    }

    @Test
    fun waitThenMove_pressIsDispatchedThenMoveIsDelayedByTheWait() {
        val chains = build(newGesture(newFinger(0L, press(10, 10), wait(300L), move(50, 50, 200L), release())))

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        val pressStroke = chain[0].getStroke(0)
        assertHoldStroke(pressStroke, 0L, 300L, Point(10, 10), willContinue = true)
        assertStroke(chain[1].getStroke(0), 300L, 200L, from = Point(10, 10), to = Point(50, 50), continues = pressStroke)
    }

    @Test
    fun moveThenWaitThenMove_waitIsMergedIntoTheNextStrokeDelay() {
        val chains = build(
            newGesture(newFinger(0L, press(0, 0), move(100, 0, 100L), wait(150L), move(100, 100, 100L), release()))
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        val firstMove = chain[0].getStroke(0)
        assertStroke(firstMove, 0L, 100L, from = Point(0, 0), to = Point(100, 0), willContinue = true)
        assertStroke(chain[1].getStroke(0), 150L, 100L, from = Point(100, 0), to = Point(100, 100), continues = firstMove)
    }

    @Test
    fun consecutiveWaits_delaysAreAccumulated() {
        val chains = build(
            newGesture(newFinger(0L, press(10, 10), wait(100L), wait(200L), move(50, 50, 300L), release()))
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        val pressStroke = chain[0].getStroke(0)
        assertHoldStroke(pressStroke, 0L, 100L, Point(10, 10), willContinue = true)
        assertStroke(chain[1].getStroke(0), 300L, 300L, from = Point(10, 10), to = Point(50, 50), continues = pressStroke)
    }

    @Test
    fun sequentialFingers_eachTapIsItsOwnChain() {
        val chains = build(newGesture(newTapFinger(10, 10), newTapFinger(20, 20, startTimeMs = 200L)))

        assertChainsCount(2, chains)
        chains!!.forEach { chain -> assertGesturesCount(1, chain) }

        assertHoldStroke(chains[0][0].getStroke(0), 0L, TAP_DURATION_MS, Point(10, 10))
        // The second tap only starts once the first one is fully played: it is delayed by the gap between them.
        assertHoldStroke(chains[1][0].getStroke(0), 100L, TAP_DURATION_MS, Point(20, 20))
    }

    @Test
    fun simultaneousFingers_singleGestureWithOneStrokePerFinger() {
        val chains = build(
            newGesture(
                newFinger(0L, press(0, 0), move(100, 0, 400L), release()),
                newFinger(0L, press(500, 0), move(400, 0, 400L), release()),
            )
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertStrokesCount(2, chain[0])
        assertStroke(chain[0].getStroke(0), 0L, 400L, from = Point(0, 0), to = Point(100, 0))
        assertStroke(chain[0].getStroke(1), 0L, 400L, from = Point(500, 0), to = Point(400, 0))
    }

    @Test
    fun waitingFingerAlongMovingFinger_singleGesture() {
        val chains = build(
            newGesture(
                newFinger(0L, press(0, 0), wait(200L), release()),
                newFinger(0L, press(50, 50), move(100, 100, 200L), release()),
            )
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(1, chain)
        assertStrokesCount(2, chain[0])
        assertHoldStroke(chain[0].getStroke(0), 0L, 200L, Point(0, 0))
        assertStroke(chain[0].getStroke(1), 0L, 200L, from = Point(50, 50), to = Point(100, 100))
    }

    @Test
    fun fingerPressingWhileAnotherIsHeld_pressIsDelayedAndMovingFingerIsSplit() {
        val chains = build(
            newGesture(
                newFinger(0L, press(0, 0), move(100, 100, 200L), move(200, 200, 200L), release()),
                newTapFinger(300, 300, startTimeMs = 200L),
            )
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(3, chain)

        // Slice 0-200ms: only the first finger, moving.
        assertStrokesCount(1, chain[0])
        val firstMove = chain[0].getStroke(0)
        assertStroke(firstMove, 0L, 200L, from = Point(0, 0), to = Point(100, 100), willContinue = true)

        // Slice 200-300ms: the first finger keeps moving, the second one taps. Its press can't be in the first step
        // of the slice, it is delayed by a millisecond.
        assertStrokesCount(2, chain[1])
        val secondMove = chain[1].getStroke(0)
        assertStroke(secondMove, 0L, 100L, from = Point(100, 100), to = Point(150, 150), willContinue = true, continues = firstMove)
        assertHoldStroke(chain[1].getStroke(1), NEW_FINGER_PRESS_DELAY_MS, TAP_DURATION_MS - NEW_FINGER_PRESS_DELAY_MS, Point(300, 300))

        // Slice 300-400ms: only the first finger, ending its move.
        assertStrokesCount(1, chain[2])
        assertStroke(chain[2].getStroke(0), 0L, 100L, from = Point(150, 150), to = Point(200, 200), continues = secondMove)
    }

    @Test
    fun fingerReleasedWhileAnotherIsHeld_heldFingerIsSplitAtTheRelease() {
        val chains = build(
            newGesture(
                newFinger(0L, press(0, 0), wait(300L), release()),
                newFinger(0L, press(50, 50), move(100, 100, 100L), release()),
            )
        )

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)

        // Slice 0-100ms: the second finger swipes while the first one is held.
        assertStrokesCount(2, chain[0])
        val holdStroke = chain[0].getStroke(0)
        assertHoldStroke(holdStroke, 0L, 100L, Point(0, 0), willContinue = true)
        assertStroke(chain[0].getStroke(1), 0L, 100L, from = Point(50, 50), to = Point(100, 100))

        // Slice 100-300ms: the first finger stays held until its release.
        assertStrokesCount(1, chain[1])
        assertHoldStroke(chain[1].getStroke(0), 0L, 200L, Point(0, 0), continues = holdStroke)
    }
}
