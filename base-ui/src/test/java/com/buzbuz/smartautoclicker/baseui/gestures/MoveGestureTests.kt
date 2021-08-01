/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.baseui.gestures

import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import android.view.View

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.baseui.utils.mockEvent

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

/** Test the [MoveGesture] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class MoveGestureTests {

    private companion object {
        private const val TEST_DATA_HANDLE_SIZE = 10f
        private const val TEST_DATA_BIG_HANDLE_SIZE = 800f
        private const val TEST_DATA_IN_EVENT_X_POS = 250f
        private const val TEST_DATA_IN_EVENT_Y_POS = 142f
        private const val TEST_DATA_OUT_EVENT_X_POS = -250f
        private const val TEST_DATA_OUT_EVENT_Y_POS = -142f
        private const val TEST_DATA_POINTER_ID = 42
        private val TEST_DATA_VIEW_AREA = RectF(0f, 0f, 800f, 600f)
    }

    /** Interface to be mocked in order to verify the calls to the gesture listener. */
    interface MoveListener {
        fun onMove(x: Float, y: Float)
    }

    @Mock private lateinit var mockListener: MoveListener
    @Mock private lateinit var mockView: View

    /** The object under test. */
    private lateinit var moveGesture: MoveGesture

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun onDown_eventInArea_withoutInnerHandle() {
        moveGesture = MoveGesture(mockView, TEST_DATA_BIG_HANDLE_SIZE, true, mockListener::onMove)
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_IN_EVENT_X_POS, TEST_DATA_IN_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", true, result)
    }

    @Test
    fun onDown_eventOutsideArea_withoutInnerHandle() {
        moveGesture = MoveGesture(mockView, TEST_DATA_BIG_HANDLE_SIZE, true, mockListener::onMove)
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_OUT_EVENT_X_POS, TEST_DATA_OUT_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", false, result)
    }

    @Test
    fun onDown_eventInArea_withInnerHandle() {
        moveGesture = MoveGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockListener::onMove)
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_IN_EVENT_X_POS, TEST_DATA_IN_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", true, result)
    }

    @Test
    fun onDown_eventOutsideArea_withInnerHandle() {
        moveGesture = MoveGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockListener::onMove)
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_OUT_EVENT_X_POS, TEST_DATA_OUT_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", false, result)
    }

    @Test
    fun onMove() {
        // First we need to start the gesture with a down event
        moveGesture = MoveGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockListener::onMove)
        moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_IN_EVENT_X_POS, TEST_DATA_IN_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        // Tested call. We change the values for x and y.
        val xOffset = 100
        val yOffset = -100
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_IN_EVENT_X_POS + xOffset, TEST_DATA_IN_EVENT_Y_POS + yOffset, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", true, result)
        verify(mockListener).onMove(TEST_DATA_VIEW_AREA.centerX() + xOffset, TEST_DATA_VIEW_AREA.centerY() + yOffset)
    }

    @Test
    fun onUp() {
        // First we need to start the gesture with a down event
        moveGesture = MoveGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockListener::onMove)
        moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_IN_EVENT_X_POS, TEST_DATA_IN_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        // Tested call. We send a UP event
        val result = moveGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_UP, TEST_DATA_IN_EVENT_X_POS, TEST_DATA_IN_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals("Invalid event result", true, result)
    }
}