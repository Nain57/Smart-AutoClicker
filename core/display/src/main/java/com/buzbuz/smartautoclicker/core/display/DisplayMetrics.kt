/*
 * Copyright (C) 2024 Kevin Buzeau
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
@file:Suppress("DEPRECATION")
package com.buzbuz.smartautoclicker.core.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides metrics for the screen such as orientation or display size.
 * In order for the metrics to be updated upon screen rotation, you must call [startMonitoring] first. Once the metrics
 * are no longer needed, call [stopMonitoring] to release all resources.
 */
@Singleton
class DisplayMetrics @Inject constructor(
    @ApplicationContext context: Context,
): Dumpable {

    /** The Android window manager. */
    private val windowManager = context.getSystemService(WindowManager::class.java)
    /** The Android display manager. */
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    /** The display to get the value from. It will always be the first one available. */
    private val display = displayManager.getDisplay(0)

    /** The listeners upon orientation changes. */
    private val orientationListeners: MutableSet<((Context) -> Unit)> = mutableSetOf()

    /** Listen to the configuration changes and calls [orientationListeners] when needed. */
    private val configChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "onConfigurationChanged")
            if (updateScreenConfig()) {
                orientationListeners.forEach { it.invoke(context) }
            }
        }
    }

    val safeInsetTop: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) display.cutout?.safeInsetTop ?: 0 else 0

    /** The orientation of the display. */
    var orientation: Int = Configuration.ORIENTATION_UNDEFINED
        private set
    /** The screen size. */
    var screenSize: Point = Point(0, 0)
        private set

    init {
        updateScreenConfig()
    }

    /** Start the monitoring of the screen metrics. */
    fun startMonitoring(context: Context) {
        updateScreenConfig()

        ContextCompat.registerReceiver(
            context,
            configChangedReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /** Stop the monitoring of the screen metrics. All listeners will be unregistered. */
    fun stopMonitoring(context: Context) {
        context.unregisterReceiver(configChangedReceiver)
        orientationListeners.clear()
    }

    /**
     * Register a new orientation listener.
     *
     * @param listener the listener to be registered.
     */
    fun addOrientationListener(listener: (Context) -> Unit) {
        orientationListeners.add(listener)
    }

    /** Unregister a previously registered listener. */
    fun removeOrientationListener(listener: (Context) -> Unit) {
        orientationListeners.remove(listener)
    }

    /** @return true if the screen config have changed, false if not. */
    private fun updateScreenConfig(): Boolean {
        val newOrientation = getCurrentOrientation()
        if (newOrientation == orientation) return false

        val newSize = getCurrentDisplaySize()
        orientation = newOrientation
        screenSize = if (newSize == screenSize) {
            Point(newSize.y, newSize.x)
        } else {
            newSize
        }

        Log.i(TAG, "Screen config updated: ScreenSize=$screenSize Orientation=$orientation")

        return true
    }

    private fun getCurrentDisplaySize(): Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.let { windowBound ->
                Point(windowBound.width(), windowBound.height())
            }
        } else {
            val realSize = Point()
            display.getRealSize(realSize)
            realSize
        }

    /**  @return the orientation of the screen. */
    private fun getCurrentOrientation(): Int = when (display.rotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> Configuration.ORIENTATION_PORTRAIT
        Surface.ROTATION_90, Surface.ROTATION_270 -> Configuration.ORIENTATION_LANDSCAPE
        else -> Configuration.ORIENTATION_UNDEFINED
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DisplayMetrics:")
            append(contentPrefix)
                .append("- topInset=$safeInsetTop; ")
                .append("orientation=${orientation.toOrientationString()}; ")
                .append("screenSize=$screenSize; ")
                .println()
            append(contentPrefix)
                .append("- displayRotation=${display.rotation.toDisplayRotationString()}; ")
                .append("displaySize=${getCurrentDisplaySize()}; ")
                .println()
        }
    }

    private fun Int?.toOrientationString(): String = when (this) {
        Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
        Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
        else -> "UNDEFINED"
    }

    private fun Int?.toDisplayRotationString(): String = when (this) {
        Surface.ROTATION_0 -> "ROTATION_0"
        Surface.ROTATION_180 -> "ROTATION_180"
        Surface.ROTATION_90 -> "ROTATION_90"
        Surface.ROTATION_270 -> "ROTATION_270"
        else -> "UNDEFINED"
    }
}

/** Tag for logs */
private const val TAG = "DisplayMetrics"