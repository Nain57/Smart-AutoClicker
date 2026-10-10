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
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl

/** Tests how [UserTouchWatcher] tells the user touches from the injected ones, and manages its watcher window. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class UserTouchWatcherTests {

    private lateinit var watcher: UserTouchWatcher

    @Before
    fun setUp() {
        watcher = UserTouchWatcher()
    }

    @Test
    fun hasUserTouchedSince_noTouch_returnsFalse() {
        assertFalse(watcher.hasUserTouchedSince(0L))
    }

    @Test
    fun hasUserTouchedSince_userTouchOutside_returnsTrueFromItsTime() {
        watcher.onWatcherTouchEvent(userTouch(MotionEvent.ACTION_OUTSIDE, eventTimeMs = 200L))

        assertTrue(watcher.hasUserTouchedSince(100L))
        assertTrue(watcher.hasUserTouchedSince(200L))
        assertFalse(watcher.hasUserTouchedSince(300L))
    }

    @Test
    fun hasUserTouchedSince_userTouchOnWindow_returnsTrue() {
        watcher.onWatcherTouchEvent(userTouch(MotionEvent.ACTION_DOWN, eventTimeMs = 200L))

        assertTrue(watcher.hasUserTouchedSince(100L))
    }

    @Test
    fun hasUserTouchedSince_userTouchNotStartingSequence_returnsFalse() {
        watcher.onWatcherTouchEvent(userTouch(MotionEvent.ACTION_MOVE, eventTimeMs = 200L))

        assertFalse(watcher.hasUserTouchedSince(100L))
    }

    @Test
    fun hasUserTouchedSince_userTouchWithVirtualDeviceId_returnsTrue() {
        // Android 11 gives the virtual device id to every event going through the accessibility input filter
        watcher.onWatcherTouchEvent(
            touchEvent(MotionEvent.ACTION_OUTSIDE, VIRTUAL_DEVICE_ID, MotionEvent.TOOL_TYPE_FINGER, eventTimeMs = 200L)
        )

        assertTrue(watcher.hasUserTouchedSince(100L))
    }

    @Test
    fun hasUserTouchedSince_userStylusTouch_returnsTrue() {
        watcher.onWatcherTouchEvent(
            touchEvent(MotionEvent.ACTION_OUTSIDE, USER_DEVICE_ID, MotionEvent.TOOL_TYPE_STYLUS, eventTimeMs = 200L)
        )

        assertTrue(watcher.hasUserTouchedSince(100L))
    }

    @Test
    fun hasUserTouchedSince_injectedTouch_returnsFalse() {
        // Android 12 and above, and Android 11
        watcher.onWatcherTouchEvent(
            touchEvent(MotionEvent.ACTION_OUTSIDE, VIRTUAL_DEVICE_ID, MotionEvent.TOOL_TYPE_UNKNOWN, eventTimeMs = 200L)
        )
        // Android 10 and below
        watcher.onWatcherTouchEvent(
            touchEvent(MotionEvent.ACTION_OUTSIDE, LEGACY_INJECTED_DEVICE_ID, MotionEvent.TOOL_TYPE_UNKNOWN, eventTimeMs = 200L)
        )

        assertFalse(watcher.hasUserTouchedSince(100L))
    }

    @Test
    fun start_addsOneWatcherWindow_watchingOutsideTouchesWithoutTakingThem() {
        val service = Robolectric.setupService(TestAccessibilityService::class.java)

        watcher.start(service)
        watcher.start(service)

        val windowManager = Shadow.extract<ShadowWindowManagerImpl>(service.getSystemService(WindowManager::class.java))
        val params = windowManager.views.single().layoutParams as WindowManager.LayoutParams
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue("The window must stay visible for input", params.alpha > 0f)
    }

    @Test
    fun stop_removesWatcherWindow() {
        val service = Robolectric.setupService(TestAccessibilityService::class.java)
        watcher.start(service)

        watcher.stop()

        val windowManager = Shadow.extract<ShadowWindowManagerImpl>(service.getSystemService(WindowManager::class.java))
        assertTrue(windowManager.views.isEmpty())
    }

    @Test
    fun watcherWindow_userTouchOutside_isRecorded() {
        val service = Robolectric.setupService(TestAccessibilityService::class.java)
        watcher.start(service)
        val windowManager = Shadow.extract<ShadowWindowManagerImpl>(service.getSystemService(WindowManager::class.java))

        windowManager.views.single()
            .dispatchTouchEvent(userTouch(MotionEvent.ACTION_OUTSIDE, eventTimeMs = 200L))

        assertTrue(watcher.hasUserTouchedSince(100L))
    }

    private fun userTouch(action: Int, eventTimeMs: Long): MotionEvent =
        touchEvent(action, USER_DEVICE_ID, MotionEvent.TOOL_TYPE_FINGER, eventTimeMs)

    private fun touchEvent(action: Int, deviceId: Int, toolType: Int, eventTimeMs: Long): MotionEvent =
        MotionEvent.obtain(
            /* downTime = */ eventTimeMs,
            /* eventTime = */ eventTimeMs,
            /* action = */ action,
            /* pointerCount = */ 1,
            /* pointerProperties = */ arrayOf(MotionEvent.PointerProperties().apply {
                id = 0
                this.toolType = toolType
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

    class TestAccessibilityService : AccessibilityService() {
        override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
        override fun onInterrupt() = Unit
    }
}

private const val USER_DEVICE_ID = 4
/** The virtual device id: given to the injected gestures since Android 12, and to every filtered event on Android 11. */
private const val VIRTUAL_DEVICE_ID = -1
/** The device id given by the system to the gestures injected by an accessibility service up to Android 10. */
private const val LEGACY_INJECTED_DEVICE_ID = 0
