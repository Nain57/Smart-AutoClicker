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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.actions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.copy.ActionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.IntentDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause.PauseDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe.SwipeDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.ToggleEventDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_ENABLED_ITEM
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.ActionTypeSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter.ChangeCounterDialog

import kotlinx.coroutines.launch

class ActionsContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ActionsViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { actionsViewModel() },
    )

    /** TouchHelper applied to [actionAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ActionReorderTouchHelper())

    private val actionConfigDialogListener: OnActionConfigCompleteListener by lazy {
        object : OnActionConfigCompleteListener {
            override fun onConfirmClicked() { viewModel.upsertEditedAction() }
            override fun onDeleteClicked() { viewModel.removeEditedAction() }
            override fun onDismissClicked() { viewModel.dismissEditedAction() }
        }
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of actions. */
    private lateinit var actionAdapter: ActionAdapter

    /** Tells if the billing flow has been triggered by the action count limit. */
    private var actionLimitReachedClick: Boolean = false
    /** Dialog for the selection of the action type when creating a new one. Null if not displayed. */
    private var actionTypeSelectionDialog: MultiChoiceDialog<ActionTypeChoice>? = null

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        actionAdapter = ActionAdapter(
            actionClickedListener = ::onActionClicked,
            actionReorderListener = viewModel::updateActionOrder,
            itemViewBound = ::onActionItemBound,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_actions,
                secondaryId = R.string.message_empty_secondary_action_list,
            )
            list.apply {
                itemTouchHelper.attachToRecyclerView(this)
                adapter = actionAdapter
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        // When the billing flow is not longer displayed, restore the dialogs states
        lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.CREATED)) {
                viewModel.isBillingFlowDisplayed.collect { isDisplayed ->
                    if (!isDisplayed) {
                        actionTypeSelectionDialog?.show()

                        if (actionLimitReachedClick) {
                            dialogController.show()
                            actionLimitReachedClick = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isActionLimitReached.collect(::updateActionLimitationVisibility) }
                launch { viewModel.canCopyAction.collect(::updateCopyButtonVisibility) }
                launch { viewModel.actionDetails.collect(::updateActionList) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorCreateActionView(dialogController.createCopyButtons.buttonNew)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopAllViewMonitoring()
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            val dialog = ActionTypeSelectionDialog(
                choices = viewModel.actionCreationItems.value,
                onChoiceSelectedListener = { choiceClicked ->
                    if (!choiceClicked.enabled) {
                        actionTypeSelectionDialog?.show()
                        viewModel.onProModeUnsubscribedActionClicked(context, choiceClicked)
                    } else {
                        actionTypeSelectionDialog = null
                        showActionConfigDialog(viewModel.createAction(context, choiceClicked))
                    }
                },
                onCancelledListener = { actionTypeSelectionDialog = null }
            )
            actionTypeSelectionDialog = dialog

            dialogController.overlayManager.navigateTo(
                context = context,
                newOverlay = dialog,
            )
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            dialogController.overlayManager.navigateTo(
                context = context,
                newOverlay = ActionCopyDialog(
                    onActionSelected = { newCopyAction ->
                        showActionConfigDialog(viewModel.createNewActionFrom(newCopyAction))
                    }
                ),
            )
        }
    }

    private fun onCreateCopyClickedWhileLimited() {
        debounceUserInteraction {
            actionLimitReachedClick = true

            dialogController.hide()
            viewModel.onActionCountReachedAddCopyClicked(context)
        }
    }

    private fun onActionClicked(action: Action) {
        debounceUserInteraction {
            showActionConfigDialog(action)
        }
    }

    private fun onActionItemBound(index: Int, itemView: View?) {
        if (index != 0) return

        if (itemView != null) viewModel.monitorFirstActionView(itemView)
        else viewModel.stopFirstActionViewMonitoring()
    }

    private fun updateActionLimitationVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.apply {
            if (isVisible) {
                root.alpha = ALPHA_DISABLED_ITEM
                buttonNew.setOnClickListener { onCreateCopyClickedWhileLimited() }
                buttonCopy.setOnClickListener { onCreateCopyClickedWhileLimited() }
            } else {
                root.alpha = ALPHA_ENABLED_ITEM
                buttonNew.setOnClickListener { onCreateButtonClicked() }
                buttonCopy.setOnClickListener { onCopyButtonClicked() }
            }
        }
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (isVisible) show() else hide()
        }
    }

    private fun updateActionList(newList: List<Pair<Action, ActionDetails>>?) {
        viewBinding.updateState(newList)
        actionAdapter.submitList(newList)
    }

    private fun showActionConfigDialog(action: Action) {
        viewModel.startActionEdition(action)

        val overlay = when (action) {
            is Action.Click -> ClickDialog(actionConfigDialogListener)
            is Action.Swipe -> SwipeDialog(actionConfigDialogListener)
            is Action.Pause -> PauseDialog(actionConfigDialogListener)
            is Action.Intent -> IntentDialog(actionConfigDialogListener)
            is Action.ToggleEvent -> ToggleEventDialog(actionConfigDialogListener)
            is Action.ChangeCounter -> ChangeCounterDialog(actionConfigDialogListener)
            else -> throw IllegalArgumentException("Not yet supported")
        }

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = overlay,
            hideCurrent = true,
        )
    }
}