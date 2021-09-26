/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.detection

import android.accessibilityservice.GestureDescription
import android.os.Build
import android.os.Looper

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.detection.utils.anyNotNull

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.kotlin.argumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

import org.robolectric.Shadows.shadowOf

import org.robolectric.annotation.Config

/** Test the [ActionExecutor] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionExecutorTests {

    private companion object {
        private const val TEST_EVENT_ID = 42L
        private const val TEST_NAME = "Action name"
        private const val TEST_DURATION = 145L
        private const val TEST_X1 = 12
        private const val TEST_X2 = 24
        private const val TEST_Y1 = 88
        private const val TEST_Y2 = 76

        fun getNewDefaultClick(id: Long) =
            Action.Click(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION, TEST_X1, TEST_Y1)
        fun getNewDefaultSwipe(id: Long) =
            Action.Swipe(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION, TEST_X1, TEST_Y1, TEST_X2, TEST_Y2)
        fun getNewDefaultPause(id: Long) =
            Action.Pause(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION)
    }

    private interface ExecutionListener {
        fun executeGesture(gesture: GestureDescription)
    }
    @Mock private lateinit var mockExecutionListener: ExecutionListener

    private lateinit var actionExecutor: ActionExecutor

    private fun assertActionGesture(gesture: GestureDescription) {
        assertEquals("Gesture should contains only one stroke", 1, gesture.strokeCount)
        gesture.getStroke(0).let { stroke ->
            assertEquals("Gesture duration is invalid", TEST_DURATION, stroke.duration)
            assertEquals("Gesture start time is invalid", 0, stroke.startTime)
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        actionExecutor = ActionExecutor()
        actionExecutor.onGestureExecutionListener = mockExecutionListener::executeGesture
    }

    @Test
    fun defaultState() {
        assertEquals(ActionExecutor.State.IDLE, actionExecutor.state)
    }

    @Test
    fun noActions() {
        actionExecutor.executeActions(emptyList())

        verify(mockExecutionListener, never()).executeGesture(anyNotNull())
        assertEquals(ActionExecutor.State.IDLE, actionExecutor.state)
    }

    @Test
    fun execute_oneClick() {
        val clickAction = getNewDefaultClick(1)

        actionExecutor.executeActions(listOf(clickAction))
        shadowOf(Looper.getMainLooper()).idle()

        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockExecutionListener).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_oneSwipe() {
        val swipeAction = getNewDefaultSwipe(1)

        actionExecutor.executeActions(listOf(swipeAction))
        shadowOf(Looper.getMainLooper()).idle()

        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockExecutionListener).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_onePause() {
        val pause = getNewDefaultPause(1)

        // Execute the pause. As the handler is waiting to the finish the pause, we should stays in EXECUTING
        actionExecutor.executeActions(listOf(pause))
        assertEquals(ActionExecutor.State.EXECUTING, actionExecutor.state)

        // Execute the pause end. Executor should be back to IDLE
        shadowOf(Looper.getMainLooper()).runToEndOfTasks()
        assertEquals(ActionExecutor.State.IDLE, actionExecutor.state)

        // Only a pause, there should be no gestures
        verify(mockExecutionListener, never()).executeGesture(anyNotNull())
    }

    @Test
    fun execute_twoPauses() {
        val pause1 = getNewDefaultPause(1)
        val pause2 = getNewDefaultPause(2)

        // Execute the pauses. As the handler is waiting to the finish the pause, we should stays in EXECUTING
        actionExecutor.executeActions(listOf(pause1, pause2))
        assertEquals(ActionExecutor.State.EXECUTING, actionExecutor.state)

        // Execute the first pause end. Executor should stays EXECUTING.
        shadowOf(Looper.getMainLooper()).runToNextTask()
        assertEquals(ActionExecutor.State.EXECUTING, actionExecutor.state)

        // Execute the second pause end. Executor should be back to IDLE
        shadowOf(Looper.getMainLooper()).runToNextTask()
        assertEquals(ActionExecutor.State.IDLE, actionExecutor.state)

        // Only a pause, there should be no gestures
        verify(mockExecutionListener, never()).executeGesture(anyNotNull())
    }

    @Test
    fun execute_mixed() {
        val click = getNewDefaultClick(1)
        val pause = getNewDefaultPause(2)
        val swipe = getNewDefaultSwipe(3)
        val gestureCaptor = argumentCaptor<GestureDescription>()

        // Execute the actions.
        actionExecutor.executeActions(listOf(click, pause, swipe))

        // Execute the click
        shadowOf(Looper.getMainLooper()).runToNextTask()
        // As the handler is waiting to the finish the pause, we should be in EXECUTING.
        assertEquals(ActionExecutor.State.EXECUTING, actionExecutor.state)
        // Execute the pause end.
        shadowOf(Looper.getMainLooper()).runToNextTask()
        // Execute the swipe.
        shadowOf(Looper.getMainLooper()).runToNextTask()
        // All actions are executed, we should be in IDLE.
        assertEquals(ActionExecutor.State.IDLE, actionExecutor.state)

        // Verify the gestures executions
        verify(mockExecutionListener, times(2)).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.firstValue)
        assertActionGesture(gestureCaptor.lastValue)
    }
}