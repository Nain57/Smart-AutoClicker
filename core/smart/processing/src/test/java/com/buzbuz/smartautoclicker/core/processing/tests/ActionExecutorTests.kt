/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.tests

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.actions.ActionExecutor
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import com.buzbuz.smartautoclicker.core.processing.utils.anyNotNull
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Test the [ActionExecutor] class. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionExecutorTests {

    private companion object {
        private val TEST_EVENT_ID = Identifier(databaseId = 42L)
        private const val TEST_NAME = "Action name"
        private const val TEST_DURATION = 25L
        private const val TEST_X1 = 12
        private const val TEST_Y1 = 88
        private const val TEST_OFFSET_X = 5
        private const val TEST_OFFSET_Y = -3
        private const val TEST_COUNTER_NAME = "counter"

        fun getNewDefaultEvent(operator: Int = OR, conditions: List<ScreenCondition.Image> = emptyList(), actions: List<Action> = emptyList()) =
            ScreenEvent(TEST_EVENT_ID, Identifier(databaseId = 12L), "Name", operator, actions, conditions, true, 0, cooldownMs = 0, keepDetecting = false)

        fun getNewDefaultClickUserPos(id: Long, duration: Long = TEST_DURATION) =
            Click(Identifier(databaseId = id), TEST_EVENT_ID, TEST_NAME, 0, duration,
                GesturePosition.Coordinates(
                    GestureCoordinatesValue.Literal(TEST_X1),
                    GestureCoordinatesValue.Literal(TEST_Y1),
                ))
        fun getNewDefaultClickCondition(id: Long, conditionId: Long? = null) =
            Click(Identifier(databaseId = id), TEST_EVENT_ID, TEST_NAME, 1, TEST_DURATION,
                GesturePosition.Condition(conditionId?.let { Identifier(databaseId = it) }))
        fun getNewDefaultPause(id: Long) =
            Pause(Identifier(databaseId = id), TEST_EVENT_ID, TEST_NAME, 3, TEST_DURATION)

        fun getNewDefaultCondition(id: Long) =
            ScreenCondition.Image(Identifier(databaseId = id), TEST_EVENT_ID, TEST_NAME, 0, true, 10, "path", Rect(), EXACT, null)
    }

    @Mock private lateinit var mockAndroidExecutor: AndroidActionExecutor
    @Mock private lateinit var mockProcessingState: ProcessingState

    private lateinit var actionExecutor: ActionExecutor

    /** A click is a single finger pressing and lifting, so it is built as one chain holding one gesture. */
    private fun BuiltGesture.toClickGesture(): GestureDescription {
        assertEquals("Click should be built as a single chain", 1, chains.size)
        assertEquals("Click chain should contains only one gesture", 1, chains.first().size)
        return chains.first().first()
    }

    private fun assertActionGesture(builtGesture: BuiltGesture) {
        val gesture = builtGesture.toClickGesture()

        assertEquals("Gesture should contains only one stroke", 1, gesture.strokeCount)
        gesture.getStroke(0).let { stroke ->
            assertEquals("Gesture duration is invalid", TEST_DURATION, stroke.duration)
            assertEquals("Gesture start time is invalid", 0, stroke.startTime)
        }
    }

    private fun assertClickPosition(builtGesture: BuiltGesture, expectedX: Int, expectedY: Int) {
        val point = shadowOf(builtGesture.toClickGesture().getStroke(0).path).points.first()
        assertEquals("Click x position is invalid", expectedX.toFloat(), point.x)
        assertEquals("Click y position is invalid", expectedY.toFloat(), point.y)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())

        actionExecutor = ActionExecutor(mockAndroidExecutor, mockProcessingState, randomize = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noActions() = runTest {
        val event = getNewDefaultEvent()
        actionExecutor.executeActions(event, ConditionsResults())
        verify(mockAndroidExecutor, never()).dispatchBuiltGesture(anyNotNull())
    }

    @Test
    fun execute_oneClick_notOnCondition() = runTest {
        val clickAction = getNewDefaultClickUserPos(1)
        val event = getNewDefaultEvent(actions = listOf(clickAction))

        actionExecutor.executeActions(event, ConditionsResults())

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_oneClick_notOnCondition_withOffset() = runTest {
        val clickAction = getNewDefaultClickUserPos(1).copy(
            offset = GesturePosition.Coordinates(
                GestureCoordinatesValue.Literal(TEST_OFFSET_X),
                GestureCoordinatesValue.Counter(TEST_COUNTER_NAME),
            ),
        )
        mockWhen(mockProcessingState.getCounterValue(TEST_COUNTER_NAME)).thenReturn(TEST_OFFSET_Y.toDouble())
        val event = getNewDefaultEvent(actions = listOf(clickAction))

        actionExecutor.executeActions(event, ConditionsResults())

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
        assertClickPosition(gestureCaptor.lastValue, TEST_X1 + TEST_OFFSET_X, TEST_Y1 + TEST_OFFSET_Y)
    }

    @Test
    fun execute_oneClick_notOnCondition_withUnresolvableOffset() = runTest {
        val clickAction = getNewDefaultClickUserPos(1).copy(
            offset = GesturePosition.Coordinates(
                GestureCoordinatesValue.Counter(TEST_COUNTER_NAME),
                GestureCoordinatesValue.Literal(TEST_OFFSET_Y),
            ),
        )
        mockWhen(mockProcessingState.getCounterValue(TEST_COUNTER_NAME)).thenReturn(null)
        val event = getNewDefaultEvent(actions = listOf(clickAction))

        actionExecutor.executeActions(event, ConditionsResults())

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertClickPosition(gestureCaptor.lastValue, TEST_X1, TEST_Y1 + TEST_OFFSET_Y)
    }

    @Test
    fun execute_oneClick_onCondition_withOffset() = runTest {
        val condition = getNewDefaultCondition(42L)
        val clickAction = getNewDefaultClickCondition(1, condition.id.databaseId).copy(
            offset = GesturePosition.Coordinates(
                GestureCoordinatesValue.Literal(TEST_OFFSET_X),
                GestureCoordinatesValue.Literal(TEST_OFFSET_Y),
            ),
        )
        val event = getNewDefaultEvent(AND, conditions = listOf(condition), actions = listOf(clickAction))
        val results = ConditionsResults()
        results.addResult(
            conditionId = condition.getDatabaseId(),
            result = ProcessedConditionResult.Screen(
                isFulfilled = true,
                haveBeenDetected = true,
                condition = condition,
                position = Point(15, 15),
                size = Point(10, 10),
                confidenceRate = 100.0
            )
        )

        actionExecutor.executeActions(event, results)

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
        assertClickPosition(gestureCaptor.lastValue, 15 + TEST_OFFSET_X, 15 + TEST_OFFSET_Y)
    }

    @Test
    fun execute_oneClick_onCondition_or() = runTest {
        val clickAction = getNewDefaultClickUserPos(1)

        val condition = getNewDefaultCondition(42L)
        val event = getNewDefaultEvent(
            OR,
            conditions = listOf(condition),
            actions = listOf(clickAction),
        )

        val results = ConditionsResults()
        results.addResult(
            conditionId = condition.getDatabaseId(),
            result = ProcessedConditionResult.Screen(
                isFulfilled = true,
                haveBeenDetected = true,
                condition = condition,
                position = Point(15, 15),
                size = Point(10, 10),
                confidenceRate = 100.0
            )
        )

        actionExecutor.executeActions(event, results)

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_oneClick_onCondition_and() = runTest {
        val conditionValid = getNewDefaultCondition(42L)
        val conditionOther = getNewDefaultCondition(75L)
        val clickAction = getNewDefaultClickCondition(1, conditionValid.id.databaseId)

        val event = getNewDefaultEvent(
            AND,
            conditions = listOf(conditionValid, conditionOther),
            actions = listOf(clickAction),
        )
        val results = ConditionsResults()
        results.addResult(
            conditionId = conditionValid.getDatabaseId(),
            result = ProcessedConditionResult.Screen(
                isFulfilled = true,
                haveBeenDetected = true,
                condition = conditionValid,
                position = Point(15, 15),
                size = Point(10, 10),
                confidenceRate = 100.0
            )
        )
        results.addResult(
            conditionId = conditionOther.getDatabaseId(),
            result = ProcessedConditionResult.Screen(
                isFulfilled = true,
                haveBeenDetected = false,
                condition = conditionOther,
                position = Point(45, 45),
                size = Point(10, 10),
                confidenceRate = 98.0
            )
        )

        actionExecutor.executeActions(event, results)

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_onePause() = runTest {
        val pause = getNewDefaultPause(1)

        // Execute the pause. As the handler is waiting to the finish the pause, we should stays in EXECUTING
        actionExecutor.executeActions(
            event = getNewDefaultEvent(actions = listOf(pause)),
            results = ConditionsResults(),
        )

        // Only a pause, there should be no gestures
        verify(mockAndroidExecutor, never()).dispatchBuiltGesture(anyNotNull())
    }

    @Test
    fun execute_mixed() = runTest {
        val click = getNewDefaultClickUserPos(1)
        val pause = getNewDefaultPause(2)
        val otherClick = getNewDefaultClickUserPos(3)
        val gestureCaptor = argumentCaptor<BuiltGesture>()

        // Execute the actions.
        actionExecutor.executeActions(
            event = getNewDefaultEvent(actions = listOf(click, pause, otherClick)),
            results = ConditionsResults(),
        )

        // Verify the gestures executions
        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.firstValue)
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_click_delay() = runTest {
        val executionDurationMs = 10L
        var isCompleted = false
        mockWhen(mockAndroidExecutor.dispatchBuiltGesture(anyNotNull())).doAnswer {
            runBlocking {
                // The execution is set to 10ms, but we simulate an input lag for a worst case scenario
                delay(executionDurationMs * 10)
                isCompleted = true
            }
        }

        launch(Dispatchers.IO) {
            actionExecutor.executeActions(
                event = getNewDefaultEvent(actions = listOf(getNewDefaultClickUserPos(1, executionDurationMs))),
                results = ConditionsResults(),
            )

            assertTrue("Action execution have not completed yet", isCompleted)
        }.join()
    }

    @Test
    fun checkHeldFinger_nothingHeld_returnsFalseWithoutChecking() = runTest {
        mockWhen(mockAndroidExecutor.getHeldPointer()).thenReturn(null)

        assertFalse(actionExecutor.checkHeldFinger())
        verify(mockAndroidExecutor, never()).checkHeldPointer()
    }

    @Test
    fun checkHeldFinger_pointerStillHeld_returnsTrue() = runTest {
        mockWhen(mockAndroidExecutor.getHeldPointer()).thenReturn(mock<HeldPointer>())

        assertTrue(actionExecutor.checkHeldFinger())
        verify(mockAndroidExecutor).checkHeldPointer()
    }

    @Test
    fun checkHeldFinger_pointerLifted_returnsFalse() = runTest {
        var heldPointer: HeldPointer? = mock<HeldPointer>()
        mockWhen(mockAndroidExecutor.getHeldPointer()).doAnswer { heldPointer }
        mockWhen(mockAndroidExecutor.checkHeldPointer()).doAnswer { heldPointer = null; Unit }

        assertFalse(actionExecutor.checkHeldFinger())
        verify(mockAndroidExecutor).checkHeldPointer()
    }

    @Test
    fun onScenarioLoopFinished_workaroundDisabled_nothingDispatched() = runTest {
        actionExecutor.onScenarioLoopFinished()

        verify(mockAndroidExecutor, never()).dispatchPixelWorkaroundGestureIfNeeded()
    }

    @Test
    fun onScenarioLoopFinished_workaroundEnabled_workaroundDispatched() = runTest {
        actionExecutor = ActionExecutor(
            mockAndroidExecutor, mockProcessingState, randomize = false, unblockWorkaroundEnabled = true,
        )

        actionExecutor.onScenarioLoopFinished()

        verify(mockAndroidExecutor).dispatchPixelWorkaroundGestureIfNeeded()
    }

    @Test
    fun onScenarioLoopFinished_workaroundEnabled_pointerHeld_nothingDispatched() = runTest {
        actionExecutor = ActionExecutor(
            mockAndroidExecutor, mockProcessingState, randomize = false, unblockWorkaroundEnabled = true,
        )
        mockWhen(mockAndroidExecutor.getHeldPointer()).thenReturn(mock<HeldPointer>())

        actionExecutor.onScenarioLoopFinished()

        // The workaround gesture continues nothing, dispatching it would cancel the held pointer
        verify(mockAndroidExecutor, never()).dispatchPixelWorkaroundGestureIfNeeded()
    }
}