/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.ui.testutils.anyNotNull

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [FullscreenOverlay] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FullscreenOverlayTests {

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param impl the mock called for each abstract method calls.
     */
    class FullscreenOverlayTestImpl(private val impl: FullscreenOverlayImpl) : FullscreenOverlay() {
        override fun onCreateView(layoutInflater: LayoutInflater): View = impl.onCreateView(layoutInflater)
        override fun onViewCreated() = impl.onViewCreated()
        override fun onStart() {
            super.onStart()
            impl.onStart()
        }
        override fun onStop() {
            super.onStop()
            impl.onStop()
        }
        override fun onDestroy() = impl.onDismissed()
    }

    /**
     * Interface to be mocked in order to instantiates an [FullscreenOverlayTestImpl].
     * Calls on abstract members of [BaseOverlay] can be verified on this mock.
     */
    interface FullscreenOverlayImpl {
        fun onCreateView(layoutInflater: LayoutInflater): View
        fun onViewCreated()
        fun onStart()
        fun onStop()
        fun onDismissed()
    }

    @Mock private lateinit var mockView: View
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockWindowManager: WindowManager
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockLayoutInflater: LayoutInflater
    @Mock private lateinit var overlayControllerImpl: FullscreenOverlayImpl

    private lateinit var overlay: FullscreenOverlayTestImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        Mockito.`when`(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        Mockito.`when`(mockContext.getSystemService(LayoutInflater::class.java)).thenReturn(mockLayoutInflater)
        Mockito.`when`(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
        Mockito.`when`(overlayControllerImpl.onCreateView(mockLayoutInflater)).thenReturn(mockView)

        overlay = FullscreenOverlayTestImpl(overlayControllerImpl)
    }

    @Test
    fun start() {
        overlay.create(mockContext)
        Mockito.clearInvocations(overlayControllerImpl)

        overlay.start()

        Mockito.verify(mockWindowManager).addView(eq(mockView), anyNotNull())
        Mockito.verify(overlayControllerImpl).onStart()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onCreateView(anyNotNull())
        Mockito.verify(overlayControllerImpl, Mockito.never()).onViewCreated()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onStop()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onDismissed()
        Assert.assertEquals(Lifecycle.State.STARTED, overlay.lifecycle.currentState)
    }

    @Test
    fun stop() {
        overlay.create(mockContext)
        overlay.start()
        Mockito.clearInvocations(overlayControllerImpl)

        overlay.stop()

        Mockito.verify(mockWindowManager).removeView(eq(mockView))
        Mockito.verify(overlayControllerImpl).onStop()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onCreateView(anyNotNull())
        Mockito.verify(overlayControllerImpl, Mockito.never()).onViewCreated()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onStart()
        Mockito.verify(overlayControllerImpl, Mockito.never()).onDismissed()
        Assert.assertEquals(Lifecycle.State.CREATED, overlay.lifecycle.currentState)
    }
}