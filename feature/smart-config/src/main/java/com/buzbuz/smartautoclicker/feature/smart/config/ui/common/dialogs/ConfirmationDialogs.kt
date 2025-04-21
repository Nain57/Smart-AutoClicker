/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay

import com.buzbuz.smartautoclicker.feature.smart.config.R

import com.google.android.material.dialog.MaterialAlertDialogBuilder


internal fun Context.showCloseWithoutSavingDialog(onOkPressed: () -> Unit): Unit =
    showConfirmationDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.warning_dialog_message_close_without_saving,
        onOkPressed = onOkPressed,
    )

internal fun Context.showCopyEventWithToggleEventFromAnotherScenarioDialog(onOkPressed: () -> Unit): Unit =
    showConfirmationDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.warning_dialog_message_toggle_action_from_another_scenario,
        onOkPressed = onOkPressed,
    )

internal fun Context.showDeleteEventWithAssociatedActionsDialog(onOkPressed: () -> Unit): Unit =
    showConfirmationDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.warning_dialog_message_event_delete_associated_action,
        onOkPressed = onOkPressed,
    )

internal fun Context.showDeleteConditionsWithAssociatedActionsDialog(onOkPressed: () -> Unit): Unit =
    showConfirmationDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.warning_dialog_message_condition_delete_associated_action,
        onOkPressed = onOkPressed,
    )

internal fun Context.showStopDownloadWarningDialog(onOkPressed: () -> Unit): Unit =
    showConfirmationDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.warning_dialog_message_stop_download,
        onOkPressed = onOkPressed,
    )

private fun Context.showConfirmationDialog(@StringRes title: Int, @StringRes message: Int, onOkPressed: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
            onOkPressed()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create()
        .showAsOverlay()
}

