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

/** Test the [ResizeLeftGesture], [ResizeTopGesture], [ResizeRightGesture] and [ResizeBottomGesture] classes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ResizeGesturesImplTests {

    private companion object {
        private const val TEST_DATA_HANDLE_SIZE = 9f
        private const val TEST_DATA_INNER_HANDLE = TEST_DATA_HANDLE_SIZE / Gesture.INNER_HANDLE_RATIO
        private const val TEST_DATA_EVENT_X_POS = 250f
        private const val TEST_DATA_EVENT_Y_POS = 142f
        private const val TEST_DATA_POINTER_ID = 42
        private val TEST_DATA_VIEW_AREA = RectF(0f, 0f, 800f, 600f)
    }

    /** Interface to be mocked in order to verify the calls to the gesture listener. */
    interface ResizeListener {
        fun onResize(newArea: RectF, @GestureType resizeType: Int)
    }

    @Mock private lateinit var mockResizeListener: ResizeListener
    @Mock private lateinit var mockView: View

    /** The object under tests. */
    private lateinit var resizeGesture: ResizeGesture

    /** */
    private fun mockValidActionDownEvent(xPos: Float, yPos: Float) = resizeGesture.onTouchEvent(
        mockEvent(MotionEvent.ACTION_DOWN, xPos, yPos, TEST_DATA_POINTER_ID),
        TEST_DATA_VIEW_AREA
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun left_getHandleArea_enoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left - TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.left + TEST_DATA_INNER_HANDLE,
            TEST_DATA_VIEW_AREA.bottom
        )

        resizeGesture = ResizeLeftGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, true)

        assertEquals(expected, result)
    }

    @Test
    fun left_getHandleArea_notEnoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left - TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.bottom
        )

        resizeGesture = ResizeLeftGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, false)

        assertEquals(expected, result)
    }

    @Test
    fun left_getNewSize() {
        resizeGesture = ResizeLeftGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.left + 1, TEST_DATA_VIEW_AREA.centerY())

        val newXPos = TEST_DATA_VIEW_AREA.left + 10f
        val expected = RectF(
            newXPos,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, newXPos, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_LEFT)
    }

    @Test
    fun left_getNewSize_corrected() {
        resizeGesture = ResizeLeftGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.left + 1, TEST_DATA_VIEW_AREA.centerY())

        val newXPos = TEST_DATA_VIEW_AREA.right
        val expected = RectF(
            newXPos,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, newXPos + 1, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_LEFT)
    }

    @Test
    fun top_getHandleArea_enoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top - TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.top + TEST_DATA_INNER_HANDLE
        )

        resizeGesture = ResizeTopGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, true)

        assertEquals(expected, result)
    }

    @Test
    fun top_getHandleArea_notEnoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top - TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.top
        )

        resizeGesture = ResizeTopGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, false)

        assertEquals(expected, result)
    }

    @Test
    fun top_getNewSize() {
        resizeGesture = ResizeTopGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.centerX(), TEST_DATA_VIEW_AREA.top + 1)

        val newYPos = TEST_DATA_VIEW_AREA.top + 10f
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            newYPos,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, newYPos, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_TOP)
    }

    @Test
    fun top_getNewSize_corrected() {
        resizeGesture = ResizeTopGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.centerX(), TEST_DATA_VIEW_AREA.top + 1)

        val newYPos = TEST_DATA_VIEW_AREA.bottom
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            newYPos,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, newYPos + 1, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_TOP)
    }

    @Test
    fun right_getHandleArea_enoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.right - TEST_DATA_INNER_HANDLE,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right + TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.bottom
        )

        resizeGesture = ResizeRightGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, true)

        assertEquals(expected, result)
    }

    @Test
    fun right_getHandleArea_notEnoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right + TEST_DATA_HANDLE_SIZE,
            TEST_DATA_VIEW_AREA.bottom
        )

        resizeGesture = ResizeRightGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, false)

        assertEquals(expected, result)
    }

    @Test
    fun right_getNewSize() {
        resizeGesture = ResizeRightGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.right - 1, TEST_DATA_VIEW_AREA.centerX())

        val newXPos = TEST_DATA_VIEW_AREA.right - 10f
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top,
            newXPos,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, newXPos, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_RIGHT)
    }

    @Test
    fun right_getNewSize_corrected() {
        resizeGesture = ResizeRightGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.right - 1, TEST_DATA_VIEW_AREA.centerX())

        val newXPos = TEST_DATA_VIEW_AREA.left
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top,
            newXPos,
            TEST_DATA_VIEW_AREA.bottom
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, newXPos, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_RIGHT)
    }

    @Test
    fun bottom_getHandleArea_enoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.bottom - TEST_DATA_INNER_HANDLE,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom + TEST_DATA_HANDLE_SIZE
        )

        resizeGesture = ResizeBottomGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, true)

        assertEquals(expected, result)
    }

    @Test
    fun bottom_getHandleArea_notEnoughInnerSpace() {
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.bottom,
            TEST_DATA_VIEW_AREA.right,
            TEST_DATA_VIEW_AREA.bottom + TEST_DATA_HANDLE_SIZE
        )

        resizeGesture = ResizeBottomGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        val result = resizeGesture.getHandleArea(TEST_DATA_VIEW_AREA, false)

        assertEquals(expected, result)
    }

    @Test
    fun bottom_getNewSize() {
        resizeGesture = ResizeBottomGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.centerX(), TEST_DATA_VIEW_AREA.bottom - 1)

        val newYPos = TEST_DATA_VIEW_AREA.bottom - 10f
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right,
            newYPos
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, newYPos, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_BOTTOM)
    }

    @Test
    fun bottom_getNewSize_corrected() {
        resizeGesture = ResizeBottomGesture(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize)
        mockValidActionDownEvent(TEST_DATA_VIEW_AREA.centerX(), TEST_DATA_VIEW_AREA.bottom - 1)

        val newYPos = TEST_DATA_VIEW_AREA.top
        val expected = RectF(
            TEST_DATA_VIEW_AREA.left,
            TEST_DATA_VIEW_AREA.top,
            TEST_DATA_VIEW_AREA.right,
            newYPos
        )
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, newYPos, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeListener).onResize(expected, RESIZE_BOTTOM)
    }
}