/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4

import junit.framework.TestCase.assertEquals

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock

import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Tests for [DisplayMetrics] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S])
class DisplayMetricsTests {

    private companion object {
        private const val DISPLAY_SIZE_X = 800
        private const val DISPLAY_SIZE_Y = 600
    }

    private interface OrientationListener {
        fun onOrientationChanged()
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockWindowManager: WindowManager
    @Mock private lateinit var mockOrientationListener: OrientationListener

    /** The object under tests. */
    private lateinit var displayMetrics: DisplayMetrics

    /** @return the broadcast receiver registered upon orientation listener registration */
    private fun getBroadcastOrientationReceiver(): BroadcastReceiver {
        val receiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(receiverCaptor.capture(), any())

        return receiverCaptor.value
    }

    @Suppress("DEPRECATION")
    private fun mockLegacyGetDisplaySize(width: Int = DISPLAY_SIZE_X, height: Int = DISPLAY_SIZE_Y) =
        Mockito.doAnswer { invocation ->
            val argument = invocation.arguments[0] as Point
            argument.x = width
            argument.y = height
            null
        }.`when`(mockDisplay).getRealSize(any())

    private fun mockGetDisplaySize(width: Int = DISPLAY_SIZE_X, height: Int = DISPLAY_SIZE_Y) {
        val mockWindowMetrics = Mockito.mock(WindowMetrics::class.java)
        mockWhen(mockWindowManager.currentWindowMetrics).thenReturn(mockWindowMetrics)
        mockWhen(mockWindowMetrics.bounds).thenReturn(Rect(0, 0, width, height))
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockWhen(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        mockWhen(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        mockWhen(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
    }

    @Test
    fun getScreenSize_initial_modern() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals(Point(DISPLAY_SIZE_X, DISPLAY_SIZE_Y), displayMetrics.screenSize)
    }

    @Test
    fun getScreenSize_update_rotated_modern() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockGetDisplaySize(DISPLAY_SIZE_Y, DISPLAY_SIZE_X)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayMetrics.screenSize)
    }

    @Test
    fun getScreenSize_update_notRotated_modern() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockGetDisplaySize(DISPLAY_SIZE_X, DISPLAY_SIZE_Y)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayMetrics.screenSize)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_initial_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals(Point(DISPLAY_SIZE_X, DISPLAY_SIZE_Y), displayMetrics.screenSize)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_update_rotated_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockLegacyGetDisplaySize(DISPLAY_SIZE_Y, DISPLAY_SIZE_X)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayMetrics.screenSize)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_update_notRotated_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockLegacyGetDisplaySize(DISPLAY_SIZE_X, DISPLAY_SIZE_Y)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayMetrics.screenSize)
    }

    @Test
    fun getOrientation_landscape_90() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals("Invalid orientation for rotation 90",
            Configuration.ORIENTATION_LANDSCAPE,
            displayMetrics.orientation
        )
    }

    @Test
    fun getOrientation_landscape_270() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_270)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals("Invalid orientation for rotation 90",
            Configuration.ORIENTATION_LANDSCAPE,
            displayMetrics.orientation
        )
    }

    @Test
    fun getOrientation_portrait_0() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals("Invalid orientation for rotation 0",
            Configuration.ORIENTATION_PORTRAIT,
            displayMetrics.orientation
        )
    }

    @Test
    fun getOrientation_portrait_180() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_180)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)

        assertEquals("Invalid orientation for rotation 180",
            Configuration.ORIENTATION_PORTRAIT,
            displayMetrics.orientation
        )
    }

    @Test
    fun orientationChanged() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        verify(mockOrientationListener).onOrientationChanged()
    }

    @Test
    fun orientationChanged_sameOrientation() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)

        displayMetrics.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }

    @Test
    fun orientationChanged_unregistration() {
        mockGetDisplaySize()
        displayMetrics = DisplayMetrics(mockContext)
        displayMetrics.startMonitoring(mockContext)
        val listener: (Context) -> Unit = { mockOrientationListener.onOrientationChanged() }

        displayMetrics.addOrientationListener(listener)
        val receiver = getBroadcastOrientationReceiver()

        displayMetrics.removeOrientationListener(listener)
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        receiver.onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }
}