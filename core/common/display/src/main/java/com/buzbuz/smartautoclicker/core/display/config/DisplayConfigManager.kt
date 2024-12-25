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
package com.buzbuz.smartautoclicker.core.display.config

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

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver
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
class DisplayConfigManager @Inject constructor(
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
    private val configChangedReceiver = object : SafeBroadcastReceiver(IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)) {
        override fun onReceive(context: Context, intent: Intent) {
            onAndroidConfigurationChanged(context)
        }
    }

    var displayConfig: DisplayConfig = getCurrentDisplayConfig()
        private set

    /** Start the monitoring of the screen metrics. */
    fun startMonitoring(context: Context) {
        displayConfig = applyInfoPatches(
            current = displayConfig,
            configToPatch = getCurrentDisplayConfig(),
        )

        configChangedReceiver.register(context)
    }

    /** Stop the monitoring of the screen metrics. All listeners will be unregistered. */
    fun stopMonitoring() {
        configChangedReceiver.unregister()
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

    private fun onAndroidConfigurationChanged(context: Context) {
        Log.i(TAG, "onAndroidConfigurationChanged")

        val newConfigPatched = applyInfoPatches(
            current = displayConfig,
            configToPatch = getCurrentDisplayConfig(),
        )

        if (newConfigPatched == displayConfig) {
            Log.i(TAG, "Same DisplayConfig, skip update")
            return
        }

        Log.i(TAG, "New DisplayConfig: $newConfigPatched")
        val orientationChanged = displayConfig.orientation != newConfigPatched.orientation
        displayConfig = newConfigPatched

        // Notify for orientation changes if needed
        if (orientationChanged) orientationListeners.forEach { it.invoke(context) }
    }

    private fun getCurrentDisplayConfig(): DisplayConfig =
        DisplayConfig(
            sizePx = getCurrentDisplaySize(),
            orientation = getCurrentDisplayOrientation(),
            safeInsetTopPx = getCurrentDisplaySafeInsetTop(),
            roundedCorners = buildMap {
                put(Corner.TOP_LEFT, getCurrentDisplayRoundedCorner(Corner.TOP_LEFT))
                put(Corner.TOP_RIGHT, getCurrentDisplayRoundedCorner(Corner.TOP_RIGHT))
                put(Corner.BOTTOM_LEFT, getCurrentDisplayRoundedCorner(Corner.BOTTOM_LEFT))
                put(Corner.BOTTOM_RIGHT, getCurrentDisplayRoundedCorner(Corner.BOTTOM_RIGHT))
            }
        )

    /**  @return the orientation of the screen. */
    private fun getCurrentDisplayOrientation(): Int = when (display.rotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> Configuration.ORIENTATION_PORTRAIT
        Surface.ROTATION_90, Surface.ROTATION_270 -> Configuration.ORIENTATION_LANDSCAPE
        else -> Configuration.ORIENTATION_UNDEFINED
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

    private fun getCurrentDisplaySafeInsetTop(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) display.cutout?.safeInsetTop ?: 0 else 0

    /** @return the rounded corner of the given position. Returns null if there is none. */
    private fun getCurrentDisplayRoundedCorner(corner: Corner): DisplayRoundedCorner? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            windowManager.currentWindowMetrics.windowInsets
                .getRoundedCorner(corner.toAndroidApiValue())?.let { cornerInfo ->
                    DisplayRoundedCorner(centerPx = cornerInfo.center, radiusPx = cornerInfo.radius)
                }
        } else null

    /**
     * Some devices doesn't interpret the Android API documentation the same way, leading to different implementations
     * across devices.
     * This method tries to unify the different behaviours encountered to get the same information for all devices.
     *
     * @param current the display configuration currently in application.
     * @param configToPatch the new display configuration to be patched.
     *
     * @return the display configuration patched
     */
    private fun applyInfoPatches(current: DisplayConfig, configToPatch: DisplayConfig): DisplayConfig {
        // Some devices changes the display value with rotation, some does not.
        // If the orientation have changed since the last configuration update, the display size should have changed
        val patchedSize =
            if (current.orientation != configToPatch.orientation && current.sizePx == configToPatch.sizePx) {
                Point(configToPatch.sizePx.y, configToPatch.sizePx.x)
            } else configToPatch.sizePx

        return configToPatch.copy(
            sizePx = patchedSize,
        )
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DisplayMetrics:")
            append(contentPrefix, displayConfig)
            append(contentPrefix, display)
            append(contentPrefix).append("DisplaySize: ${getCurrentDisplaySize()}").println()
        }
    }
}

/** Tag for logs */
private const val TAG = "DisplayMetrics"