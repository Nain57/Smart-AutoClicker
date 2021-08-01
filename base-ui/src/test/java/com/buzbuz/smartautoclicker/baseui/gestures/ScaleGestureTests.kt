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
import android.view.ScaleGestureDetector
import android.view.View

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.baseui.utils.mockEvent

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.withSettings
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Test the [ScaleGesture] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScaleGestureTests {

    private companion object {
        private const val TEST_DATA_SCALE_FACTOR = 0.72f
        private const val TEST_DATA_HANDLE_SIZE = 10f
        private const val TEST_DATA_EVENT_X_POS = 250f
        private const val TEST_DATA_EVENT_Y_POS = 142f
        private const val TEST_DATA_POINTER_ID = 42
        private const val TEST_DATA_POINTER_INDEX = 18
        private val TEST_DATA_VIEW_AREA = RectF(0f, 0f, 800f, 600f)
    }

    /** Interface to be mocked in order to verify the calls to the gesture listener. */
    interface ScaleListener {
        fun onScale(ratio: Float)
    }

    @Mock private lateinit var mockView: View
    @Mock private lateinit var mockScaleListener: ScaleListener

    /** Mock for the Android gesture detector. Initialized on first use, i.e, after a call to [ScaleGesture.onTouchEvent] */
    private lateinit var mockGestureDetector: ScaleGestureDetector
    /** Actual Android gesture detector listener. Initialized with [mockGestureDetector]. */
    private lateinit var gestureDetectorListener: ScaleGestureDetector.SimpleOnScaleGestureListener

    /** The object under tests. */
    private lateinit var scaleGesture: ScaleGesture

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        scaleGesture = ScaleGesture(mockView, TEST_DATA_HANDLE_SIZE, context, true, mockScaleListener::onScale)
        scaleGesture.scaleDetectorSupplier = { listener ->
            gestureDetectorListener = listener
            mockGestureDetector = mock(
                ScaleGestureDetector::class.java,
                withSettings()
                    .spiedInstance(ScaleGestureDetector(context, listener))
                    .defaultAnswer(Answers.RETURNS_DEFAULTS)
            )
            mockGestureDetector
        }
    }

    @Test
    fun onDown() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onDownMultiplePointers() {
        val event = mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID,
            TEST_DATA_POINTER_INDEX, 2)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onMove() {
        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onMoveMultiplePointers() {
        val event = mockEvent(MotionEvent.ACTION_MOVE, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID,
            TEST_DATA_POINTER_INDEX, 2)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onUp() {
        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onUpMultiplePointers() {
        val event = mockEvent(MotionEvent.ACTION_UP, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS, TEST_DATA_POINTER_ID,
            TEST_DATA_POINTER_INDEX, 2)
        scaleGesture.onTouchEvent(event, TEST_DATA_VIEW_AREA)
        verify(mockGestureDetector).onTouchEvent(event)
    }

    @Test
    fun onScaleListener() {
        // We must call onTouchEvent at least once to initialize the mocks
        scaleGesture.onTouchEvent(mockEvent(MotionEvent.ACTION_DOWN, TEST_DATA_EVENT_X_POS, TEST_DATA_EVENT_Y_POS,
            TEST_DATA_POINTER_ID), TEST_DATA_VIEW_AREA)

        mockWhen(mockGestureDetector.scaleFactor).thenReturn(TEST_DATA_SCALE_FACTOR)
        gestureDetectorListener.onScale(mockGestureDetector)

        verify(mockScaleListener).onScale(TEST_DATA_SCALE_FACTOR)
    }
}