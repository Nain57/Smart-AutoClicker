/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4

import junit.framework.TestCase.assertEquals

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any

import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Tests for [ScreenMetrics] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScreenMetricsTests {

    private companion object {
        private const val DISPLAY_SIZE_X = 800
        private const val DISPLAY_SIZE_Y = 600
    }

    private interface OrientationListener {
        fun onOrientationChanged()
    }

    private lateinit var mockContext: Context
    private lateinit var mockDisplay: Display
    private lateinit var mockDisplayManager: DisplayManager
    private lateinit var mockOrientationListener: OrientationListener

    /** The object under tests. */
    private lateinit var screenMetrics: ScreenMetrics

    @Before
    fun setUp() {
        mockContext = Mockito.mock(Context::class.java)
        mockDisplayManager = Mockito.mock(DisplayManager::class.java)
        mockDisplay = Mockito.mock(Display::class.java)
        mockOrientationListener = Mockito.mock(OrientationListener::class.java)

        mockWhen(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        mockWhen(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
        Mockito.doAnswer { invocation ->
            val argument = invocation.arguments[0] as Point
            argument.x = DISPLAY_SIZE_X
            argument.y = DISPLAY_SIZE_Y
            null
        }.`when`(mockDisplay).getRealSize(any())
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)

        screenMetrics = ScreenMetrics(mockContext)
    }

    @Test
    fun getScreenSize() {
        assertEquals(Point(DISPLAY_SIZE_X, DISPLAY_SIZE_Y), screenMetrics.getScreenSize())
    }

    @Test
    fun getOrientation_landscape() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        assertEquals("Invalid orientation for rotation 90",
            Configuration.ORIENTATION_LANDSCAPE,
            screenMetrics.getOrientation()
        )

        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_270)
        assertEquals("Invalid orientation for rotation 270",
            Configuration.ORIENTATION_LANDSCAPE,
            screenMetrics.getOrientation()
        )
    }

    @Test
    fun getOrientation_portrait() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        assertEquals("Invalid orientation for rotation 0",
            Configuration.ORIENTATION_PORTRAIT,
            screenMetrics.getOrientation()
        )

        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_180)
        assertEquals("Invalid orientation for rotation 180",
            Configuration.ORIENTATION_PORTRAIT,
            screenMetrics.getOrientation()
        )
    }

    @Test
    fun orientationChanged() {
        screenMetrics.registerOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        val receiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(receiverCaptor.capture(), any())

        receiverCaptor.value.onReceive(mockContext, Intent())

        verify(mockOrientationListener).onOrientationChanged()
    }

    @Test
    fun orientationChanged_sameOrientation() {
        screenMetrics.registerOrientationListener { mockOrientationListener.onOrientationChanged() }
        val receiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(receiverCaptor.capture(), any())

        receiverCaptor.value.onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }

    @Test
    fun orientationChanged_unregistration() {
        screenMetrics.registerOrientationListener { mockOrientationListener.onOrientationChanged() }
        val receiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(receiverCaptor.capture(), any())

        screenMetrics.unregisterOrientationListener()
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        receiverCaptor.value.onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }
}