/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.base.workarounds

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build

// On Pixel devices on Android 15, the InputDispatcher can mess up and block all touch input
// Google tacker issue: https://issuetracker.google.com/issues/384188031
// A workaround is to inject a multi tap to unblock to InputDispatcher

fun isImpactedByInputBlock(): Boolean =
    Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM
            && Build.BRAND == GOOGLE_DEVICE_BRAND
            && Build.MODEL.lowercase().contains(GOGGLE_MODE_PIXEL)


fun GestureDescription.Builder.buildUnblockGesture(): GestureDescription =
    addStroke(createUnblockClickStroke(1f, 1f))
        .addStroke(createUnblockClickStroke(1f, 3f))
        .addStroke(createUnblockClickStroke(2f, 2f))
        .build()

private fun createUnblockClickStroke(posX: Float, posY: Float): GestureDescription.StrokeDescription =
    GestureDescription.StrokeDescription(
        Path().apply { moveTo(posX, posY) },
        0L,
        1L,
    )


class UnblockGestureScheduler {

    private var lastUnblockTimeMs = System.currentTimeMillis()

    fun shouldTrigger(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime >= (lastUnblockTimeMs + UNBLOCK_GESTURE_DELAY_MS)) {
            lastUnblockTimeMs = currentTime
            return true
        }

        return false
    }
}

private const val GOOGLE_DEVICE_BRAND = "google"
private const val GOGGLE_MODE_PIXEL = "pixel"

private const val UNBLOCK_GESTURE_DELAY_MS = 10000L