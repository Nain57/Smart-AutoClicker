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
package com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
import com.buzbuz.smartautoclicker.core.processing.data.processor.actions.FingerResolver
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.COUNTER_NAME
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.COUNTER_VALUE
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.coordinates
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.move
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newCondition
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newConditionsResults
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newGesture
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newProcessingState
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newScreenEvent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newTapFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newTriggerEvent
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.press
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the resolution of the gesture positions from the detection results and the counters by [FingerResolver]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FingerResolverTests {

    private lateinit var resolver: FingerResolver

    @Before
    fun setUp() {
        resolver = FingerResolver(newProcessingState(), AndroidGestureBuilder.createNewBuilder(random = null))
    }

    private fun resolve(gesture: Gesture, event: Event = newScreenEvent(), results: ConditionsResults? = null): List<ResolvedFinger> =
        resolver.resolve(gesture, event, results)

    private fun conditionPosition(conditionId: Long? = null): Position.Condition =
        Position.Condition(conditionId?.let { Identifier(databaseId = it) })

    private fun assertNoFinger(fingers: List<ResolvedFinger>) {
        assertTrue("No finger should have been resolved", fingers.isEmpty())
    }

    /** All the positions the finger goes through, from its press to its release. */
    private fun assertPositions(fingers: List<ResolvedFinger>, vararg positions: Point) {
        val finger = fingers.single()
        val actual = listOf(finger.segments.first().from) +
                finger.segments.filter { segment -> segment.from != segment.to }.map { segment -> segment.to }
        assertEquals("Invalid finger positions", positions.toList(), actual)
    }

    @Test
    fun resolve_noFingers_returnsEmpty() {
        assertNoFinger(resolve(newGesture()))
    }

    @Test
    fun resolve_incompleteFinger_skipsFinger() {
        assertNoFinger(resolve(newGesture(newFinger(0L, press(10, 20)))))
    }

    @Test
    fun resolve_fingerPressingTwice_skipsFinger() {
        assertNoFinger(resolve(newGesture(newFinger(0L, press(10, 20), press(30, 40), release()))))
    }

    @Test
    fun resolve_oneFingerUnresolvable_skipsOnlyThatFinger() {
        val gesture = newGesture(
            newFinger(0L, press(10, 20), press(30, 40), release()),
            newTapFinger(50, 60),
        )

        val fingers = resolve(gesture)

        assertPositions(fingers, Point(50, 60))
        assertEquals("Skipped finger should not shift the indexes", 1, fingers.single().index)
    }

    @Test
    fun resolve_literalCoordinates_usesThem() {
        assertPositions(resolve(newGesture(newTapFinger(10, 20))), Point(10, 20))
    }

    @Test
    fun resolve_offsetIsAdded() {
        val gesture = newGesture(newFinger(0L, press(coordinates(10, 20), coordinates(5, -5)), release()))

        assertPositions(resolve(gesture), Point(15, 15))
    }

    @Test
    fun resolve_counterCoordinate_usesCounterValue() {
        val position = Position.Coordinates(
            x = CoordinatesValue.Counter(COUNTER_NAME),
            y = CoordinatesValue.Literal(20),
        )
        val gesture = newGesture(newFinger(0L, press(position), release()))

        assertPositions(resolve(gesture), Point(COUNTER_VALUE, 20))
    }

    @Test
    fun resolve_unknownCounter_skipsFinger() {
        val position = Position.Coordinates(
            x = CoordinatesValue.Counter("unknown"),
            y = CoordinatesValue.Literal(20),
        )

        assertNoFinger(resolve(newGesture(newFinger(0L, press(position), release()))))
    }

    @Test
    fun resolve_unresolvableMove_skipsFinger() {
        val position = Position.Coordinates(
            x = CoordinatesValue.Counter("unknown"),
            y = CoordinatesValue.Literal(20),
        )
        val gesture = newGesture(newFinger(0L, press(10, 20), move(position, durationMs = 100L), release()))

        assertNoFinger(resolve(gesture))
    }

    @Test
    fun resolve_movePositions_areAllResolved() {
        val gesture = newGesture(newFinger(0L, press(0, 0), move(100, 0, 200L), move(100, 100, 300L), release()))

        assertPositions(resolve(gesture), Point(0, 0), Point(100, 0), Point(100, 100))
    }

    @Test
    fun resolve_conditionOrOperator_usesFirstDetectedCondition() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val results = newConditionsResults(condition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertPositions(resolve(gesture, event, results), Point(15, 25))
    }

    @Test
    fun resolve_conditionOrOperator_noResults_skipsFinger() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNoFinger(resolve(gesture, event, null))
        assertNoFinger(resolve(gesture, event, newConditionsResults()))
    }

    @Test
    fun resolve_conditionAndOperator_usesSelectedCondition() {
        val selectedCondition = newCondition(42L)
        val otherCondition = newCondition(43L)
        val event = newScreenEvent(AND, listOf(otherCondition, selectedCondition))
        val results = newConditionsResults(otherCondition to Point(1, 1), selectedCondition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition(42L)), release()))

        assertPositions(resolve(gesture, event, results), Point(15, 25))
    }

    @Test
    fun resolve_conditionAndOperator_conditionNotProcessed_skipsFinger() {
        val condition = newCondition(42L)
        val event = newScreenEvent(AND, listOf(condition))
        val gesture = newGesture(newFinger(0L, press(conditionPosition(42L)), release()))

        assertNoFinger(resolve(gesture, event, newConditionsResults()))
    }

    @Test
    fun resolve_conditionAndOperator_noConditionSelected_skipsFinger() {
        val condition = newCondition(42L)
        val event = newScreenEvent(AND, listOf(condition))
        val results = newConditionsResults(condition to Point(15, 25))
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNoFinger(resolve(gesture, event, results))
    }

    @Test
    fun resolve_conditionNotDetected_skipsFinger() {
        val condition = newCondition(42L)
        val event = newScreenEvent(OR, listOf(condition))
        val results = newConditionsResults(condition to null)
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNoFinger(resolve(gesture, event, results))
    }

    @Test
    fun resolve_conditionOnTriggerEvent_skipsFinger() {
        val gesture = newGesture(newFinger(0L, press(conditionPosition()), release()))

        assertNoFinger(resolve(gesture, newTriggerEvent(), null))
    }
}
