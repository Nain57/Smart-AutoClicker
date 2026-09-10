/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.os.Build
import android.view.View

import androidx.test.core.app.ApplicationProvider

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import java.util.concurrent.TimeUnit

/** Ensures an animation completion cancels its delayed fallback callback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayMenuAnimationsTests {

    @Test
    fun showAnimation_completionDoesNotAlsoRunDelayedTimeout() {
        val animations = OverlayMenuAnimations()
        val view = View(ApplicationProvider.getApplicationContext())
        var completionCount = 0

        animations.startShowAnimation(view) { completionCount++ }

        shadowOf(android.os.Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        assertEquals(1, completionCount)

        shadowOf(android.os.Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertEquals(1, completionCount)
    }

    @Test
    fun hideAnimation_completionDoesNotAlsoRunDelayedTimeout() {
        val animations = OverlayMenuAnimations()
        val view = View(ApplicationProvider.getApplicationContext())
        var completionCount = 0

        animations.startHideAnimation(view) { completionCount++ }

        shadowOf(android.os.Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        assertEquals(1, completionCount)

        shadowOf(android.os.Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertEquals(1, completionCount)
    }
}
