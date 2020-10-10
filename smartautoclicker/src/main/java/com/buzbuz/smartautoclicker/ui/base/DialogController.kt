/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.TYPE_COMPAT_OVERLAY

import kotlin.IllegalStateException

/**
 * Controller for a dialog opened from a service.
 *
 * This class ensure that all dialogs opened from a service will have the same behaviour. It provides basic lifecycle
 * alike methods to ease the view initialization/cleaning, as well as a back stack management with other
 * [DialogController] opened through the [showSubDialog] methods, allowing to open another dialog from this one and
 * easily go back to this one once the sub dialog is closed. It also provide the same behaviour with an
 * [OverlayMenuController], allowing to open an overlay menu from a dialog and provide a way to go back to the dialog
 * once the user is done with the overlay menu.
 */
abstract class DialogController {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "DialogController"
    }

    /**
     * Listener called when the dialog shown by the controller is dismissed.
     * Null unless the dialog is shown.
     */
    private var onDismissListener: (() -> Unit)? = null
    /**
     * DialogController for a dialog shown from this DialogController using [showSubDialog].
     * Null if none has been shown, or if a previous subDialog has been dismissed.
     */
    private var subDialog: DialogController? = null
    /**
     * OverlayMenu shown from this DialogController using [showOverlayMenu].
     * Null if none has been shown, or if a previous overlayMenu has been dismissed.
     */
    private var overlayMenu: OverlayMenuController? = null

    /**
     * The dialog currently displayed by this controller.
     * Null until [showDialog] is called, or if it has been dismissed.
     */
    protected var dialog: AlertDialog? = null
        private set
    /**
     * The Android context used to display the [dialog].
     * Null if the dialog is null.
     */
    protected var context: Context? = null
        get() = dialog?.context
        private set

    /**
     * Builder for the dialog shown by this controller.
     * Note that the title, the cancelable value and the dismiss listener will be overridden with internal values once
     * [showDialog] is called, so any values for them defined here will not be kept.
     */
    protected abstract val dialogBuilder: AlertDialog.Builder
    /** String resource for the title of the dialog. */
    protected abstract val dialogTitle: Int

    /**
     * Creates and show the dialog.
     *
     * @param dismissListener object notified upon the shown dialog dismissing.
     */
    fun showDialog(dismissListener: () -> Unit) {
        if (dialog != null) {
            return
        }
        Log.d(TAG, "create dialog ${hashCode()}")

        onDismissListener = dismissListener

        @SuppressLint("InflateParams") // Dialog views have no parent at inflation time
        val titleView = dialogBuilder.context.getSystemService(LayoutInflater::class.java)!!
            .inflate(R.layout.view_dialog_title, null)
        titleView.findViewById<TextView>(R.id.title).setText(dialogTitle)

        dialog = dialogBuilder
            .setCustomTitle(titleView)
            .setOnDismissListener { dialogDismissed() }
            .setCancelable(false)
            .create()
            .also {
                it.window!!.setType(TYPE_COMPAT_OVERLAY)
                Log.d(TAG, "show dialog: ${hashCode()}")
                it.show()
            }

        Log.d(TAG, "dialog shown ${hashCode()}")
        onDialogShown(dialog!!)
    }

    /** Dismiss the dialog, if already shown. */
    fun dismissDialog() {
        if (dialog == null) {
            return
        }

        Log.d(TAG, "dismiss dialog ${hashCode()}")
        dialog?.dismiss()
    }

    /**
     * Creates and show another dialog managed by a DialogController from this dialog.
     *
     * Using this method instead of directly calling [showDialog] on the new DialogController will allow to keep a
     * back stack of dialogs, allowing to resume the current dialog once the new dialog is dismissed.
     *
     * @param dialogController the controller of the new dialog to be shown.
     * @param hideDialog true to hide the current dialog, false to display the new dialog over it.
     *
     * @throws IllegalStateException if the dialog managed by this DialogController isn't shown.
     */
    protected fun showSubDialog(dialogController: DialogController, hideDialog: Boolean = false) {
        Log.d(TAG, "show sub dialog: ${dialogController.hashCode()}; hide=$hideDialog; parent=${hashCode()}")

        dialog?.let {
            subDialog = dialogController
            dialogController.showDialog { subDialogDismissed(dialogController) }
            if (hideDialog) hideDialog()
        } ?: throw IllegalStateException("showSubDialog called while dialog isn't shown")
    }

    /**
     * Show the provided OverlayMenu and hide the current dialog.
     * Once the overlay menu is dismissed, the current dialog will be automatically shown again.
     *
     * @param overlay the controller of the overlay menu.
     *
     * @throws IllegalStateException if the dialog managed by this DialogController isn't shown.
     */
    protected fun showOverlayMenu(overlay: OverlayMenuController) {
        Log.d(TAG, "show overlay menu: ${overlay.hashCode()}; parent=${hashCode()}")

        dialog?.let {
            overlayMenu = overlay
            overlay.show { onOverlayMenuDismissed(overlay) }
            hideDialog()
        } ?: throw IllegalStateException("showSubDialog called while dialog isn't shown")
    }

    /**
     * Update the selected button display.
     *
     * @param button the button to be updated.
     * @param visibility the new button visibility.
     * @param textId the string resource identifier for the text of the button.
     * @param listener the new click listener of the button. Can be null if none is needed.
     */
    protected fun changeButtonState(button: Button, visibility: Int, textId: Int = -1, listener: ((View) -> Unit)? = null) {
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

    /**
     * Called after a call to [showDialog], once the dialog is created and shown.
     *
     * This method will allow the implementation to initialize the views contained by the dialog, such as setting up the
     * view listeners or initializing the view contents.
     * Note that it will not be called after the dialog is shown again after the sub dialog or overlay menu shown with
     * [showSubDialog] or [showOverlayMenu] is dismissed.
     *
     * @param dialog the new dialog shown.
     */
    protected abstract fun onDialogShown(dialog: AlertDialog)

    /**
     * Called when the dialog is dismissed.
     *
     * Useful for internal cleaning once the dialog is no longer needed, it will be called right before the dismiss
     * callback provided in [showDialog] is called.
     *
     * @param dialog the dialog dismissed.
     */
    protected open fun onDialogDismissed(dialog: AlertDialog) {}

    /**
     * Called when the visibility of the dialog has changed due to a call to [showSubDialog] or [showOverlayMenu].
     *
     * Once the sub element is dismissed, this method will be called again, notifying for the new visibility of the
     * dialog.
     *
     * @param shown the dialog visibility value. True for visible, false for hidden.
     */
    protected open fun onVisibilityChanged(shown: Boolean) {}

    /**
     * Hide/show the dialog managed by this controller.
     *
     * @param hide true to hide the dialog, true to show it.
     */
    private fun hideDialog(hide: Boolean = true) {
        dialog?.let {
            if (hide && it.isShowing) {
                Log.d(TAG, "dialog hide ${hashCode()}")
                it.hide()
                onVisibilityChanged(false)
            } else if (!hide && !it.isShowing) {
                Log.d(TAG, "dialog shown again ${hashCode()}")
                it.show()
                onVisibilityChanged(true)
            }
        }
    }

    /**
     * Listener upon the managed dialog closing.
     * Notifies the client listener for the dialog dismiss and close any sub dialog/overlay menu opened from this class.
     */
    private fun dialogDismissed() {
        Log.d(TAG, "dialog dismissed ${hashCode()}")

        val dismissedDialog = dialog
        dialog = null
        onDialogDismissed(dismissedDialog!!)

        subDialog?.dismissDialog()
        overlayMenu?.dismiss()
        onDismissListener?.invoke()
        onDismissListener = null
    }

    /**
     * Listener upon the closing of a dialog opened with [showSubDialog].
     *
     * @param dismissedDialog the subs dialog dismissed.
     */
    private fun subDialogDismissed(dismissedDialog: DialogController) {
        Log.d(TAG, "sub dialog dismissed: ${dismissedDialog.hashCode()}; parent=${hashCode()}")

        if (dismissedDialog == subDialog) {
            if (overlayMenu == null) {
                hideDialog(false)
            }
            subDialog = null
        }
    }

    /**
     * Listener upon the closing of an overlay menu opened with [showOverlayMenu].
     *
     * @param dismissedOverlay the overlay dismissed.
     */
    private fun onOverlayMenuDismissed(dismissedOverlay: OverlayMenuController) {
        Log.d(TAG, "overlay menu dismissed: ${dismissedOverlay.hashCode()}; parent=${hashCode()}")

        if (dismissedOverlay == overlayMenu) {
            if (subDialog == null) {
                hideDialog(false)
            }
            overlayMenu = null
        }
    }
}