/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.engine.utils.anyNotNull

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest

import org.junit.After
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

import org.robolectric.annotation.Config

/** Test the [ActionExecutor] class. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionExecutorTests {

    private companion object {
        private const val TEST_EVENT_ID = 42L
        private const val TEST_NAME = "Action name"
        private const val TEST_DURATION = 25L
        private const val TEST_X1 = 12
        private const val TEST_X2 = 24
        private const val TEST_Y1 = 88
        private const val TEST_Y2 = 76

        fun getNewDefaultClick(id: Long, clickOnCondition: Boolean) =
            com.buzbuz.smartautoclicker.domain.Action.Click(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION, TEST_X1, TEST_Y1, clickOnCondition)
        fun getNewDefaultSwipe(id: Long) =
            com.buzbuz.smartautoclicker.domain.Action.Swipe(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION, TEST_X1, TEST_Y1, TEST_X2, TEST_Y2)
        fun getNewDefaultPause(id: Long) =
            com.buzbuz.smartautoclicker.domain.Action.Pause(id, TEST_EVENT_ID, TEST_NAME, TEST_DURATION)
    }

    @Mock private lateinit var mockAndroidExecutor: AndroidExecutor

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

        actionExecutor = ActionExecutor(mockAndroidExecutor)
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
}