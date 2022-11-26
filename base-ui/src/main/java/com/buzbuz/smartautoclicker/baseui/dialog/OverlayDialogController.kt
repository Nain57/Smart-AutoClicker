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
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.coordinatorlayout.widget.CoordinatorLayout

import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.baseui.ScreenMetrics

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Controller for a dialog opened from a service as an overlay.
 *
 * This class ensure that all dialogs opened from a service will have the same behaviour. It provides basic lifecycle
 * alike methods to ease the view initialization/cleaning, as well as a back stack management with other
 * [OverlayController] opened through the [showSubOverlay] methods, allowing to open another dialog from this one and
 * easily go back to this one once the sub dialog is closed.
 */
abstract class OverlayDialogController(
    context: Context,
    @StyleRes theme: Int,
) : OverlayController(context, theme, recreateOnRotation = true) {

    /** The Android InputMethodManger, for ensuring the keyboard dismiss on dialog dismiss. */
    private val inputMethodManager: InputMethodManager = context.getSystemService(InputMethodManager::class.java)
    /** Touch listener hiding the software keyboard and propagating the touch event normally. */
    protected val hideSoftInputTouchListener = { view: View, _: MotionEvent ->
        hideSoftInput()
        view.performClick()
    }

    /** Tells if the dialog is visible. */
    private var isShowing = false

    /**
     * The dialog currently displayed by this controller.
     * Null until [onDialogCreated] is called, or if it has been dismissed.
     */
    protected var dialog: BottomSheetDialog? = null
        private set

    /**
     * The coordinator layout of the dialog.
     * Null until [onDialogCreated] is called, or if the dialog has been dismissed.
     */
    protected var dialogCoordinatorLayout: CoordinatorLayout? = null
        private set

    /**
     * Creates the dialog shown by this controller.
     * Note that the cancelable value and the dismiss listener will be overridden with internal values once, so any
     * values for them defined here will not be kept.
     *
     * @return the builder for the dialog to be created.
     */
    protected abstract fun onCreateView(): ViewGroup

    /**
     * Setup the dialog view.
     * Called once the dialog is created and first show, it allows the implementation to initialize the content views.
     *
     * @param dialog the newly created dialog.
     */
    protected abstract fun onDialogCreated(dialog: BottomSheetDialog)

    final override fun onCreate() {
        dialog = BottomSheetDialog(context).apply {
            val view = onCreateView()

            setContentView(view)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@OverlayDialogController.destroy()
                    true
                } else {
                    false
                }
            }
            create()

            window?.apply {
                setType(ScreenMetrics.TYPE_COMPAT_OVERLAY)
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                decorView.setOnTouchListener(hideSoftInputTouchListener)
            }

            dialogCoordinatorLayout = (view.parent.parent as CoordinatorLayout)

            if (screenMetrics.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                behavior.apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    isDraggable = false

                }
            }
        }

        onDialogCreated(dialog!!)
    }

    @CallSuper
    override fun onStart() {
        if (isShowing) return

        isShowing = true
        dialog?.show()
    }

    @CallSuper
    override fun onStop() {
        if (!isShowing) return

        hideSoftInput()
        dialog?.hide()
        isShowing = false
    }

    @CallSuper
    override fun onDestroyed() {
        dialog?.dismiss()
        dialog = null
    }

    final override fun showSubOverlay(overlayController: OverlayController, hideCurrent: Boolean) {
        super.showSubOverlay(overlayController, true)
        hideSoftInput()
    }

    /**
     * Update the selected button display.
     *
     * @param button the button to be updated.
     * @param visibility the new button visibility.
     * @param textId the string resource identifier for the text of the button.
     * @param listener the new click listener of the button. Can be null if none is needed.
     */
    protected fun changeButtonState(button: Button, visibility: Int, textId: Int = -1, listener: View.OnClickListener? = null) {
        button.apply {
            when (visibility) {
                View.VISIBLE -> {
                    this.visibility = View.VISIBLE
                    if (textId != -1) {
                        setText(textId)
                    }
                    setOnClickListener(listener)
                    isEnabled = true
                }
                View.INVISIBLE -> {
                    this.visibility = View.VISIBLE
                    if (textId != -1) {
                        setText(textId)
                    }
                    isEnabled = false
                }
                View.GONE -> {
                    this.visibility = View.GONE
                }
            }
        }
    }

    /** Hide the software keyboard. */
    private fun hideSoftInput() {
        dialog?.let {
            inputMethodManager.hideSoftInputFromWindow(it.window!!.decorView.windowToken, 0)
        }
    }
}