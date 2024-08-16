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
import android.view.RoundedCorner
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics

import androidx.test.ext.junit.runners.AndroidJUnit4

import junit.framework.TestCase.assertEquals

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock

import org.mockito.Mockito
import org.mockito.Mockito.eq
import org.mockito.Mockito.isNull
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Tests for [DisplayConfigManager] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S])
class DisplayConfigManagerTests {

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
    private lateinit var displayConfigManager: DisplayConfigManager

    /** @return the broadcast receiver registered upon orientation listener registration */
    private fun getBroadcastOrientationReceiver(): BroadcastReceiver {
        val receiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(receiverCaptor.capture(), any(), isNull(), isNull(), eq(0))

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

    private fun mockCurrentWindowMetrics(width: Int = DISPLAY_SIZE_X, height: Int = DISPLAY_SIZE_Y) {
        val mockWindowMetrics = Mockito.mock(WindowMetrics::class.java)
        mockWhen(mockWindowManager.currentWindowMetrics).thenReturn(mockWindowMetrics)
        mockWhen(mockWindowMetrics.bounds).thenReturn(Rect(0, 0, width, height))

        val mockWindowInsets = Mockito.mock(WindowInsets::class.java)
        val mockRoundedCorner = Mockito.mock(RoundedCorner::class.java)
        mockWhen(mockWindowMetrics.windowInsets).thenReturn(mockWindowInsets)
        mockWhen(mockWindowInsets.getRoundedCorner(anyInt())).thenReturn(mockRoundedCorner)
        mockWhen(mockRoundedCorner.center).thenReturn(Point(0, 0))
        mockWhen(mockRoundedCorner.radius).thenReturn(5)
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
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals(Point(DISPLAY_SIZE_X, DISPLAY_SIZE_Y), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    fun getScreenSize_update_rotated_modern() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockCurrentWindowMetrics(DISPLAY_SIZE_Y, DISPLAY_SIZE_X)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    fun getScreenSize_update_notRotated_modern() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockCurrentWindowMetrics(DISPLAY_SIZE_X, DISPLAY_SIZE_Y)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_initial_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals(Point(DISPLAY_SIZE_X, DISPLAY_SIZE_Y), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_update_rotated_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockLegacyGetDisplaySize(DISPLAY_SIZE_Y, DISPLAY_SIZE_X)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun getScreenSize_update_notRotated_legacy() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockLegacyGetDisplaySize()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockLegacyGetDisplaySize(DISPLAY_SIZE_X, DISPLAY_SIZE_Y)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        assertEquals(Point(DISPLAY_SIZE_Y, DISPLAY_SIZE_X), displayConfigManager.displayConfig.sizePx)
    }

    @Test
    fun getOrientation_landscape_90() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals("Invalid orientation for rotation 90",
            Configuration.ORIENTATION_LANDSCAPE,
            displayConfigManager.displayConfig.orientation,
        )
    }

    @Test
    fun getOrientation_landscape_270() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_270)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals("Invalid orientation for rotation 90",
            Configuration.ORIENTATION_LANDSCAPE,
            displayConfigManager.displayConfig.orientation,
        )
    }

    @Test
    fun getOrientation_portrait_0() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals("Invalid orientation for rotation 0",
            Configuration.ORIENTATION_PORTRAIT,
            displayConfigManager.displayConfig.orientation,
        )
    }

    @Test
    fun getOrientation_portrait_180() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_180)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)

        assertEquals("Invalid orientation for rotation 180",
            Configuration.ORIENTATION_PORTRAIT,
            displayConfigManager.displayConfig.orientation,
        )
    }

    @Test
    fun orientationChanged() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        verify(mockOrientationListener).onOrientationChanged()
    }

    @Test
    fun orientationChanged_sameOrientation() {
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_0)
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)

        displayConfigManager.addOrientationListener { mockOrientationListener.onOrientationChanged() }
        getBroadcastOrientationReceiver().onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }

    @Test
    fun orientationChanged_unregistration() {
        mockCurrentWindowMetrics()
        displayConfigManager = DisplayConfigManager(mockContext)
        displayConfigManager.startMonitoring(mockContext)
        val listener: (Context) -> Unit = { mockOrientationListener.onOrientationChanged() }

        displayConfigManager.addOrientationListener(listener)
        val receiver = getBroadcastOrientationReceiver()

        displayConfigManager.removeOrientationListener(listener)
        mockWhen(mockDisplay.rotation).thenReturn(Surface.ROTATION_90)
        receiver.onReceive(mockContext, Intent())

        verify(mockOrientationListener, never()).onOrientationChanged()
    }
}