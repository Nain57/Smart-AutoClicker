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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.Gesture
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.buildGesture
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.move
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newGesture
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.press
import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureBuilderTestData.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests how [AndroidGestureBuilderCurrent] holds a pointer down, and carries it along the next gestures. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidGestureBuilderHeldPointerTests {

    private lateinit var builder: AndroidGestureBuilderCurrent

    @Before
    fun setUp() {
        builder = AndroidGestureBuilderCurrent(random = null)
    }

    /** A gesture holding its first finger at [position], and the pointer it leaves down. */
    private fun newHold(position: Point): HeldPointer =
        builder.buildGesture(newGesture(newHoldingFinger(position)))!!.heldPointer!!

    private fun newHoldingFinger(position: Point) =
        newFinger(0L, press(position.x, position.y), release(hold = true))

    private fun build(gesture: Gesture, heldPointer: HeldPointer?): BuiltGesture =
        builder.buildGesture(gesture, heldPointer)!!

    /** All the strokes of a gesture, in the order they were added. */
    private fun GestureDescription.strokes(): List<StrokeDescription> =
        (0 until strokeCount).map { index -> getStroke(index) }

    /** The stroke continuing [heldPointer], the one keeping it down for this gesture. */
    private fun GestureDescription.carriedStroke(heldPointer: HeldPointer): StrokeDescription? =
        strokes().firstOrNull { stroke -> stroke.continuedStrokeId == heldPointer.stroke.strokeId }

    @Test
    fun hold_leavesPointerDown() {
        val position = Point(10, 20)
        val gesture = build(newGesture(newHoldingFinger(position)), null)

        val heldPointer = gesture.heldPointer
        assertNotNull("The gesture should leave a pointer down", heldPointer)
        assertEquals("The pointer is held at the wrong position", position, heldPointer!!.position)
        assertTrue("The last stroke of a held finger should continue", heldPointer.stroke.willContinue())
    }

    @Test
    fun hold_onSecondFinger_isIgnored() {
        val gesture = build(
            newGesture(newTapFinger(10, 20), newHoldingFinger(Point(30, 40))),
            null,
        )

        assertNull("Only the first finger of a gesture can be held", gesture.heldPointer)
    }

    @Test
    fun noHold_leavesNothingDown() {
        val gesture = build(newGesture(newTapFinger(10, 20)), null)

        assertNull("A gesture without hold should leave nothing down", gesture.heldPointer)
    }

    @Test
    fun carry_continuesHeldPointerInFirstGesture() {
        val heldPointer = newHold(Point(10, 20))

        val gesture = build(newGesture(newTapFinger(30, 40)), heldPointer)
        val firstGesture = gesture.chains.first().first()

        val carriedStroke = firstGesture.carriedStroke(heldPointer)
        assertNotNull("The held pointer should be continued by the first dispatched gesture", carriedStroke)
        assertEquals("The pointer should not have moved", listOf(Point(10, 20)), carriedStroke!!.points)
    }

    @Test
    fun carry_keepsPointerDownAfterGesture() {
        val heldPointer = newHold(Point(10, 20))

        val gesture = build(newGesture(newTapFinger(30, 40)), heldPointer)

        val newHeldPointer = gesture.heldPointer
        assertNotNull("The pointer should still be held once the gesture is played", newHeldPointer)
        assertEquals("The pointer should not have moved", Point(10, 20), newHeldPointer!!.position)
        assertTrue("The carried pointer should keep continuing", newHeldPointer.stroke.willContinue())
    }

    @Test
    fun carry_doesNotTakeTheSlotOfAGestureFinger() {
        val heldPointer = newHold(Point(1, 1))

        // Five fingers, the maximum a gesture can have, all pressing at the same time.
        val gesture = build(
            newGesture(
                newFinger(0L, press(10, 10), move(20, 20, 50L), release()),
                newFinger(0L, press(30, 30), move(40, 40, 50L), release()),
                newFinger(0L, press(50, 50), move(60, 60, 50L), release()),
                newFinger(0L, press(70, 70), move(80, 80, 50L), release()),
                newFinger(0L, press(90, 90), move(100, 100, 50L), release()),
            ),
            heldPointer,
        )

        val maxPointersDown = gesture.chains.flatten().maxOf { description -> description.strokeCount }
        assertEquals("The held pointer should be a sixth finger, not replace one of the five", 6, maxPointersDown)
    }

    @Test
    fun carry_pressesTheGestureFingersAfterTheFirstStep() {
        val heldPointer = newHold(Point(10, 20))

        val gesture = build(newGesture(newTapFinger(30, 40)), heldPointer)
        val firstGesture = gesture.chains.first().first()

        // The system refuses the continuation when a finger presses in the same step as the carried pointer.
        val carriedStroke = firstGesture.carriedStroke(heldPointer)!!
        firstGesture.strokes().filter { stroke -> stroke !== carriedStroke }.forEach { stroke ->
            assertTrue(
                "A finger pressing while a pointer is carried should start after it",
                stroke.startTime > carriedStroke.startTime,
            )
        }
    }

    @Test
    fun carry_isDispatchedAsASingleChain() {
        val heldPointer = newHold(Point(10, 20))

        // Two taps one after the other: without the carried pointer, the gap between them would break the chain.
        val gesture = build(
            newGesture(newTapFinger(30, 40), newTapFinger(50, 60, startTimeMs = 500L)),
            heldPointer,
        )

        assertEquals("A gesture carrying a pointer must be dispatched in one go", 1, gesture.chains.size)
    }

    @Test
    fun newHold_liftsThePreviousPointer() {
        val heldPointer = newHold(Point(10, 20))

        val gesture = build(newGesture(newHoldingFinger(Point(30, 40))), heldPointer)
        val firstGesture = gesture.chains.first().first()

        val carriedStroke = firstGesture.carriedStroke(heldPointer)
        assertNotNull("The previous pointer should be part of the gesture taking the hold over", carriedStroke)
        assertEquals(
            "The previous pointer should be lifted as soon as the new gesture starts",
            false,
            carriedStroke!!.willContinue(),
        )
    }

    @Test
    fun newHold_replacesTheHeldPointer() {
        val heldPointer = newHold(Point(10, 20))

        val gesture = build(newGesture(newHoldingFinger(Point(30, 40))), heldPointer)

        val newHeldPointer = gesture.heldPointer
        assertNotNull("The new finger should be the held one", newHeldPointer)
        assertEquals("The hold should have moved to the new finger", Point(30, 40), newHeldPointer!!.position)
    }

    @Test
    fun hold_belongsToTheGestureOwner() {
        val gesture = builder.buildGesture(newGesture(newHoldingFinger(Point(10, 20))), owner = OWNER_1)!!

        assertEquals("The held pointer should belong to the gesture holding it", OWNER_1, gesture.heldPointer!!.owner)
    }

    @Test
    fun carry_keepsTheOwnerOfTheHeldPointer() {
        val heldPointer = builder.buildGesture(newGesture(newHoldingFinger(Point(10, 20))), owner = OWNER_1)!!
            .heldPointer!!

        val gesture = builder.buildGesture(newGesture(newTapFinger(30, 40)), heldPointer, owner = OWNER_2)!!

        assertEquals("A carried pointer should keep its owner", OWNER_1, gesture.heldPointer!!.owner)
    }

    @Test
    fun newHold_takesTheOwnershipOver() {
        val heldPointer = builder.buildGesture(newGesture(newHoldingFinger(Point(10, 20))), owner = OWNER_1)!!
            .heldPointer!!

        val gesture = builder.buildGesture(newGesture(newHoldingFinger(Point(30, 40))), heldPointer, owner = OWNER_2)!!

        assertEquals("The gesture taking the hold over should own the pointer", OWNER_2, gesture.heldPointer!!.owner)
    }

    private companion object {
        const val OWNER_1 = "owner1"
        const val OWNER_2 = "owner2"
    }
}
