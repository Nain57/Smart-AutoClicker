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
package com.buzbuz.smartautoclicker.baseui.dialog

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.baseui.utils.anyNotNull
import com.google.android.material.bottomsheet.BottomSheetDialog

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

/** Test the [OverlayDialogController] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayDialogControllerTests {

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param context the android context.
     * @param impl the mock called for each abstract method calls.
     */
    class OverlayDialogControllerTestImpl(context: Context, private val impl: OverlayDialogControllerImpl) : OverlayDialogController(context) {
        override fun onCreateView(): BottomSheetDialog = impl.onCreateDialog()
        override fun onDialogCreated(dialog: BottomSheetDialog) = impl.onDialogCreated(dialog)
        override fun onVisibilityChanged(visible: Boolean): Unit = impl.onVisibilityChanged(visible)
        fun publicGetDialog() = dialog
        fun publicChangeButtonState(button: Button, visibility: Int, textId: Int = -1, listener: View.OnClickListener? = null) {
            changeButtonState(button, visibility, textId, listener)
        }
    }

    /**
     * Interface to be mocked in order to instantiates an [OverlayDialogControllerTestImpl].
     * Calls on abstract members of [OverlayDialogController] can be verified on this mock.
     */
    interface OverlayDialogControllerImpl {
        fun onCreateDialog(): BottomSheetDialog
        fun onDialogCreated(dialog: BottomSheetDialog)
        fun onVisibilityChanged(visible: Boolean)
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockInputMethodManager: InputMethodManager
    @Mock private lateinit var mockDialog: BottomSheetDialog
    @Mock private lateinit var mockDialogWindow: Window
    @Mock private lateinit var mockDialogDecorView: View
    @Mock private lateinit var mockDialogWindowToken: IBinder
    @Mock private lateinit var overlayDialogControllerImpl: OverlayDialogControllerImpl

    /** The object under tests. */
    private lateinit var overlayDialogController: OverlayDialogControllerTestImpl

    /** Clear any previous mockito invocation on all mocks. */
    private fun clearMockitoInvocations() = clearInvocations(mockContext, mockInputMethodManager,
        mockDialog, mockDialogWindow, mockDialogDecorView, mockDialogWindowToken, overlayDialogControllerImpl)

    /** Verify that there is no interaction with any of the mocks in this class. */
    private fun verifyNoMocksInteractions() = verifyNoInteractions(mockContext, mockInputMethodManager,
        mockDialog, mockDialogWindow, mockDialogDecorView, mockDialogWindowToken, overlayDialogControllerImpl)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock Android managers
        mockWhen(mockContext.getSystemService(InputMethodManager::class.java)).thenReturn(mockInputMethodManager)

        // Mock dialog builder provided by tested class implementation
        mockWhen(overlayDialogControllerImpl.onCreateDialog()).thenReturn(mockDialog)
        mockWhen(mockDialog.window).thenReturn(mockDialogWindow)
        mockWhen(mockDialogWindow.decorView).thenReturn(mockDialogDecorView)
        mockWhen(mockDialogDecorView.windowToken).thenReturn(mockDialogWindowToken)

        overlayDialogController = OverlayDialogControllerTestImpl(mockContext, overlayDialogControllerImpl)
    }

    @Test
    fun createLifecycle() {
        overlayDialogController.create()

        inOrder(overlayDialogControllerImpl, mockDialog).apply {
            verify(overlayDialogControllerImpl).onCreateDialog()
            verify(mockDialog).show()
            verify(overlayDialogControllerImpl).onDialogCreated(anyNotNull())
        }
        verify(overlayDialogControllerImpl, never()).onVisibilityChanged(anyBoolean())
    }

    @Test
    fun createDialogBuilderSetup() {
        overlayDialogController.create()

        verify(mockDialog).setOnDismissListener(anyNotNull())
        verify(mockDialog).setCancelable(false)
        verify(mockDialog).create()
    }

    @Test
    fun createDialogSetup() {
        overlayDialogController.create()

        verify(mockDialogWindow).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        verify(mockDialogWindow).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        verify(mockDialogDecorView).setOnTouchListener(anyNotNull())
    }

    @Test
    fun createDialogShow() {
        overlayDialogController.create()
        verify(mockDialog).show()
    }

    @Test
    fun dialogCreated() {
        overlayDialogController.create()
        verify(overlayDialogControllerImpl).onDialogCreated(mockDialog)
        assertEquals(mockDialog, overlayDialogController.publicGetDialog())
    }

    @Test
    fun hideFromShown() {
        overlayDialogController.create()
        clearMockitoInvocations()

        overlayDialogController.hide(true)

        verify(mockInputMethodManager).hideSoftInputFromWindow(mockDialogWindowToken, 0)
        verify(mockDialog).hide()
        verify(overlayDialogControllerImpl).onVisibilityChanged(false)
    }

    @Test
    fun hideFromHidden() {
        overlayDialogController.create()
        overlayDialogController.hide()
        clearMockitoInvocations()

        overlayDialogController.hide()

        verifyNoMocksInteractions()
    }

    @Test
    fun showFromShown() {
        overlayDialogController.create()
        clearMockitoInvocations()

        overlayDialogController.show()

        verifyNoMocksInteractions()
    }

    @Test
    fun showFromHidden() {
        overlayDialogController.create()
        overlayDialogController.hide(true)
        clearMockitoInvocations()

        overlayDialogController.show()

        verify(mockDialog).show()
        verify(overlayDialogControllerImpl).onVisibilityChanged(true)
    }

    @Test
    fun dismiss() {
        overlayDialogController.create()
        clearMockitoInvocations()

        overlayDialogController.destroy()

        verify(mockDialog).dismiss()
    }

    @Test
    fun decorViewTouchHideKeyboard() {
        overlayDialogController.create()

        val decorViewTouchListener = ArgumentCaptor.forClass(View.OnTouchListener::class.java)
        verify(mockDialogDecorView).setOnTouchListener(decorViewTouchListener.capture())
        decorViewTouchListener.value.onTouch(mock(View::class.java), mock(MotionEvent::class.java))

        verify(mockInputMethodManager).hideSoftInputFromWindow(mockDialogWindowToken, 0)
    }

    @Test
    fun changeButtonVisible() {
        overlayDialogController.create()

        val button = mock(Button::class.java)
        overlayDialogController.publicChangeButtonState(button, View.VISIBLE)

        verify(button).visibility = View.VISIBLE
        verify(button).isEnabled = true
        verify(button).setOnClickListener(null)
        verify(button, never()).setText(anyInt())
    }

    @Test
    fun changeButtonVisibleAllParams() {
        overlayDialogController.create()

        val textId = 42
        val clickListener = mock(View.OnClickListener::class.java)
        val button = mock(Button::class.java)
        overlayDialogController.publicChangeButtonState(button, View.VISIBLE, textId, clickListener)

        verify(button).visibility = View.VISIBLE
        verify(button).isEnabled = true
        verify(button).setOnClickListener(clickListener)
        verify(button).setText(textId)
    }

    @Test
    fun changeButtonInvisible() {
        overlayDialogController.create()

        val button = mock(Button::class.java)
        overlayDialogController.publicChangeButtonState(button, View.INVISIBLE)

        verify(button).visibility = View.VISIBLE
        verify(button).isEnabled = false
        verify(button, never()).setText(anyInt())
    }

    @Test
    fun changeButtonInvisibleAllParams() {
        overlayDialogController.create()

        val textId = 42
        val button = mock(Button::class.java)
        overlayDialogController.publicChangeButtonState(button, View.INVISIBLE, textId)

        verify(button).visibility = View.VISIBLE
        verify(button).isEnabled = false
        verify(button).setText(textId)
    }

    @Test
    fun changeButtonGone() {
        overlayDialogController.create()

        val button = mock(Button::class.java)
        overlayDialogController.publicChangeButtonState(button, View.GONE)

        verify(button).visibility = View.GONE
    }
}