
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

