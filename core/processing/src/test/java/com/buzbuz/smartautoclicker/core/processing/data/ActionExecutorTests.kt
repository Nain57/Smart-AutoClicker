/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.processing.utils.anyNotNull

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

import org.robolectric.annotation.Config

/** Test the [ActionExecutor] class. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionExecutorTests {

    private companion object {
        private val TEST_EVENT_ID = Identifier(42L)
        private const val TEST_NAME = "Action name"
        private const val TEST_DURATION = 25L
        private const val TEST_X1 = 12
        private const val TEST_X2 = 24
        private const val TEST_Y1 = 88
        private const val TEST_Y2 = 76

        fun getNewDefaultClick(id: Long, clickOnCondition: Boolean, duration: Long = TEST_DURATION) =
            Action.Click(Identifier(id), TEST_EVENT_ID, TEST_NAME, duration, TEST_X1, TEST_Y1, clickOnCondition)
        fun getNewDefaultSwipe(id: Long) =
            Action.Swipe(Identifier(id), TEST_EVENT_ID, TEST_NAME, TEST_DURATION, TEST_X1, TEST_Y1, TEST_X2, TEST_Y2)
        fun getNewDefaultPause(id: Long) =
            Action.Pause(Identifier(id), TEST_EVENT_ID, TEST_NAME, TEST_DURATION)
    }

    @Mock private lateinit var mockAndroidExecutor: AndroidExecutor
    @Mock private lateinit var mockScenarioEditor: ScenarioEditor

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
        Dispatchers.setMain(StandardTestDispatcher())

        actionExecutor = ActionExecutor(mockAndroidExecutor, mockScenarioEditor, randomize = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noActions() = runTest {
        actionExecutor.executeActions(emptyList(), Point())
        verify(mockAndroidExecutor, never()).executeGesture(anyNotNull())
    }

    @Test
    fun execute_oneClick_notOnCondition() = runTest {
        val clickAction = getNewDefaultClick(1, false)

        actionExecutor.executeActions(listOf(clickAction), Point())

        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockAndroidExecutor).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_oneClick_onCondition() = runTest {
        val clickAction = getNewDefaultClick(1, true)

        actionExecutor.executeActions(listOf(clickAction), Point(15, 15))

        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockAndroidExecutor).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_oneSwipe() = runTest {
        val swipeAction = getNewDefaultSwipe(1)

        actionExecutor.executeActions(listOf(swipeAction), Point())

        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockAndroidExecutor).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_onePause() = runTest {
        val pause = getNewDefaultPause(1)

        // Execute the pause. As the handler is waiting to the finish the pause, we should stays in EXECUTING
        actionExecutor.executeActions(listOf(pause), Point())

        // Only a pause, there should be no gestures
        verify(mockAndroidExecutor, never()).executeGesture(anyNotNull())
    }

    @Test
    fun execute_mixed() = runTest {
        val click = getNewDefaultClick(1, false)
        val pause = getNewDefaultPause(2)
        val swipe = getNewDefaultSwipe(3)
        val gestureCaptor = argumentCaptor<GestureDescription>()

        // Execute the actions.
        actionExecutor.executeActions(listOf(click, pause, swipe), Point())

        // Verify the gestures executions
        verify(mockAndroidExecutor, times(2)).executeGesture(gestureCaptor.capture())
        assertActionGesture(gestureCaptor.firstValue)
        assertActionGesture(gestureCaptor.lastValue)
    }

    @Test
    fun execute_click_delay() = runTest {
        val executionDurationMs = 10L
        var isCompleted = false
        mockWhen(mockAndroidExecutor.executeGesture(anyNotNull())).doAnswer {
            runBlocking {
                // The execution is set to 10ms, but we simulate an input lag for a worst case scenario
                delay(executionDurationMs * 10)
                isCompleted = true
            }
        }

        launch(Dispatchers.IO) {
            actionExecutor.executeActions(
                listOf(getNewDefaultClick(1, false, executionDurationMs)),
                Point()
            )

            assertTrue("Action execution have not completed yet", isCompleted)
        }.join()
    }
}