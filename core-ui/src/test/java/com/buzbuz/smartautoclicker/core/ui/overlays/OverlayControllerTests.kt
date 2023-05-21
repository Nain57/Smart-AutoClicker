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

/** Test the [OverlayController] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayControllerTests {

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param context the android context.
     * @param impl the mock called for each abstract method calls.
     */
    class OverlayControllerTestImpl(context: Context, private val impl: OverlayControllerImpl? = null) : OverlayController(context) {
        override fun onCreate() { impl?.onCreate() }
        override fun onStart() { impl?.onStart() }
        override fun onStop() { impl?.onStop() }
        override fun onDestroyed() { impl?.onDismissed() }
        fun publicShowSubOverlay(overlayController: OverlayController, hideCurrent: Boolean = false) {
            showSubOverlay(overlayController, hideCurrent)
        }
    }

    /**
     * Interface to be mocked in order to instantiates an [OverlayControllerTestImpl].
     * Calls on abstract members of [OverlayController] can be verified on this mock.
     */
    interface OverlayControllerImpl {
        fun onCreate()
        fun onStart()
        fun onStop()
        fun onDismissed()
    }

    /** Interface to be mocked in order to verify the calls on the dismiss listener. */
    interface DismissListener {
        fun onDismissed()
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var overlayControllerImpl: OverlayControllerImpl
    @Mock private lateinit var dismissListener: DismissListener

    private lateinit var overlayController: OverlayControllerTestImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        Mockito.`when`(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)

        overlayController = OverlayControllerTestImpl(mockContext, overlayControllerImpl)
    }

    @Test
    fun create() {
        overlayController.create()

        val lifecycleOrder = inOrder(overlayControllerImpl)
        lifecycleOrder.verify(overlayControllerImpl).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.CREATED, overlayController.lifecycle.currentState)
    }

    @Test
    fun createAlreadyCreated() {
        overlayController.create()
        clearInvocations(overlayControllerImpl)
        val expectedState = overlayController.lifecycle.currentState

        overlayController.create()

        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlayController.lifecycle.currentState)
    }

    @Test
    fun hide() {
        overlayController.create()
        overlayController.show()
        clearInvocations(overlayControllerImpl)

        overlayController.hide()

        verify(overlayControllerImpl).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.CREATED, overlayController.lifecycle.currentState)
    }

    @Test
    fun hideNotCreated() {
        val expectedState = overlayController.lifecycle.currentState
        overlayController.hide()

        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlayController.lifecycle.currentState)
    }

    @Test
    fun hideAlreadyHidden() {
        overlayController.create()
        overlayController.hide()
        clearInvocations(overlayControllerImpl)
        val expectedState = overlayController.lifecycle.currentState

        overlayController.hide()

        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlayController.lifecycle.currentState)
    }

    @Test
    fun show() {
        overlayController.create()
        overlayController.hide()
        clearInvocations(overlayControllerImpl)

        overlayController.show()

        verify(overlayControllerImpl).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.RESUMED, overlayController.lifecycle.currentState)
    }

    @Test
    fun showNotCreated() {
        val expectedState = overlayController.lifecycle.currentState
        overlayController.show()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlayController.lifecycle.currentState)
    }

    @Test
    fun showAlreadyShown() {
        overlayController.create()
        overlayController.show()
        clearInvocations(overlayControllerImpl)
        val expectedState = overlayController.lifecycle.currentState

        overlayController.show()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(expectedState, overlayController.lifecycle.currentState)
    }

    @Test
    fun dismiss() {
        overlayController.create(dismissListener::onDismissed)
        clearInvocations(overlayControllerImpl)

        overlayController.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl).onDismissed()
        verify(dismissListener).onDismissed()
        assertEquals(Lifecycle.State.DESTROYED, overlayController.lifecycle.currentState)
    }

    @Test
    fun dismissNotCreated() {
        overlayController.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        verify(dismissListener, never()).onDismissed()
        assertEquals(Lifecycle.State.INITIALIZED, overlayController.lifecycle.currentState)
    }

    @Test
    fun dismissNotShown() {
        overlayController.create(dismissListener::onDismissed)
        overlayController.hide()
        clearInvocations(overlayControllerImpl)

        overlayController.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl).onDismissed()
        verify(dismissListener).onDismissed()
        assertEquals(Lifecycle.State.DESTROYED, overlayController.lifecycle.currentState)
    }

    @Test
    fun showSubOverlay() {
        overlayController.create()
        overlayController.show()
        clearInvocations(overlayControllerImpl)
        val subOverlay = OverlayControllerTestImpl(mockContext)

        overlayController.publicShowSubOverlay(subOverlay, true)

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.RESUMED, subOverlay.lifecycle.currentState)
    }

    @Test
    fun showSubOverlayHide() {
        overlayController.create()
        overlayController.show()
        clearInvocations(overlayControllerImpl)
        val subOverlay = OverlayControllerTestImpl(mockContext)

        overlayController.publicShowSubOverlay(subOverlay, true)

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.RESUMED, subOverlay.lifecycle.currentState)
    }

    @Test
    fun showSubOverlayNotCreated() {
        val subOverlay = OverlayControllerTestImpl(mockContext)
        val expectedState = subOverlay.lifecycle.currentState
        overlayController.publicShowSubOverlay(subOverlay)

        assertEquals(expectedState, subOverlay.lifecycle.currentState)
    }

    @Test
    fun subOverlayDismissed() {
        overlayController.create()
        overlayController.show()
        val subOverlay = OverlayControllerTestImpl(mockContext)
        overlayController.publicShowSubOverlay(subOverlay, true)
        clearInvocations(overlayControllerImpl)

        subOverlay.destroy()

        verify(overlayControllerImpl).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.RESUMED, overlayController.lifecycle.currentState)
    }

    @Test
    fun subOverlayDismissedWasHidden() {
        overlayController.create()
        overlayController.show()
        val subOverlay = OverlayControllerTestImpl(mockContext)
        overlayController.publicShowSubOverlay(subOverlay, true)
        clearInvocations(overlayControllerImpl)

        subOverlay.destroy()

        verify(overlayControllerImpl).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl, never()).onDismissed()
        assertEquals(Lifecycle.State.RESUMED, overlayController.lifecycle.currentState)
    }

    @Test
    fun dismissWithSubOverlay() {
        overlayController.create()
        overlayController.show()
        val subOverlay = OverlayControllerTestImpl(mockContext)
        overlayController.publicShowSubOverlay(subOverlay)
        clearInvocations(overlayControllerImpl)

        overlayController.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl).onStop()
        verify(overlayControllerImpl).onDismissed()
        assertEquals(Lifecycle.State.DESTROYED, overlayController.lifecycle.currentState)
        assertEquals(Lifecycle.State.DESTROYED, subOverlay.lifecycle.currentState)
    }

    @Test
    fun dismissWithSubOverlayWasHidden() {
        overlayController.create()
        overlayController.show()
        val subOverlay = OverlayControllerTestImpl(mockContext)
        overlayController.publicShowSubOverlay(subOverlay, true)
        clearInvocations(overlayControllerImpl)

        overlayController.destroy()

        verify(overlayControllerImpl, never()).onStart()
        verify(overlayControllerImpl, never()).onCreate()
        verify(overlayControllerImpl, never()).onStop()
        verify(overlayControllerImpl).onDismissed()
        assertEquals(Lifecycle.State.DESTROYED, overlayController.lifecycle.currentState)
        assertEquals(Lifecycle.State.DESTROYED, subOverlay.lifecycle.currentState)
    }
}