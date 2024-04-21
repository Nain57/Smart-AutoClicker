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

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [BaseOverlay] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BaseOverlayTests {

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param impl the mock called for each abstract method calls.
     */
    class BaseOverlayTestImpl(private val impl: BaseOverlayImpl? = null) : BaseOverlay() {
        override fun onCreate() { impl?.onCreate() }
        override fun onStart() { impl?.onStart() }
        override fun onStop() { impl?.onStop() }
        override fun onDestroy() { impl?.onDismissed() }
    }

    /**
     * Interface to be mocked in order to instantiates an [BaseOverlayTestImpl].
     * Calls on abstract members of [BaseOverlay] can be verified on this mock.
     */
    interface BaseOverlayImpl {
        fun onCreate()
        fun onStart()
        fun onStop()
        fun onDismissed()
    }

    /** Interface to be mocked in order to verify the calls on the dismiss listener. */
    interface DismissListener {
        fun onDismissed(context: Context, overlay: Overlay)
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var overlayControllerImpl: BaseOverlayImpl
    @Mock private lateinit var dismissListener: DismissListener

    private lateinit var overlay: BaseOverlayTestImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        Mockito.`when`(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)

        overlay = BaseOverlayTestImpl(overlayControllerImpl)
    }

    @Test
    fun create() {
        overlay.create(mockContext)

        val lifecycleOrder = inOrder(overlayControllerImpl)
        lifecycleOrder.verify(overlayControllerImpl).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.CREATED, overlay.lifecycle.currentState)
    }

    @Test
    fun createAlreadyCreated() {
        overlay.create(mockContext)
        clearInvocations(overlayControllerImpl)
        val expectedState = overlay.lifecycle.currentState

        overlay.create(mockContext)

        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlay.lifecycle.currentState)
    }

    @Test
    fun hide() {
        overlay.create(mockContext)
        overlay.start()
        clearInvocations(overlayControllerImpl)

        overlay.stop()

        verify(overlayControllerImpl).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.CREATED, overlay.lifecycle.currentState)
    }

    @Test
    fun hideNotCreated() {
        val expectedState = overlay.lifecycle.currentState
        overlay.stop()

        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlay.lifecycle.currentState)
    }

    @Test
    fun hideAlreadyHidden() {
        overlay.create(mockContext)
        overlay.stop()
        clearInvocations(overlayControllerImpl)
        val expectedState = overlay.lifecycle.currentState

        overlay.stop()

        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlay.lifecycle.currentState)
    }

    @Test
    fun show() {
        overlay.create(mockContext)
        overlay.stop()
        clearInvocations(overlayControllerImpl)

        overlay.start()

        verify(overlayControllerImpl).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.STARTED, overlay.lifecycle.currentState)
    }

    @Test
    fun showNotCreated() {
        val expectedState = overlay.lifecycle.currentState
        overlay.start()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlay.lifecycle.currentState)
    }

    @Test
    fun showAlreadyShown() {
        overlay.create(mockContext)
        overlay.start()
        clearInvocations(overlayControllerImpl)
        val expectedState = overlay.lifecycle.currentState

        overlay.start()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlay.lifecycle.currentState)
    }

    @Test
    fun dismiss() {
        overlay.create(mockContext, dismissListener::onDismissed)
        clearInvocations(overlayControllerImpl)

        overlay.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl).onDismissed()
        verify(dismissListener).onDismissed(mockContext, overlay)
        assertEquals(Lifecycle.State.DESTROYED, overlay.lifecycle.currentState)
    }

    @Test
    fun dismissNotCreated() {
        overlay.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        verify(dismissListener, never()).onDismissed(mockContext, overlay)
        assertEquals(Lifecycle.State.INITIALIZED, overlay.lifecycle.currentState)
    }

    @Test
    fun dismissNotShown() {
        overlay.create(mockContext, dismissListener::onDismissed)
        overlay.stop()
        clearInvocations(overlayControllerImpl)

        overlay.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl).onDismissed()
        verify(dismissListener).onDismissed(mockContext, overlay)
        assertEquals(Lifecycle.State.DESTROYED, overlay.lifecycle.currentState)
    }
}