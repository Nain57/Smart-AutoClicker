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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilderCompat24
import com.buzbuz.smartautoclicker.core.processing.data.processor.gesture.AndroidGestureBuilderCurrent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.COUNTER_NAME
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.COUNTER_VALUE
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.TAP_DURATION_MS
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.coordinates
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.move
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newCondition
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newConditionsResults
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newGesture
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newProcessingState
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newScreenEvent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.newTriggerEvent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.press
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.release
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.GestureBuilderTestData.wait

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import kotlin.random.Random

/**
 * Tests the behaviour shared by all [AndroidGestureBuilder] implementations: finger validation, position
 * resolution, randomization and error handling.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidGestureBuilderTests {

    private companion object {
        const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
        const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L
    }

    private fun newBuilder(random: Random? = null): AndroidGestureBuilder =
        AndroidGestureBuilderCurrent(newProcessingState(), random)

    private fun conditionPosition(conditionId: Long? = null): GesturePosition.Condition =
        GesturePosition.Condition(conditionId?.let { Identifier(databaseId = it) })

    @Test
    fun createNewBuilder_currentSdk_returnsCurrent() {
        assertTrue(AndroidGestureBuilder.createNewBuilder(newProcessingState(), null) is AndroidGestureBuilderCurrent)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun createNewBuilder_belowOreo_returnsCompat24() {
        assertTrue(AndroidGestureBuilder.createNewBuilder(newProcessingState(), null) is AndroidGestureBuilderCompat24)
    }

    @Test
    fun build_noFingers_returnsNull() {
        assertNull(newBuilder().build(newGesture(), newScreenEvent(), null))
    }

    @Test
    fun build_incompleteFinger_returnsNull() {
        val gesture = newGesture(newFinger(0L, press(10, 20)))

        assertNull(newBuilder().build(gesture, newScreenEvent(), null))
    }

    @Test
    fun build_fingerPressingTwice_returnsNull() {
        val gesture = newGesture(newFinger(0L, press(10, 20), press(30, 40), release()))

        assertNull(newBuilder().build(gesture, newScreenEvent(), null))
    }

    @Test
    fun build_fingerReleasingTwice_returnsNull() {
        val gesture = newGesture(newFinger(0L, press(10, 20), release(), release()))

        assertNull(newBuilder().build(gesture, newScreenEvent(), null))
    }

    @Test
    fun build_oneFingerUnresolvable_skipsOnlyThatFinger() {
        val gesture = newGesture(
            newFinger(0L, press(10, 20), press(30, 40), release()),
            newTapFinger(50, 60),
        )

        val chains = newBuilder().build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        val description = chains!!.first().single()
        assertStrokesCount(1, description)
        assertHoldStroke(description.getStroke(0), 0L, TAP_DURATION_MS, Point(50, 60))
    }

    @Test
    fun build_negativeStartTime_coercedToZero() {
        val gesture = newGesture(newTapFinger(10, 20, startTimeMs = -50L))

        val chains = newBuilder().build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(10, 20))
    }

    @Test
    fun build_tooManyStrokes_returnsNull() {
        val fingers = Array(GestureDescription.getMaxStrokeCount() + 1) { index -> newTapFinger(index * 10, index * 10) }

        assertNull(newBuilder().build(newGesture(*fingers), newScreenEvent(), null))
    }

    @Test
    fun resolvePosition_offsetIsAdded() {
        val gesture = newGesture(newFinger(0L, press(coordinates(10, 20), coordinates(5, -5)), release()))

        val chains = newBuilder().build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(15, 15))
    }

    @Test
    fun resolvePosition_negativeCoordinates_clampedToZero() {
        val gesture = newGesture(newTapFinger(-10, -20))

        val chains = newBuilder().build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(0, 0))
    }

    @Test
    fun resolvePosition_counterCoordinate_usesCounterValue() {
        val position = GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Counter(COUNTER_NAME),
            y = GestureCoordinatesValue.Literal(20),
        )
        val gesture = newGesture(newFinger(0L, press(position), release()))

        val chains = newBuilder().build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(COUNTER_VALUE, 20))
    }

    @Test
    fun resolvePosition_unknownCounter_returnsNull() {
        val position = GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Counter("unknown"),
            y = GestureCoordinatesValue.Literal(20),
        )
        val gesture = newGesture(newFinger(0L, press(position), release()))

        assertNull(newBuilder().build(gesture, newScreenEvent(), null))
    }

    @Test
    fun resolvePosition_unresolvableMove_returnsNull() {
        val position = GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Counter("unknown"),
            y = GestureCoordinatesValue.Literal(20),
        )
        val gesture = newGesture(newFinger(0L, press(10, 20), move(position, durationMs = 100L), release()))

        assertNull(newBuilder().build(gesture, newScreenEvent(), null))
    }

    @Test
    fun resolvePosition_conditionOrOperator_usesFirstDetectedCondition() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val results = newConditionsResults(condition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        val chains = newBuilder().build(gesture, event, results)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(15, 25))
    }

    @Test
    fun resolvePosition_conditionOrOperator_noResults_returnsNull() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNull(newBuilder().build(gesture, event, null))
        assertNull(newBuilder().build(gesture, event, newConditionsResults()))
    }

    @Test
    fun resolvePosition_conditionAndOperator_usesSelectedCondition() {
        val selectedCondition = newCondition(42L)
        val otherCondition = newCondition(43L)
        val event = newScreenEvent(AND, listOf(otherCondition, selectedCondition))
        val results = newConditionsResults(otherCondition to Point(1, 1), selectedCondition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition(42L)), release()))

        val chains = newBuilder().build(gesture, event, results)

        assertChainsCount(1, chains)
        assertHoldStroke(chains!!.first().single().getStroke(0), 0L, TAP_DURATION_MS, Point(15, 25))
    }

    @Test
    fun resolvePosition_conditionAndOperator_conditionNotProcessed_returnsNull() {
        val condition = newCondition(42L)
        val event = newScreenEvent(AND, listOf(condition))
        val gesture = newGesture(newFinger(0L, press(conditionPosition(42L)), release()))

        assertNull(newBuilder().build(gesture, event, newConditionsResults()))
    }

    @Test
    fun resolvePosition_conditionAndOperator_noConditionSelected_returnsNull() {
        val condition = newCondition(42L)
        val event = newScreenEvent(AND, listOf(condition))
        val results = newConditionsResults(condition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNull(newBuilder().build(gesture, event, results))
    }

    @Test
    fun resolvePosition_conditionNotDetected_returnsNull() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val results = newConditionsResults(condition to null)
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNull(newBuilder().build(gesture, event, results))
    }

    @Test
    fun resolvePosition_conditionOnTriggerEvent_returnsNull() {
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNull(newBuilder().build(gesture, newTriggerEvent(), null))
    }

    @Test
    fun randomize_swipe_positionsAndDurationStayWithinOffsets() {
        val gesture = newGesture(newFinger(0L, press(100, 200), move(300, 400, 500L), release()))

        val chains = newBuilder(Random(42)).build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        val stroke = chains!!.first().single().getStroke(0)
        assertTrue(stroke.duration in 500L - RANDOMIZATION_DURATION_MAX_OFFSET_MS..500L + RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        assertPointNear(Point(100, 200), stroke.points.first())
        assertPointNear(Point(300, 400), stroke.points.last())
    }

    @Test
    fun randomize_negativeCoordinates_clampedToZero() {
        val gesture = newGesture(newTapFinger(0, 0))

        val chains = newBuilder(Random(42)).build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        val position = chains!!.first().single().getStroke(0).points.single()
        assertTrue("Position should be clamped to the screen", position.x >= 0 && position.y >= 0)
    }

    @Test
    fun randomize_continuedStroke_startsWhereThePreviousOneEnded() {
        val gesture = newGesture(newFinger(0L, press(100, 200), move(300, 400, 500L), wait(300L), release()))

        val chains = newBuilder(Random(42)).build(gesture, newScreenEvent(), null)

        assertChainsCount(1, chains)
        val chain = chains!!.first()
        assertGesturesCount(2, chain)
        val moveStroke = chain[0].getStroke(0)
        val holdStroke = chain[1].getStroke(0)
        assertEquals("Continuation should start at the randomized end of the move", moveStroke.points.last(), holdStroke.points.single())
        assertEquals(moveStroke.strokeId, holdStroke.continuedStrokeId)
    }

    private fun assertPointNear(expected: Point, actual: Point) {
        assertTrue(
            "Point $actual should be within $RANDOMIZATION_POSITION_MAX_OFFSET_PX px of $expected",
            expected.x - RANDOMIZATION_POSITION_MAX_OFFSET_PX <= actual.x
                    && actual.x <= expected.x + RANDOMIZATION_POSITION_MAX_OFFSET_PX
                    && expected.y - RANDOMIZATION_POSITION_MAX_OFFSET_PX <= actual.y
                    && actual.y <= expected.y + RANDOMIZATION_POSITION_MAX_OFFSET_PX,
        )
    }
}
