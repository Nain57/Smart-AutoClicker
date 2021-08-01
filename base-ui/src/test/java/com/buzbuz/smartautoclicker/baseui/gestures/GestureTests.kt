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
import androidx.annotation.IntDef

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.baseui.utils.mockEvent

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

/** Test the [Gesture] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureTests {

    private companion object {
        private const val TEST_DATA_HANDLE_SIZE = 10f
        private const val TEST_DATA_GESTURE_TYPE = MOVE
        private const val TEST_DATA_EVENT_X_POS = 250f
        private const val TEST_DATA_EVENT_Y_POS = 142f
        private const val TEST_DATA_POINTER_ID = 42
        private const val TEST_DATA_POINTER_INDEX = 18
        private val TEST_DATA_VIEW_AREA = RectF(0f, 0f, 800f, 600f)

        /** Value describing the Gesture implementation call to verify with [verifyEventHandling]. */
        @IntDef(ON_DOWN_EVENT, ON_EVENT, ON_GESTURE_POINTER_UP)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class EventMethod
        /** Verify [Gesture.onDownEvent] */
        private const val ON_DOWN_EVENT = 1
        /** Verify [Gesture.onEvent] */
        private const val ON_EVENT = 2
        /** Verify [Gesture.onGesturePointerUp] */
        private const val ON_GESTURE_POINTER_UP = 3
        /** Nothing should be called.  */
        private const val NONE = 4
    }

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param view the mocked view to apply the gesture on.
     * @param handleSize the size of the handle.
     * @param vibrate vibrate or not on gesture detection.
     * @param ignorePointers true to ignore motion events pointers, false to use them.
     * @param impl the mock called for each abstract method calls.
     */
    class GestureTestImpl(
        view: View,
        handleSize: Float,
        vibrate: Boolean,
        ignorePointers: Boolean = false,
        private val impl: GestureImpl
    ) : Gesture(view, handleSize, vibrate, ignorePointers) {

        override val gestureType = TEST_DATA_GESTURE_TYPE
        override fun onDownEvent(event: MotionEvent, viewArea: RectF) = impl.onDownEvent(event, viewArea)
        override fun onEvent(event: MotionEvent, viewArea: RectF) = impl.onEvent(event, viewArea)
        override fun onGesturePointerUp(event: MotionEvent, viewArea: RectF) = impl.onGesturePointerUp(event, viewArea)
        fun publicInnerHandleSize() = innerHandleSize
        fun publicFirstPointerDownId() = firstPointerDownId
        fun publicCurrentPointerDownIndex() = currentPointerDownIndex
    }

    /**
     * Interface to be mocked in order to instantiates an [GestureTestImpl].
     * Calls on abstract members of [Gesture] can be verified on this mock.
     */
    interface GestureImpl {
        fun onDownEvent(event: MotionEvent, viewArea: RectF): Boolean
        fun onEvent(event: MotionEvent, viewArea: RectF): Boolean
        fun onGesturePointerUp(event: MotionEvent, viewArea: RectF): Boolean
    }

    @Mock private lateinit var mockView: View
    @Mock private lateinit var gestureImpl: GestureImpl

    /** The object under tests. */
    private lateinit var gesture: GestureTestImpl

    /**
     * Assert the state of the [Gesture] after a call to [Gesture.onTouchEvent].
     *
     * @param gesture the gesture.
     * @param expectedResult the expected result of [Gesture.onTouchEvent].
     * @param actualResult the actual result of [Gesture.onTouchEvent].
     * @param expectedFirstPointerId the expected value for [Gesture.firstPointerDownId].
     * @param expectedCurrentPointerIndex the expected value for [Gesture.currentPointerDownIndex].
     */
    private fun assertGestureState(
        gesture: GestureTestImpl,
        expectedResult: Boolean,
        actualResult: Boolean,
        expectedFirstPointerId: Int = Gesture.NO_POINTER_ID,
        expectedCurrentPointerIndex: Int = Gesture.NO_POINTER_ID
    ) {

        assertEquals("Invalid event result", expectedResult, actualResult)
        assertEquals("Invalid first pointer id", expectedFirstPointerId, gesture.publicFirstPointerDownId())
        assertEquals("Invalid current pointer id", expectedCurrentPointerIndex,
            gesture.publicCurrentPointerDownIndex())
    }

    /**
     * Verify the mock calls after a call to [Gesture.onTouchEvent].
     *
     * @param event the parameter of the previous call to [Gesture.onTouchEvent].
     * @param viewArea the second parameter of the previous call to [Gesture.onTouchEvent].
     * @param eventMethod the [Gesture] implementation method to be called. All others will be verified for no calls.
     * @param downIsHandled true if the event should be considered as a down event that has been handled. For haptic
     *                      feedback verification.
     */
    private fun verifyEventHandling(
        event: MotionEvent,
        viewArea: RectF,
        @EventMethod eventMethod: Int?,
        downIsHandled: Boolean = false
    ) {

        verify(gestureImpl, oneOrNever(eventMethod == ON_DOWN_EVENT)).onDownEvent(event, viewArea)
        verify(gestureImpl, oneOrNever(eventMethod == ON_EVENT)).onEvent(event, viewArea)
        verify(gestureImpl, oneOrNever(eventMethod == ON_GESTURE_POINTER_UP)).onGesturePointerUp(event, viewArea)
        verify(mockView, oneOrNever(downIsHandled)).performHapticFeedback(anyInt())
    }

    /** If true, the method will be verified for one call, of false, it should not be called. */
    private fun oneOrNever(one: Boolean) = if (one) times(1) else never()

    /**
     * Clear the mocks expectations for events handling.
     * Useful when chaining two events, but willing to verifies ony the second.
     */
    private fun clearEventHandlingExpectations() = clearInvocations(gestureImpl, mockView)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun innerHandleSize() {
        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        assertEquals(TEST_DATA_HANDLE_SIZE / Gesture.INNER_HANDLE_RATIO, gesture.publicInnerHandleSize())
    }

    @Test
    fun initialPointers() {
        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        assertEquals(Gesture.NO_POINTER_ID, gesture.publicFirstPointerDownId())
        assertEquals(Gesture.NO_POINTER_ID, gesture.publicCurrentPointerDownIndex())
    }

    @Test
    fun onInvalidFirstEvent_WithPointers() {
        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = false

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, NONE)
    }

    @Test
    fun onDownEventNotHandled_WithPointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        val expectedResult = false
        mockWhen(gestureImpl.onDownEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_DOWN_EVENT)
    }

    @Test
    fun onDownEventHandled_WithPointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        val expectedResult = true
        mockWhen(gestureImpl.onDownEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult, TEST_DATA_POINTER_ID)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_DOWN_EVENT, true)
    }

    @Test
    fun onDownEventSecondPointer_WithPointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 2)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, false, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, NONE)
    }

    @Test
    fun onSecondEventInvalidActionIndex_WithPointers() {
        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)

        // First event, it needs to be handled
        val firstEvent = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        mockWhen(gestureImpl.onDownEvent(firstEvent, TEST_DATA_VIEW_AREA)).thenReturn(true)
        gesture.onTouchEvent(firstEvent, TEST_DATA_VIEW_AREA)
        clearEventHandlingExpectations()

        // Second event, the action index must be different than our index
        val secondEvent = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 2, TEST_DATA_POINTER_INDEX, 1)
        val expectedResult = false
        val actualResult = gesture.onTouchEvent(secondEvent, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult, TEST_DATA_POINTER_ID, TEST_DATA_POINTER_INDEX)
        verifyEventHandling(secondEvent, TEST_DATA_VIEW_AREA, NONE)
    }

    @Test
    fun onSecondEventNotUpHandled_WithPointers() {
        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)

        // First event, it needs to be handled
        val firstEvent = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        mockWhen(gestureImpl.onDownEvent(firstEvent, TEST_DATA_VIEW_AREA)).thenReturn(true)
        gesture.onTouchEvent(firstEvent, TEST_DATA_VIEW_AREA)
        clearEventHandlingExpectations()

        // Second event, implementation always returns true on that one
        val secondEvent = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = true
        mockWhen(gestureImpl.onEvent(secondEvent, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)
        val actualResult = gesture.onTouchEvent(secondEvent, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult, TEST_DATA_POINTER_ID, TEST_DATA_POINTER_INDEX)
        verifyEventHandling(secondEvent, TEST_DATA_VIEW_AREA, ON_EVENT)
    }

    @Test
    fun onSecondEventUpHandled_WithPointers() {
        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, false, gestureImpl)

        // First event, it needs to be handled
        val firstEvent = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        mockWhen(gestureImpl.onDownEvent(firstEvent, TEST_DATA_VIEW_AREA)).thenReturn(true)
        gesture.onTouchEvent(firstEvent, TEST_DATA_VIEW_AREA)
        clearEventHandlingExpectations()

        // Second event, implementation always returns true on that one
        val secondEvent = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = true
        mockWhen(gestureImpl.onGesturePointerUp(secondEvent, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)
        val actualResult = gesture.onTouchEvent(secondEvent, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(secondEvent, TEST_DATA_VIEW_AREA, ON_GESTURE_POINTER_UP)
    }

    @Test
    fun onFirstEventUpHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = true
        mockWhen(gestureImpl.onGesturePointerUp(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_GESTURE_POINTER_UP)
    }

    @Test
    fun onFirstEventUpNotHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = false
        mockWhen(gestureImpl.onGesturePointerUp(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_GESTURE_POINTER_UP)
    }

    @Test
    fun onFirstEventOtherHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = true
        mockWhen(gestureImpl.onEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_EVENT)
    }

    @Test
    fun onFirstEventOtherNotHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID, 1, TEST_DATA_POINTER_INDEX)
        val expectedResult = false
        mockWhen(gestureImpl.onEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_EVENT)
    }

    @Test
    fun onFirstEventEventHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        val expectedResult = true
        mockWhen(gestureImpl.onDownEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_DOWN_EVENT, true)
    }

    @Test
    fun onFirstEventEventNotHandled_WithoutPointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID)
        val expectedResult = false
        mockWhen(gestureImpl.onDownEvent(event, TEST_DATA_VIEW_AREA)).thenReturn(expectedResult)

        gesture = GestureTestImpl(mockView, TEST_DATA_HANDLE_SIZE, true, true, gestureImpl)
        val actualResult = gesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)

        assertGestureState(gesture, expectedResult, actualResult)
        verifyEventHandling(event, TEST_DATA_VIEW_AREA, ON_DOWN_EVENT, false)
    }
}