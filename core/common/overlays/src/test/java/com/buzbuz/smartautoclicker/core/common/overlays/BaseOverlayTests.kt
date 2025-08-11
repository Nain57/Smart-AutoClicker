
package com.buzbuz.smartautoclicker.core.common.overlays

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
    class BaseOverlayTestImpl(private val impl: BaseOverlayImpl? = null) : com.buzbuz.smartautoclicker.core.common.overlays.base.BaseOverlay() {
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
        fun onDismissed(context: Context, overlay: com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay)
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