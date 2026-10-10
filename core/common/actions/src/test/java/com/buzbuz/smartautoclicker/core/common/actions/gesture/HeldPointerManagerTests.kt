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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent

import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tests how [HeldPointerManager] keeps track of the pointer left down on the screen, checks it against the user touches and lifts it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class HeldPointerManagerTests {

    private lateinit var userTouchWatcher: UserTouchWatcher
    private lateinit var manager: HeldPointerManager
    private val dispatchedGestures = mutableListOf<GestureDescription>()

    @Before
    fun setUp() {
        userTouchWatcher = UserTouchWatcher()
        manager = HeldPointerManager(userTouchWatcher)
        dispatchedGestures.clear()
    }

    @Test
    fun onHeldPointerContinuationChanged_updatesHeldPointer() {
        val pointer = heldPointer()

        manager.onHeldPointerContinuationChanged(pointer)
        assertSame(pointer, manager.heldPointer)

        manager.onHeldPointerContinuationChanged(null)
        assertNull(manager.heldPointer)
    }

    @Test
    fun onHeldPointerCancelled_dropsHeldPointer() {
        manager.onHeldPointerContinuationChanged(heldPointer())

        manager.onHeldPointerCancelled()

        assertNull(manager.heldPointer)
    }

    @Test
    fun checkHeldPointer_nothingHeld_returnsTrue() {
        assertTrue(manager.checkHeldPointer())
    }

    @Test
    fun checkHeldPointer_noUserTouch_returnsTrue_andKeepsHeldPointer() {
        val pointer = heldPointer()
        manager.onHeldPointerContinuationChanged(pointer, dispatchStartMs = 100L)

        assertTrue(manager.checkHeldPointer())
        assertSame(pointer, manager.heldPointer)
    }

    @Test
    fun checkHeldPointer_userTouchAfterHoldStart_returnsFalse_andDropsHeldPointer() {
        manager.onHeldPointerContinuationChanged(heldPointer(), dispatchStartMs = 100L)
        userTouchWatcher.onWatcherTouchEvent(touchEvent(MotionEvent.ACTION_OUTSIDE, USER_DEVICE_ID, eventTimeMs = 200L))

        assertFalse(manager.checkHeldPointer())
        assertNull(manager.heldPointer)
    }

    @Test
    fun checkHeldPointer_userTouchBeforeHoldStart_returnsTrue_andKeepsHeldPointer() {
        userTouchWatcher.onWatcherTouchEvent(touchEvent(MotionEvent.ACTION_OUTSIDE, USER_DEVICE_ID, eventTimeMs = 50L))
        val pointer = heldPointer()
        manager.onHeldPointerContinuationChanged(pointer, dispatchStartMs = 100L)

        assertTrue(manager.checkHeldPointer())
        assertSame(pointer, manager.heldPointer)
    }

    @Test
    fun checkHeldPointer_userTouchDuringContinuationDispatch_returnsFalse_andDropsHeldPointer() {
        manager.onHeldPointerContinuationChanged(heldPointer(), dispatchStartMs = 100L)
        userTouchWatcher.onWatcherTouchEvent(touchEvent(MotionEvent.ACTION_OUTSIDE, USER_DEVICE_ID, eventTimeMs = 150L))

        // The continuation was built before the touch and completes after it: the hold still started at 100
        manager.onHeldPointerContinuationChanged(heldPointer(), dispatchStartMs = 200L)

        assertFalse(manager.checkHeldPointer())
        assertNull(manager.heldPointer)
    }

    @Test
    fun checkHeldPointer_newHoldAfterLoss_startsAtItsDispatch() {
        manager.onHeldPointerContinuationChanged(heldPointer(), dispatchStartMs = 100L)
        userTouchWatcher.onWatcherTouchEvent(touchEvent(MotionEvent.ACTION_OUTSIDE, USER_DEVICE_ID, eventTimeMs = 200L))
        assertFalse(manager.checkHeldPointer())
        val pointer = heldPointer()

        manager.onHeldPointerContinuationChanged(pointer, dispatchStartMs = 300L)

        assertTrue(manager.checkHeldPointer())
        assertSame(pointer, manager.heldPointer)
    }

    @Test
    fun releaseHeldPointer_nothingHeld_returnsTrueWithoutDispatching() = runTest {
        assertTrue(manager.releaseHeldPointer(dispatcher(result = true)))
        assertTrue(dispatchedGestures.isEmpty())
    }

    @Test
    fun releaseHeldPointer_held_dispatchesLastStrokeAtPointerPosition() = runTest {
        manager.onHeldPointerContinuationChanged(heldPointer())

        assertTrue(manager.releaseHeldPointer(dispatcher(result = true)))

        val release = dispatchedGestures.single()
        assertEquals(1, release.strokeCount)
        val releaseStroke = release.getStroke(0)
        assertFalse("The release must end the touch sequence", releaseStroke.willContinue())
        assertPathPoints(releaseStroke, HELD_POSITION)
        assertNull(manager.heldPointer)
    }

    @Test
    fun releaseHeldPointer_releaseRefused_returnsFalse_andDropsHeldPointer() = runTest {
        manager.onHeldPointerContinuationChanged(heldPointer())

        assertFalse(manager.releaseHeldPointer(dispatcher(result = false)))
        assertNull(manager.heldPointer)
    }

    @Test
    fun releaseHeldPointerNow_dispatchesWithoutWaitingForResult() {
        val service = mock(AccessibilityService::class.java)
        manager.onHeldPointerContinuationChanged(heldPointer())

        manager.releaseHeldPointerNow(service)

        verify(service).dispatchGesture(any(), isNull(), isNull())
        assertNull(manager.heldPointer)
    }

    @Test
    fun releaseHeldPointerNow_noService_dropsHeldPointer() {
        manager.onHeldPointerContinuationChanged(heldPointer())

        manager.releaseHeldPointerNow(null)

        assertNull(manager.heldPointer)
    }

    @Test
    fun releaseHeldPointerNow_dispatchException_dropsHeldPointer() {
        val service = mock(AccessibilityService::class.java)
        doThrow(IllegalStateException("Accessibility service is unavailable"))
            .`when`(service)
            .dispatchGesture(any(), any(), any())
        manager.onHeldPointerContinuationChanged(heldPointer())

        manager.releaseHeldPointerNow(service)

        assertNull(manager.heldPointer)
    }

    private fun dispatcher(result: Boolean): suspend (GestureDescription) -> Boolean = { gesture ->
        dispatchedGestures.add(gesture)
        result
    }

    private fun assertPathPoints(stroke: StrokeDescription, vararg expected: Point) {
        val points = shadowOf(stroke.path).points.map { PointF(it.x, it.y) }
        assertEquals(expected.map { PointF(it.x.toFloat(), it.y.toFloat()) }, points)
    }

    private fun touchEvent(action: Int, deviceId: Int, eventTimeMs: Long): MotionEvent =
        MotionEvent.obtain(
            /* downTime = */ eventTimeMs,
            /* eventTime = */ eventTimeMs,
            /* action = */ action,
            /* pointerCount = */ 1,
            /* pointerProperties = */ arrayOf(MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }),
            /* pointerCoords = */ arrayOf(MotionEvent.PointerCoords()),
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 1f,
            /* yPrecision = */ 1f,
            /* deviceId = */ deviceId,
            /* edgeFlags = */ 0,
            /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
            /* flags = */ 0,
        )

    private fun heldPointer(): HeldPointer =
        HeldPointer(
            stroke = StrokeDescription(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(HELD_POSITION.x.toFloat(), HELD_POSITION.y.toFloat())
                },
                0L,
                100L,
                true,
            ),
            position = HELD_POSITION,
            owner = OWNER,
        )
}

private val HELD_POSITION = Point(10, 20)
private const val OWNER = "owner"
private const val USER_DEVICE_ID = 4
