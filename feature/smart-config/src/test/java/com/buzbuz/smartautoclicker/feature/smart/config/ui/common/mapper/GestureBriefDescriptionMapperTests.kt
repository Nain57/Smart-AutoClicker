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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureAnimationSegment
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypointCondition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests the conversion of a smart [Gesture] into the [GestureDescription] displayed by the gesture renderer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureBriefDescriptionMapperTests {

    private companion object {
        val SCREEN_SIZE = Point(1000, 2000)
        val SCREEN_CENTER = PointF(500f, 1000f)

        val EVENT_ID = Identifier(databaseId = 1L)
        val SCENARIO_ID = Identifier(databaseId = 2L)

        val CONDITION_1_ID = Identifier(databaseId = 10L)
        val CONDITION_2_ID = Identifier(databaseId = 11L)

        val MOTION_1_ID = Identifier(databaseId = 100L)
        val MOTION_2_ID = Identifier(databaseId = 101L)
        val MOTION_3_ID = Identifier(databaseId = 102L)
        val MOTION_4_ID = Identifier(databaseId = 103L)

        const val COUNTER_X = "counterX"
        const val COUNTER_Y = "counterY"
    }

    // ---- Literal coordinates & structure ----

    @Test
    fun literalCoordinates_mappedAsIs() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, literal(12, 34)), release(MOTION_2_ID)),
        ).map()

        assertEquals(PointF(12f, 34f), description.waypoint(0, 0).position)
    }

    @Test
    fun pressMoveWaitRelease_waypointsAndSegments() {
        val description = gestureOf(
            fingerOf(
                press(MOTION_1_ID, literal(0, 0)),
                move(MOTION_2_ID, literal(100, 200), durationMs = 300),
                GestureFingerMotion.Wait(MOTION_3_ID, durationMs = 50),
                release(MOTION_4_ID),
            ),
        ).map()

        val finger = description.fingers.single()
        assertEquals(2, finger.waypoints.size)
        assertEquals(listOf(MOTION_1_ID, MOTION_2_ID), finger.waypoints.map { it.motionId })
        assertEquals(
            listOf(
                GestureAnimationSegment.Move(fromWpIdx = 0, toWpIdx = 1, durationMs = 300),
                GestureAnimationSegment.Wait(wpIdx = 1, durationMs = 50),
            ),
            finger.animationSegments,
        )
        assertEquals(350L, finger.durationMs)
    }

    @Test
    fun totalDuration_isLatestFingerEnd() {
        val description = gestureOf(
            fingerOf(
                press(MOTION_1_ID, literal(0, 0)),
                move(MOTION_2_ID, literal(1, 1), durationMs = 100),
                release(MOTION_3_ID),
                startTimeMs = 0,
            ),
            fingerOf(
                press(MOTION_4_ID, literal(0, 0)),
                move(Identifier(databaseId = 104L), literal(1, 1), durationMs = 100),
                release(Identifier(databaseId = 105L)),
                startTimeMs = 250,
            ),
        ).map()

        assertEquals(250L, description.fingers[1].startTimeMs)
        assertEquals(350L, description.totalDurationMs)
    }

    @Test
    fun singleEditableMotionId_onlyThisWaypointIsEditable() {
        val description = gestureOf(
            fingerOf(
                press(MOTION_1_ID, literal(0, 0)),
                move(MOTION_2_ID, literal(1, 1), durationMs = 100),
                release(MOTION_3_ID),
            ),
        ).map(singleEditableMotionId = MOTION_2_ID)

        assertFalse(description.waypoint(0, 0).isEditable)
        assertTrue(description.waypoint(0, 1).isEditable)
    }

    @Test
    fun noSingleEditableMotionId_allWaypointsAreEditable() {
        val description = gestureOf(
            fingerOf(
                press(MOTION_1_ID, literal(0, 0)),
                move(MOTION_2_ID, literal(1, 1), durationMs = 100),
                release(MOTION_3_ID),
            ),
        ).map()

        assertTrue(description.fingers.single().waypoints.all { it.isEditable })
    }

    // ---- Hold ----

    @Test
    fun releaseHoldUntilUnfulfilled_fingerIsHeld() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, literal(0, 0)), release(MOTION_2_ID, hold = true)),
        ).map()

        assertTrue(description.fingers.single().isHeld)
    }

    @Test
    fun releaseNow_fingerIsNotHeld() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, literal(0, 0)), release(MOTION_2_ID, hold = false)),
        ).map()

        assertFalse(description.fingers.single().isHeld)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun releaseHoldUntilUnfulfilled_beforeAndroidO_fingerIsNotHeld() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, literal(0, 0)), release(MOTION_2_ID, hold = true)),
        ).map()

        assertFalse(description.fingers.single().isHeld)
    }

    // ---- Counter coordinates ----

    @Test
    fun counterCoordinates_useCounterStartingValues() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, counters(COUNTER_X, COUNTER_Y)), release(MOTION_2_ID)),
        ).map(counters = listOf(counter(COUNTER_X, 150.0), counter(COUNTER_Y, 320.0)))

        assertEquals(PointF(150f, 320f), description.waypoint(0, 0).position)
    }

    @Test
    fun counterCoordinates_counterWithoutConfig_startsAtZero() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, counters(COUNTER_X, COUNTER_Y)), release(MOTION_2_ID)),
        ).map(counters = listOf(counter(COUNTER_X, 150.0)))

        assertEquals(PointF(150f, 0f), description.waypoint(0, 0).position)
    }

    @Test
    fun mixedLiteralAndCounterCoordinates() {
        val position = GesturePosition.Coordinates(
            x = GestureCoordinatesValue.Literal(42),
            y = GestureCoordinatesValue.Counter(COUNTER_Y),
        )
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, position), release(MOTION_2_ID)),
        ).map(counters = listOf(counter(COUNTER_Y, 77.0)))

        assertEquals(PointF(42f, 77f), description.waypoint(0, 0).position)
    }

    // ---- Condition positions: area per condition type ----

    @Test
    fun conditionPosition_imageExact_captureAreaCenter() {
        val condition = imageCondition(
            CONDITION_1_ID,
            area = Rect(100, 100, 200, 300),
            detectionType = EXACT,
            detectionArea = Rect(0, 0, 1000, 1000),
        )

        assertEquals(PointF(150f, 200f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_imageInArea_detectionAreaCenter() {
        val condition = imageCondition(
            CONDITION_1_ID,
            area = Rect(100, 100, 200, 300),
            detectionType = IN_AREA,
            detectionArea = Rect(400, 600, 600, 1000),
        )

        assertEquals(PointF(500f, 800f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_imageInAreaWithoutDetectionArea_captureAreaCenter() {
        val condition = imageCondition(
            CONDITION_1_ID,
            area = Rect(100, 100, 200, 300),
            detectionType = IN_AREA,
            detectionArea = null,
        )

        assertEquals(PointF(150f, 200f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_imageWholeScreen_screenCenter() {
        val condition = imageCondition(
            CONDITION_1_ID,
            area = Rect(100, 100, 200, 300),
            detectionType = WHOLE_SCREEN,
        )

        assertEquals(SCREEN_CENTER, mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_color_detectionAreaCenter() {
        val condition = ScreenCondition.Color(
            id = CONDITION_1_ID, eventId = EVENT_ID, name = "color", threshold = 0, shouldBeDetected = true,
            priority = 0, color = 0, detectionArea = Rect(10, 20, 30, 60),
        )

        assertEquals(PointF(20f, 40f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_number_detectionAreaCenter() {
        val condition = ScreenCondition.Number(
            id = CONDITION_1_ID, eventId = EVENT_ID, name = "number", threshold = 0, priority = 0,
            detectionArea = Rect(0, 0, 100, 50), comparisonOperation = ComparisonOperation.EQUALS,
            counterValue = CounterOperationValue.Number(0.0),
        )

        assertEquals(PointF(50f, 25f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    @Test
    fun conditionPosition_text_detectionAreaCenter() {
        val condition = ScreenCondition.Text(
            id = CONDITION_1_ID, eventId = EVENT_ID, name = "text", threshold = 0, shouldBeDetected = true,
            priority = 0, text = "hello", detectionArea = Rect(200, 400, 300, 500), alphabet = OCRAlphabet.LATIN,
        )

        assertEquals(PointF(250f, 450f), mapConditionPosition(CONDITION_1_ID, listOf(condition)))
    }

    // ---- Condition positions: condition selection ----

    @Test
    fun conditionPosition_and_selectedCondition() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))

        assertEquals(PointF(450f, 850f), mapConditionPosition(CONDITION_2_ID, conditions, AND))
    }

    @Test
    fun conditionPosition_and_noSelectedCondition_firstCondition() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))

        assertEquals(PointF(50f, 50f), mapConditionPosition(null, conditions, AND))
    }

    @Test
    fun conditionPosition_and_selectedConditionNotFound_screenCenter() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0))

        assertEquals(SCREEN_CENTER, mapConditionPosition(CONDITION_2_ID, conditions, AND))
    }

    @Test
    fun conditionPosition_or_selectedConditionIgnored_firstCondition() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))

        assertEquals(PointF(50f, 50f), mapConditionPosition(CONDITION_2_ID, conditions, OR))
    }

    @Test
    fun conditionPosition_or_noSelectedCondition_firstCondition() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))

        assertEquals(PointF(50f, 50f), mapConditionPosition(null, conditions, OR))
    }

    @Test
    fun conditionPosition_noConditions_screenCenter() {
        assertEquals(SCREEN_CENTER, mapConditionPosition(null, emptyList(), OR))
    }

    // ---- Condition displays ----

    @Test
    fun conditionPosition_displayOfResolvedConditionAttached() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))
        val displays = mapOf(
            CONDITION_1_ID to GestureWaypointCondition(label = "first"),
            CONDITION_2_ID to GestureWaypointCondition(label = "second"),
        )

        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, GesturePosition.Condition(CONDITION_2_ID)), release(MOTION_2_ID)),
        ).map(conditions = conditions, conditionDisplays = displays)

        assertEquals(GestureWaypointCondition(label = "second"), description.waypoint(0, 0).condition)
    }

    @Test
    fun conditionPosition_or_displayOfFirstConditionAttached() {
        val conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0), exactImageAt(CONDITION_2_ID, 400, 800))
        val displays = mapOf(
            CONDITION_1_ID to GestureWaypointCondition(label = "first"),
            CONDITION_2_ID to GestureWaypointCondition(label = "second"),
        )

        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, GesturePosition.Condition(CONDITION_2_ID)), release(MOTION_2_ID)),
        ).map(conditionOperator = OR, conditions = conditions, conditionDisplays = displays)

        assertEquals(GestureWaypointCondition(label = "first"), description.waypoint(0, 0).condition)
    }

    @Test
    fun conditionPosition_conditionWithoutDisplay_noCondition() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, GesturePosition.Condition(CONDITION_1_ID)), release(MOTION_2_ID)),
        ).map(conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0)), conditionDisplays = emptyMap())

        assertNull(description.waypoint(0, 0).condition)
    }

    @Test
    fun conditionPosition_conditionNotFound_noCondition() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, GesturePosition.Condition(CONDITION_2_ID)), release(MOTION_2_ID)),
        ).map(
            conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0)),
            conditionDisplays = mapOf(CONDITION_1_ID to GestureWaypointCondition(label = "first")),
        )

        assertNull(description.waypoint(0, 0).condition)
    }

    @Test
    fun coordinatesPosition_noCondition() {
        val description = gestureOf(
            fingerOf(press(MOTION_1_ID, literal(0, 0)), release(MOTION_2_ID)),
        ).map(
            conditions = listOf(exactImageAt(CONDITION_1_ID, 0, 0)),
            conditionDisplays = mapOf(CONDITION_1_ID to GestureWaypointCondition(label = "first")),
        )

        assertNull(description.waypoint(0, 0).condition)
    }

    // ---- Helpers ----

    private fun Gesture.map(
        conditionOperator: Int = AND,
        conditions: List<ScreenCondition> = emptyList(),
        counters: List<Counter> = emptyList(),
        singleEditableMotionId: Identifier? = null,
        conditionDisplays: Map<Identifier, GestureWaypointCondition> = emptyMap(),
    ): GestureDescription =
        toGestureDescription(conditionOperator, conditions, counters, SCREEN_SIZE, singleEditableMotionId, conditionDisplays)

    private fun mapConditionPosition(
        conditionId: Identifier?,
        conditions: List<ScreenCondition>,
        conditionOperator: Int = AND,
    ): PointF =
        gestureOf(fingerOf(press(MOTION_1_ID, GesturePosition.Condition(conditionId)), release(MOTION_2_ID)))
            .map(conditionOperator = conditionOperator, conditions = conditions)
            .waypoint(0, 0)
            .position

    private fun GestureDescription.waypoint(fingerIdx: Int, waypointIdx: Int) =
        fingers[fingerIdx].waypoints[waypointIdx]

    private fun gestureOf(vararg fingers: GestureFinger) = Gesture(
        id = Identifier(databaseId = 1000L),
        eventId = EVENT_ID,
        name = "gesture",
        priority = 0,
        fingers = fingers.toList(),
    )

    private fun fingerOf(vararg motions: GestureFingerMotion, startTimeMs: Long = 0L) = GestureFinger(
        id = Identifier(databaseId = 2000L + startTimeMs),
        startTimeMs = startTimeMs,
        motions = motions.toList(),
    )

    private fun press(id: Identifier, position: GesturePosition) =
        GestureFingerMotion.Press(id = id, position = position, offset = literal(0, 0))

    private fun move(id: Identifier, position: GesturePosition, durationMs: Long) =
        GestureFingerMotion.Move(id = id, position = position, offset = literal(0, 0), durationMs = durationMs)

    private fun release(id: Identifier, hold: Boolean = false) =
        GestureFingerMotion.Release(id = id, holdUntilUnfulfilled = hold)

    private fun literal(x: Int, y: Int) = GesturePosition.Coordinates(
        x = GestureCoordinatesValue.Literal(x),
        y = GestureCoordinatesValue.Literal(y),
    )

    private fun counters(xCounter: String, yCounter: String) = GesturePosition.Coordinates(
        x = GestureCoordinatesValue.Counter(xCounter),
        y = GestureCoordinatesValue.Counter(yCounter),
    )

    private fun counter(name: String, startValue: Double) =
        Counter(counterName = name, defaultValue = startValue, scenarioId = SCENARIO_ID)

    private fun imageCondition(id: Identifier, area: Rect, detectionType: Int, detectionArea: Rect? = null) =
        ScreenCondition.Image(
            id = id, eventId = EVENT_ID, name = "image", threshold = 0, shouldBeDetected = true, priority = 0,
            path = "path", area = area, detectionType = detectionType, detectionArea = detectionArea,
        )

    /** An EXACT image condition of 100x100 px with its top left corner at [left], [top]. */
    private fun exactImageAt(id: Identifier, left: Int, top: Int) =
        imageCondition(id, area = Rect(left, top, left + 100, top + 100), detectionType = EXACT)
}
