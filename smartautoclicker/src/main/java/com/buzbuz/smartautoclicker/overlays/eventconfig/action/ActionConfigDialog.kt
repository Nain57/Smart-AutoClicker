/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.databinding.DialogActionConfigBinding
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.click.ClickConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.click.setupClickUi
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent.IntentConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent.setupIntentUi
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.pause.PauseConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.pause.setupPauseUi
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.swipe.SwipeConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.swipe.setupSwipeUi
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying an event action and providing a button to delete it.
 *
 * This dialog is generic for all [Action] type. Title and displayed views will change according to the type of the
 * action parameter.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param action the event action that will be edited.
 * @param onConfirmClicked the listener called when the user presses the ok button.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ActionConfigDialog(
    context: Context,
    action: Action,
    private val onConfirmClicked: (Action) -> Unit,
    private val onDeleteClicked: () -> Unit
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: ActionConfigModel? = ActionConfigModel(context).apply {
        attachToLifecycle(this@ActionConfigDialog)
        setConfigAction(action)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogActionConfigBinding

    /** Possible dialog titles. One for each action type. */
    private val title: Pair<Int, Int> = when (action) {
        is Action.Click -> R.string.dialog_action_type_click to R.drawable.ic_click
        is Action.Swipe -> R.string.dialog_action_type_swipe to R.drawable.ic_swipe
        is Action.Pause -> R.string.dialog_action_type_pause to R.drawable.ic_wait
        is Action.Intent -> R.string.dialog_action_type_intent to R.drawable.ic_intent
    }

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogActionConfigBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(
                R.layout.view_dialog_title,
                title.first,
                title.second,
                R.color.overlayViewPrimary,
            )
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.dialog_condition_delete) { _, _ -> onDeleteClicked.invoke() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.apply {
            root.setOnTouchListener(hideSoftInputTouchListener)
            editName.apply {
                setSelectAllOnFocus(true)
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel?.setName(viewBinding.editName.text.toString())
                    }
                })
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.name?.collect { name ->
                        viewBinding.editName.apply {
                            setText(name)
                            setSelection(name?.length ?: 0)
                        }
                    }
                }

                launch {
                    viewModel?.actionModel?.collect { actionValues ->
                        when (actionValues) {
                            is ClickConfigModel -> viewBinding.includeClickConfig.setupClickUi(
                                context, actionValues, this@ActionConfigDialog, lifecycleScope, ::showSubOverlay
                            )
                            is SwipeConfigModel -> viewBinding.includeSwipeConfig.setupSwipeUi(
                                context, actionValues, this@ActionConfigDialog, lifecycleScope, ::showSubOverlay
                            )
                            is PauseConfigModel -> viewBinding.includePauseConfig.setupPauseUi(
                                actionValues, this@ActionConfigDialog, lifecycleScope
                            )
                            is IntentConfigModel -> viewBinding.includeIntentConfig.setupIntentUi(
                                context, actionValues, this@ActionConfigDialog, lifecycleScope, ::showSubOverlay
                            )
                        }
                    }
                }

                launch {
                    viewModel?.isValidAction?.collect { isValid ->
                        changeButtonState(
                            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                            visibility = if (isValid) View.VISIBLE else View.INVISIBLE,
                            listener = { onOkClicked() }
                        )
                    }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }

    /** Notify the confirm listener and dismiss the dialog. */
    private fun onOkClicked() {
        viewModel?.let { model ->
            model.saveLastConfig()
            onConfirmClicked(model.getConfiguredAction())
        }
        dismiss()
    }
}
