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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.databinding.DialogEventConfigBinding
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionConfigDialog
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionSelectorMenu
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionConfigDialog
import com.buzbuz.smartautoclicker.overlays.copy.actions.ActionCopyDialog
import com.buzbuz.smartautoclicker.overlays.copy.conditions.ConditionCopyDialog
import com.buzbuz.smartautoclicker.overlays.utils.MultiChoiceDialog
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying a event info and allowing the user to edit it.
 *
 * Any changes done on the event by the user will be saved only when the user clicks on the positive button of the
 * dialog. If the dialog is dismissed by any other means, no changes will be kept.
 * If the event info aren't complete (i.e, there is no conditions or actions), the positive button will be disabled,
 * preventing the user to configure an invalid click.
 *
 * This dialog is the root of many other event config dialog. This logic is managed by the [ConfigSubOverlayModel],
 * and any state change will be handled by [updateSubOverlay].
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param event the event to be displayed on the dialog.
 * @param onConfigComplete listener notified when the user click on the OK button. Contains the edited event.
 */
class EventConfigDialog(
    context: Context,
    event: Event,
    private val onConfigComplete: (Event) -> Unit,
) : OverlayDialogController(context) {

    /** The view model for the data displayed in this dialog. */
    private var viewModel: EventConfigModel? = EventConfigModel(context).apply {
        attachToLifecycle(this@EventConfigDialog)
        setConfigEvent(event)
    }
    /** The view model managing this dialog sub overlays. */
    private var subOverlayViewModel: ConfigSubOverlayModel? = ConfigSubOverlayModel(context).apply {
        attachToLifecycle(this@EventConfigDialog)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEventConfigBinding

    /** TouchHelper applied to [actionsAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ActionReorderTouchHelper())

    /** Adapter displaying all actions for the event displayed by this dialog. */
    private val actionsAdapter = ActionsAdapter(
        addActionClickedListener = {
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionTypeSelection)
        },
        copyActionClickedListener = {
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionCopy)
        },
        actionClickedListener = { index, action ->
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionConfig(action, index))
        },
        actionReorderListener = { actions -> viewModel?.updateActionOrder(actions) }
    )
    /** Adapter displaying all conditions for the event displayed by this dialog. */
    private val conditionsAdapter = ConditionAdapter(
        addConditionClickedListener = {
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionCapture)
        },
        copyConditionClickedListener = {
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionCopy)
        },
        conditionClickedListener = { index, condition ->
            subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionConfig(condition, index))
        },
        bitmapProvider = { bitmap, onLoaded ->
            viewModel?.getConditionBitmap(bitmap, onLoaded)
        },
    )

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEventConfigBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_event_config_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.apply {
            root.setOnTouchListener(hideSoftInputTouchListener)

            layoutConditionOperator.setOnClickListener {
                viewModel?.toggleConditionOperator()
            }

            editName.apply {
                setSelectAllOnFocus(true)
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel?.setEventName(s.toString())
                    }
                })
            }

            listConditions.adapter = conditionsAdapter
            listActions.adapter = actionsAdapter
            itemTouchHelper.attachToRecyclerView(listActions)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel?.eventName?.collect { name ->
                        viewBinding.editName.apply {
                            setText(name)
                            setSelection(name?.length ?: 0)
                        }
                    }
                }

                launch {
                    viewModel?.actionListItems?.collect { actions ->
                        actionsAdapter.submitList(actions.toMutableList())
                    }
                }

                launch {
                    viewModel?.conditionOperator?.collect { conditionOperator ->
                        viewBinding.textConditionOperatorDesc.apply {
                            when (conditionOperator) {
                                AND -> {
                                    viewBinding.textConditionOperatorDesc.apply {
                                        setLeftRightCompoundDrawables(R.drawable.ic_all_conditions, R.drawable.ic_chevron)
                                        text = context.getString(R.string.condition_operator_and)
                                    }
                                }
                                OR -> {
                                    viewBinding.textConditionOperatorDesc.apply {
                                        setLeftRightCompoundDrawables(R.drawable.ic_one_condition, R.drawable.ic_chevron)
                                        text = context.getString(R.string.condition_operator_or)
                                    }
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel?.conditionListItems?.collect { conditions ->
                        conditionsAdapter.submitList(conditions)
                    }
                }

                // Allow/Forbid the access to "OK" depending on the validity of event
                launch {
                    viewModel?.isValidEvent?.collect { isValid ->
                        changeButtonState(
                            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                            visibility = if (isValid) View.VISIBLE else View.INVISIBLE,
                            listener = { onOkClicked() }
                        )
                    }
                }

                launch {
                    subOverlayViewModel?.subOverlayRequest?.collect { updateSubOverlay(it) }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
        subOverlayViewModel = null
    }

    /**
     * Update the current sub overlay.
     *
     * This display the correct overlay according to the provided state.
     * To modify the sub overlay, call [ConfigSubOverlayModel.requestSubOverlay].
     *
     * @param overlayType the type of sub overlay to show.
     */
    private fun updateSubOverlay(overlayType: SubOverlay) {
        when (overlayType) {

            is SubOverlay.ActionTypeSelection -> {
                showSubOverlay(MultiChoiceDialog(
                    context = context,
                    dialogTitle = R.string.dialog_action_type_title,
                    choices = listOf(ActionTypeChoice.Click, ActionTypeChoice.Swipe, ActionTypeChoice.Pause, ActionTypeChoice.Intent),
                    onChoiceSelected = { choiceClicked ->
                        viewModel?.let { model ->
                            subOverlayViewModel?.requestSubOverlay(
                                SubOverlay.ActionConfig(
                                    model.createAction(context, choiceClicked),
                                )
                            )
                        }
                    }
                ))
            }

            is SubOverlay.ActionCopy -> {
                viewModel?.let {
                    showSubOverlay(ActionCopyDialog(
                        context = context,
                        actions = it.actions.value!!,
                        onActionSelected = { actionSelected ->
                            subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionConfig(actionSelected))
                        }
                    ))
                }
            }

            is SubOverlay.ActionConfig -> {
                showSubOverlay(
                    overlayController = ActionConfigDialog(
                        context= context,
                        action = overlayType.action,
                        onConfirmClicked = {
                            if (overlayType.index != -1) {
                                viewModel?.updateAction(it, overlayType.index)
                            } else {
                                viewModel?.addAction(it)
                            }
                        },
                        onDeleteClicked = { viewModel?.removeAction(overlayType.action) }
                    ),
                    hideCurrent = true,
                )
            }

            is SubOverlay.ConditionCopy -> {
                viewModel?.let {
                    showSubOverlay(ConditionCopyDialog(
                        context = context,
                        conditions = it.conditions.value!!,
                        onConditionSelected = { conditionSelected ->
                            subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionConfig(conditionSelected))
                        }
                    ))
                }
            }

            is SubOverlay.ConditionCapture -> {
                showSubOverlay(
                    overlayController = ConditionSelectorMenu(
                        context = context,
                        onConditionSelected = { area, bitmap ->
                            viewModel?.let { model ->
                                subOverlayViewModel?.requestSubOverlay(
                                    SubOverlay.ConditionConfig(model.createCondition(context, area, bitmap))
                                )
                            }
                        }
                    ),
                    hideCurrent = true,
                )
            }

            is SubOverlay.ConditionConfig -> {
                showSubOverlay(ConditionConfigDialog(
                    context = context,
                    condition = overlayType.condition,
                    onConfirmClicked = {
                        if (overlayType.index != -1) {
                            viewModel?.updateCondition(it, overlayType.index)
                        } else {
                            viewModel?.addCondition(it)
                        }
                    },
                    onDeleteClicked = { viewModel?.removeCondition(overlayType.condition) }
                ))
            }

            is SubOverlay.None -> { /* Nothing to do */ }
        }

        subOverlayViewModel?.consumeRequest()
    }

    /**
     * Called when the user clicks on the ok button to close the dialog.
     * This will close the dialog and notify the listener.
     */
    private fun onOkClicked() {
        viewModel?.let {
            onConfigComplete(it.getConfiguredEvent())
        }
        dismiss()
    }
}
