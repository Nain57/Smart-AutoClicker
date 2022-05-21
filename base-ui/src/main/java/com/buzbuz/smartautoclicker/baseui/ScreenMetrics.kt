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
@file:Suppress("DEPRECATION")
package com.buzbuz.smartautoclicker.baseui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager

/**
 * Provides metrics for the screen such as orientation or display size.
 *
 * @param context the Android context.
 */
class ScreenMetrics(private val context: Context) {

    companion object {
        /** WindowManager LayoutParams type for a window over applications. */
        @JvmField
        val TYPE_COMPAT_OVERLAY =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
    }

    /** The Android display manager. */
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    /** The display to get the value from. It will always be the first one available. */
    private val display = displayManager.getDisplay(0)
    /** The listener upon orientation changes. */
    private var orientationListener: ((Context) -> Unit)? = null

    /** Listen to the configuration changes and calls [orientationListener] when needed. */
    private val configChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (updateScreenConfig()) {
                orientationListener?.invoke(context)
            }
        }
    }

    /** The orientation of the display. */
    var orientation: Int = -1
        private set
    /** The screen size. */
    var screenSize: Point = Point(0, 0)
        private set

    init { updateScreenConfig() }

    /**
     * Register a new orientation listener.
     * If a previous listener was registered, the new one will replace it.
     *
     * @param listener the listener to be registered.
     */
    fun registerOrientationListener(listener: (Context) -> Unit) {
        if (listener == orientationListener) {
            return
        }

        unregisterOrientationListener()
        orientationListener = listener
        context.registerReceiver(configChangedReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
    }

    /** Unregister a previously registered listener. */
    fun unregisterOrientationListener() {
        orientationListener?.let {
            context.unregisterReceiver(configChangedReceiver)
            orientationListener = null
        }
    }

    /** @return true if the screen config have changed, false if not. */
    private fun updateScreenConfig(): Boolean {
        val newOrientation = getCurrentOrientation()
        if (newOrientation == orientation) return false

        val newSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds.let { windowBound ->
                Point(windowBound.width(), windowBound.height())
            }
        } else {
            val realSize = Point()
            display.getRealSize(realSize)
            realSize
        }

        orientation = newOrientation
        screenSize = if (newSize == screenSize) {
            Point(newSize.y, newSize.x)
        } else {
            newSize
        }

        return true
    }

    /**  @return the orientation of the screen. */
    private fun getCurrentOrientation(): Int = when (display.rotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> Configuration.ORIENTATION_PORTRAIT
        Surface.ROTATION_90, Surface.ROTATION_270 -> Configuration.ORIENTATION_LANDSCAPE
        else -> Configuration.ORIENTATION_UNDEFINED
    }
}