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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.databinding.DialogActionConfigBinding
import com.buzbuz.smartautoclicker.extensions.addOnAfterTextChangedListener
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * [ActionConfigDialog] implementation for displaying an event action and providing a button to delete it.
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
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.actionValues?.collect { actionValues ->
                        when (actionValues) {
                            is ActionConfigModel.ClickActionValues -> setupClickUi(actionValues)
                            is ActionConfigModel.SwipeActionValues -> setupSwipeUi(actionValues)
                            is ActionConfigModel.PauseActionValues -> setupPauseUi(actionValues)
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

    /**
     * Setup the UI for a click Action.
     * @param clickValues view model values for a click action.
     */
    private fun setupClickUi(clickValues: ActionConfigModel.ClickActionValues) {
        viewBinding.apply {
            editName.addOnAfterTextChangedListener { editable ->
                clickValues.setName(editable.toString())
            }

            includeClickConfig.apply {
                actionConfigLayoutClick.visibility = View.VISIBLE

                editPressDuration.addOnAfterTextChangedListener { editable ->
                    val pressDuration = editable.toString()
                    clickValues.setPressDuration(if (pressDuration.isNotEmpty()) pressDuration.toLong() else 0)
                }

                textClickPosition.setOnClickListener {
                    showSubOverlay(ClickSwipeSelectorMenu(
                        context = context,
                        selector = CoordinatesSelector.One(),
                        onCoordinatesSelected = { selector ->
                            clickValues.setPosition((selector as CoordinatesSelector.One).coordinates!!)
                        },
                    ), hideCurrent = true)
                }
                textClickPosition.setLeftRightCompoundDrawables(R.drawable.ic_click, R.drawable.ic_chevron)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    clickValues.name.collect { name ->
                        viewBinding.editName.apply {
                            setText(name)
                            setSelection(length())
                        }
                    }
                }

                launch {
                    clickValues.pressDuration.collect { duration ->
                        viewBinding.includeClickConfig.editPressDuration.apply {
                            setText(duration.toString())
                            setSelection(length())
                        }
                    }
                }

                launch {
                    clickValues.position.collect { position ->
                        viewBinding.includeClickConfig.textClickPosition.apply {
                            if (position == null) {
                                setText(R.string.dialog_action_config_click_position_none)
                            } else {
                                text = context.getString(
                                    R.string.dialog_action_config_click_position,
                                    position.x,
                                    position.y
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup the UI for a swipe Action.
     * @param swipeValues view model values for a swipe action.
     */
    private fun setupSwipeUi(swipeValues: ActionConfigModel.SwipeActionValues) {
        viewBinding.apply {
            editName.addOnAfterTextChangedListener { editable ->
                swipeValues.setName(editable.toString())
            }

            includeSwipeConfig.apply {
                actionConfigLayoutSwipe.visibility = View.VISIBLE

                editSwipeDuration.addOnAfterTextChangedListener { editable ->
                    val swipeDuration = editable.toString()
                    swipeValues.setSwipeDuration(if (swipeDuration.isNotEmpty()) swipeDuration.toLong() else 0)
                }

                textSwipePosition.setOnClickListener {
                    showSubOverlay(
                        ClickSwipeSelectorMenu(
                            context = context,
                            selector = CoordinatesSelector.Two(),
                            onCoordinatesSelected = { selector ->
                                (selector as CoordinatesSelector.Two).let {
                                    swipeValues.setPositions(it.coordinates1!!, it.coordinates2!!)
                                }
                            },
                        ),
                        hideCurrent = true
                    )
                }
                textSwipePosition.setLeftRightCompoundDrawables(R.drawable.ic_swipe, R.drawable.ic_chevron)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    swipeValues.name.collect { name ->
                        viewBinding.editName.apply {
                            setText(name)
                            setSelection(length())
                        }
                    }
                }

                launch {
                    swipeValues.swipeDuration.collect { duration ->
                        viewBinding.includeSwipeConfig.editSwipeDuration.apply {
                            setText(duration.toString())
                            setSelection(length())
                        }
                    }
                }

                launch {
                    swipeValues.positions.collect { (from, to) ->
                        viewBinding.includeSwipeConfig.textSwipePosition.apply {
                            if (from == null || to == null) {
                                setText(R.string.dialog_action_config_swipe_position_none)
                            } else {
                                text = context.getString(
                                    R.string.dialog_action_config_swipe_position,
                                    from.x,
                                    from.y,
                                    to.x,
                                    to.y
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup the UI for a pause Action.
     * @param pauseValues view model values for a pause action.
     */
    private fun setupPauseUi(pauseValues: ActionConfigModel.PauseActionValues) {
        viewBinding.apply {
            editName.addOnAfterTextChangedListener { editable ->
                pauseValues.setName(editable.toString())
            }

            includePauseConfig.apply {
                actionConfigLayoutPause.visibility = View.VISIBLE

                editPauseDuration.addOnAfterTextChangedListener { editable ->
                    val pauseDuration = editable.toString()
                    pauseValues.setPauseDuration(if (pauseDuration.isNotEmpty()) pauseDuration.toLong() else 0)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    pauseValues.name.collect { name ->
                        viewBinding.editName.apply {
                            if (name.isNullOrEmpty()) {
                                setText(R.string.default_event_name)
                            } else {
                                setText(name)
                            }
                            setSelection(length())
                        }
                    }
                }

                launch {
                    pauseValues.pauseDuration.collect { duration ->
                        viewBinding.includePauseConfig.editPauseDuration.apply {
                            setText(duration.toString())
                            setSelection(length())
                        }
                    }
                }
            }
        }
    }

    /** Notify the confirm listener and dismiss the dialog. */
    private fun onOkClicked() {
        viewModel?.let {
            onConfirmClicked.invoke(it.getConfiguredAction())
        }
        dismiss()
    }
}
