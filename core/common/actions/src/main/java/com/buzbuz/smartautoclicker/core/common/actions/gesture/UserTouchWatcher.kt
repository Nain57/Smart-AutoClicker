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
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.base.extensions.safeAddView
import com.buzbuz.smartautoclicker.core.base.extensions.safeRemoveView

import javax.inject.Inject
import javax.inject.Singleton


/**
 * Watches the user touching the screen, through a one pixel window receiving the touches outside of it.
 *
 * The system silently lifts a pointer held by an accessibility service when the user touches the screen. Up to Android
 * 16, the gesture injector was reset by the touch, and refused any continuation of the held stroke afterwards. Since
 * Android 16 QPR2, the cancellation happens further down the input pipeline and the injector keeps accepting
 * continuations, so the user touches must be observed from the app side to know the pointer has been lost.
 *
 * The window is not touchable, it never takes a touch from the user, but it must stay visible with a non zero alpha:
 * the system only sends ACTION_OUTSIDE to windows visible for input.
 */
@Singleton
internal class UserTouchWatcher @Inject constructor() {

    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    /** The [MotionEvent.getEventTime] of the last touch down from the user, in the uptime base. */
    @Volatile
    private var lastUserTouchDownTimeMs: Long = NO_TOUCH_TIME

    private var windowManager: WindowManager? = null
    private var watcherView: View? = null

    /** Add the watcher window, if not already shown. Can be called from any thread. */
    fun start(service: AccessibilityService): Unit = runOnMainThread {
        if (watcherView != null) return@runOnMainThread

        val wm = service.getSystemService(WindowManager::class.java) ?: let {
            Log.w(TAG, "Can't watch user touches, window manager is not available")
            return@runOnMainThread
        }

        @SuppressLint("ClickableViewAccessibility") // The view only receives touches outside of it
        val view = View(service).apply {
            setOnTouchListener { _, event ->
                onWatcherTouchEvent(event)
                false
            }
        }
        if (!wm.safeAddView(view, watcherLayoutParams())) {
            Log.w(TAG, "Can't watch user touches, watcher window can't be added")
            return@runOnMainThread
        }

        windowManager = wm
        watcherView = view
    }

    /** Remove the watcher window, if shown. Can be called from any thread. */
    fun stop(): Unit = runOnMainThread {
        val view = watcherView ?: return@runOnMainThread
        windowManager?.safeRemoveView(view)

        windowManager = null
        watcherView = null
    }

    /** Tells if the user has touched the screen since the provided time, in the uptime base. */
    fun hasUserTouchedSince(timeMs: Long): Boolean =
        lastUserTouchDownTimeMs >= timeMs

    @VisibleForTesting
    internal fun onWatcherTouchEvent(event: MotionEvent) {
        Log.d(TAG, "DEBUG watcher event: action=${MotionEvent.actionToString(event.action)}, " +
                "deviceId=${event.deviceId}, source=0x${Integer.toHexString(event.source)}, " +
                "toolType=${event.getToolType(0)}, eventTime=${event.eventTime}")
        if (!event.isFromUser()) return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_OUTSIDE -> lastUserTouchDownTimeMs = event.eventTime
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }
}

/**
 * Gestures from an accessibility service are injected with an unknown tool type, on every Android version, while real
 * touches always have one (finger, stylus...).
 *
 * The device id can't be used: on Android 11, every event going through the accessibility input filter, the user ones
 * included, is given the virtual device id (-1) once injected back into the input dispatcher.
 */
private fun MotionEvent.isFromUser(): Boolean =
    pointerCount > 0 && getToolType(0) != MotionEvent.TOOL_TYPE_UNKNOWN

private fun watcherLayoutParams(): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WATCHER_WINDOW_SIZE_PX,
        WATCHER_WINDOW_SIZE_PX,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

private const val WATCHER_WINDOW_SIZE_PX = 1
private const val NO_TOUCH_TIME = -1L
private const val TAG = "UserTouchWatcher"
