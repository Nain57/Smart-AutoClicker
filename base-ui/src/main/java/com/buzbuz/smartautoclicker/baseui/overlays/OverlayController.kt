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
package com.buzbuz.smartautoclicker.baseui.overlays

import android.content.Context
import android.util.Log

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Base class for an overlay based ui providing lifecycle and back stack management.
 *
 * Initialization starts with the [create] method, which will call the correct implementation methods to creates and
 * show the overlay ui object.
 * Back stack management is ensured by the method [showSubOverlay], resuming the parent ui object once the child is
 * dismissed.
 */
abstract class OverlayController(protected val context: Context) : LifecycleOwner {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "OverlayController"
    }

    /** The lifecycle of the ui component controlled by this class */
    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    /** Tells if the overlay is shown. */
    private var isShown: Boolean = false
    /**
     * OverlayController for an overlay shown from this OverlayController using [showSubOverlay].
     * Null if none has been shown, or if a previous sub OverlayController has been dismissed.
     */
    private var subOverlayController: OverlayController? = null
    /**
     * Listener called when the overlay shown by the controller is dismissed.
     * Null unless the overlay is shown.
     */
    private var onDismissListener: (() -> Unit)? = null

    /**
     * Call to [showSubOverlay] that has been made while hidden.
     * It will be executed once [show] is called.
     */
    private var pendingSubOverlayRequest: Pair<OverlayController, Boolean>? = null

    /** Creates the ui object to be shown. */
    protected abstract fun onCreate()
    /** Show the ui object to the user. */
    protected abstract fun onShow()
    /** Hide the ui object from the user. */
    protected abstract fun onHide()
    /** Destroys the ui object. */
    protected abstract fun onDismissed()

    /**
     * Creates and show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     *
     * @param dismissListener object notified upon the shown ui dismissing.
     */
    fun create(dismissListener: (() -> Unit)? = null) {
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            return
        }

        Log.d(TAG, "create overlay ${hashCode()}")
        onDismissListener = dismissListener
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        show()
    }

    /**
     * Show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    fun show() {
        if (isShown || lifecycleRegistry.currentState != Lifecycle.State.STARTED) {
            return
        }

        Log.d(TAG, "show overlay ${hashCode()}")
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        isShown = true
        onShow()

        if (pendingSubOverlayRequest != null) {
            showSubOverlay(pendingSubOverlayRequest!!.first, pendingSubOverlayRequest!!.second)
            pendingSubOverlayRequest = null
        }
    }

    /**
     * Hide the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    fun hide() {
        if (!isShown) {
            return
        }

        Log.d(TAG, "hide overlay ${hashCode()}")
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        isShown = false
        onHide()
    }

    /**
     * Dismiss the ui object. If not hidden, hide it first.
     * If the lifecycle doesn't allows it, does nothing.
     */
    fun dismiss() {
        if (lifecycleRegistry.currentState < Lifecycle.State.CREATED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            return
        }

        Log.d(TAG, "dismiss overlay ${hashCode()}")

        hide()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        subOverlayController?.dismiss()

        onDismissed()
        onDismissListener?.invoke()
        onDismissListener = null
    }

    /**
     * Creates and show another overlay managed by a OverlayController from this dialog.
     *
     * Using this method instead of directly calling [create] and [show] on the new OverlayController will allow to keep
     * a back stack of OverlayController, allowing to resume the current overlay once the new overlay is dismissed.
     *
     * @param overlayController the controller of the new overlay to be shown.
     * @param hideCurrent true to hide the current overlay, false to display the new overlay over it.
     */
    @CallSuper
    protected open fun showSubOverlay(overlayController: OverlayController, hideCurrent: Boolean = false) {
        if (lifecycleRegistry.currentState < Lifecycle.State.STARTED) {
            Log.e(TAG, "Can't show ${overlayController.hashCode()}, parent ${hashCode()} is not created")
            return
        } else if (lifecycleRegistry.currentState < Lifecycle.State.RESUMED) {
            Log.i(TAG, "Delaying sub overlay: ${overlayController.hashCode()}; hide=$hideCurrent; " +
                    "parent=${hashCode()}")
            pendingSubOverlayRequest = overlayController to hideCurrent
            return
        }

        Log.d(TAG, "show sub overlay: ${overlayController.hashCode()}; hide=$hideCurrent; parent=${hashCode()}")

        subOverlayController = overlayController
        if (hideCurrent) {
            hide()
        } else {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        overlayController.create { onSubOverlayDismissed(overlayController) }
    }

    /**
     * Listener upon the closing of a overlay opened with [showSubOverlay].
     *
     * @param dismissedOverlay the sub overlay dismissed.
     */
    private fun onSubOverlayDismissed(dismissedOverlay: OverlayController) {
        Log.d(TAG, "sub overlay dismissed: ${dismissedOverlay.hashCode()}; parent=${hashCode()}")

        if (dismissedOverlay == subOverlayController) {
            subOverlayController = null

            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return

            if (isShown) {
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            } else {
                show()
            }
        }
    }
}