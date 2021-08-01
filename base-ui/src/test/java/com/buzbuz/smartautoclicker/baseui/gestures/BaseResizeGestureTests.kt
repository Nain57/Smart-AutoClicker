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

import com.buzbuz.smartautoclicker.baseui.utils.anyNotNull
import com.buzbuz.smartautoclicker.baseui.utils.mockEvent

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Answers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Test the [ResizeGesture] file class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BaseResizeGestureTests {

    private companion object {
        private const val TEST_DATA_HANDLE_SIZE = 10f
        private const val TEST_DATA_BIG_HANDLE_SIZE = 800f
        private const val TEST_DATA_GESTURE_TYPE = MOVE
        private const val TEST_DATA_EVENT_X_POS = 250f
        private const val TEST_DATA_EVENT_Y_POS = 142f
        private const val TEST_DATA_POINTER_ID = 42
        private val TEST_DATA_VIEW_AREA = RectF(0f, 0f, 800f, 600f)
        private val TEST_DATA_VIEW_AREA_NEW_SIZE = RectF(100f, 100f, 800f, 600f)
        /** Handle containing the point at [TEST_DATA_EVENT_X_POS] / [TEST_DATA_EVENT_Y_POS] */
        private val TEST_DATA_IN_HANDLE_AREA = RectF(200f, 0f, 300f, 600f)
        /** Handle that doesn't contains the point at [TEST_DATA_EVENT_X_POS] / [TEST_DATA_EVENT_Y_POS] */
        private val TEST_DATA_OUT_HANDLE_AREA = RectF(200f, 0f, 300f, 10f)
    }

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param view the mocked view to apply the gesture on.
     * @param handleSize the size of the handle.
     * @param vibrate vibrate or not on gesture detection.
     * @param onResizeListener listener notified upon resize.
     * @param impl the mock called for each abstract method calls.
     */
    class ResizeGestureTestImpl(
        view: View,
        handleSize: Float,
        vibrate: Boolean,
        onResizeListener: (RectF, Int) -> Unit,
        private val impl: ResizeGestureImpl
    ) : ResizeGesture(view, handleSize, vibrate, onResizeListener) {

        override val gestureType = TEST_DATA_GESTURE_TYPE
        override fun getNewSize(event: MotionEvent, viewArea: RectF) = impl.getNewSize(event, viewArea)
        override fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) = impl.getHandleArea(viewArea, enoughInnerSpace)
    }

    /**
     * Interface to be mocked in order to instantiates an [ResizeGestureImpl].
     * Calls on abstract members of [ResizeGesture] can be verified on this mock.
     */
    interface ResizeGestureImpl {
        fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean): RectF
        fun getNewSize(event: MotionEvent, viewArea: RectF): RectF
    }

    /** Interface to be mocked in order to verify the calls to the gesture listener. */
    interface ResizeListener {
        fun onResize(newArea: RectF, @GestureType resizeType: Int)
    }

    @Mock private lateinit var mockView: View
    @Mock private lateinit var mockResizeListener: ResizeListener
    @Mock(answer = Answers.RETURNS_MOCKS) private lateinit var mockResizeGestureImpl: ResizeGestureImpl

    /** The object under test. */
    private lateinit var resizeGesture: ResizeGestureTestImpl

    /** Mock a first valid [MotionEvent.ACTION_DOWN] event. */
    private fun mockFirstValidEvent() {
        mockWhen(mockResizeGestureImpl.getHandleArea(TEST_DATA_VIEW_AREA, true))
            .thenReturn(TEST_DATA_IN_HANDLE_AREA)
        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun onDown_enoughSpace_implCall() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true,  mockResizeListener::onResize,
            mockResizeGestureImpl)

        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeGestureImpl).getHandleArea(TEST_DATA_VIEW_AREA, true)
    }

    @Test
    fun onDown_enoughSpace_inHandle() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockWhen(mockResizeGestureImpl.getHandleArea(TEST_DATA_VIEW_AREA, true))
            .thenReturn(TEST_DATA_IN_HANDLE_AREA)

        val result = resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals(true, result)
    }

    @Test
    fun onDown_enoughSpace_notInHandle() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockWhen(mockResizeGestureImpl.getHandleArea(TEST_DATA_VIEW_AREA, true))
            .thenReturn(TEST_DATA_OUT_HANDLE_AREA)

        val result = resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals(false, result)
    }

    @Test
    fun onDown_notEnoughSpace_implCall() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_BIG_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)

        resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        verify(mockResizeGestureImpl).getHandleArea(TEST_DATA_VIEW_AREA, false)
    }

    @Test
    fun onDown_notEnoughSpace_inHandle() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_BIG_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockWhen(mockResizeGestureImpl.getHandleArea(TEST_DATA_VIEW_AREA, false))
            .thenReturn(TEST_DATA_IN_HANDLE_AREA)

        val result = resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals(true, result)
    }

    @Test
    fun onDown_notEnoughSpace_notInHandle() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_BIG_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockWhen(mockResizeGestureImpl.getHandleArea(TEST_DATA_VIEW_AREA, false))
            .thenReturn(TEST_DATA_OUT_HANDLE_AREA)

        val result = resizeGesture.onTouchEvent(
            mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID),
            TEST_DATA_VIEW_AREA
        )

        assertEquals(false, result)
    }

    @Test
    fun onMove_result() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockFirstValidEvent()

        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        val result = resizeGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertEquals(true, result)
    }

    @Test
    fun onMove_implCall() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockFirstValidEvent()

        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        resizeGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        verify(mockResizeGestureImpl).getNewSize(event, TEST_DATA_VIEW_AREA)
    }

    @Test
    fun onMove_listenerCall() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockFirstValidEvent()

        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        mockWhen(mockResizeGestureImpl.getNewSize(event, TEST_DATA_VIEW_AREA)).thenReturn(TEST_DATA_VIEW_AREA_NEW_SIZE)
        resizeGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        verify(mockResizeListener).onResize(TEST_DATA_VIEW_AREA_NEW_SIZE, TEST_DATA_GESTURE_TYPE)
    }

    @Test
    fun onUp() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockFirstValidEvent()

        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        val result = resizeGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertEquals(true, result)
        verify(mockResizeGestureImpl, never()).getNewSize(event, TEST_DATA_VIEW_AREA)
        verify(mockResizeListener, never()).onResize(anyNotNull(), eq(TEST_DATA_GESTURE_TYPE))
    }

    @Test
    fun onOtherEvent() {
        resizeGesture = ResizeGestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, mockResizeListener::onResize,
            mockResizeGestureImpl)
        mockFirstValidEvent()

        val event = mockEvent(MotionEvent.ACTION_SCROLL, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        val result = resizeGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertEquals(false, result)
        verify(mockResizeGestureImpl, never()).getNewSize(event, TEST_DATA_VIEW_AREA)
        verify(mockResizeListener, never()).onResize(anyNotNull(), eq(TEST_DATA_GESTURE_TYPE))
    }
}