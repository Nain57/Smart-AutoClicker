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
package com.buzbuz.smartautoclicker.overlays.event.actions

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentActionsBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.bindings.setEmptyText
import com.buzbuz.smartautoclicker.overlays.bindings.updateState
import com.buzbuz.smartautoclicker.overlays.action.copy.ActionCopyDialog
import com.buzbuz.smartautoclicker.overlays.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionConfigDialog
import com.buzbuz.smartautoclicker.overlays.utils.MultiChoiceDialog

import kotlinx.coroutines.launch

class ActionsContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogViewModelStoreOwner).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: ActionsViewModel by lazy {
        ViewModelProvider(this).get(ActionsViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentActionsBinding
    /** Adapter for the list of actions. */
    private lateinit var actionAdapter: ActionAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(dialogViewModel.configuredEvent)

        viewBinding = ContentActionsBinding.inflate(LayoutInflater.from(context), container, false).apply {
            buttonNew.setOnClickListener { onNewButtonClicked() }
            buttonCopy.setOnClickListener { onCopyButtonClicked() }
        }

        actionAdapter = ActionAdapter(
            actionClickedListener = ::onActionClicked,
            actionReorderListener = viewModel::updateActionOrder
        )

        viewBinding.layoutList.apply {
            setEmptyText(R.string.dialog_conditions_empty)
            list.adapter = actionAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.actionDetails.collect(::updateActionList) }
            }
        }
    }

    private fun onNewButtonClicked() {
        dialogViewModel.requestSubOverlay(newActionTypeSelectionNavigationRequest())
    }

    private fun onCopyButtonClicked() {
        dialogViewModel.requestSubOverlay(newActionCopyNavigationRequest())
    }

    private fun onActionClicked(action: Action, index: Int) {
        dialogViewModel.requestSubOverlay(newActionConfigNavigationRequest(action, index))
    }

    private fun updateActionList(newList: List<ActionDetails>?) {
        viewBinding.layoutList.updateState(newList)
        actionAdapter.submitList(newList)
    }

    private fun newActionTypeSelectionNavigationRequest() = NavigationRequest(
        MultiChoiceDialog(
            context = context,
            dialogTitle = R.string.dialog_action_type_title,
            choices = listOf(ActionTypeChoice.Click, ActionTypeChoice.Swipe, ActionTypeChoice.Pause, ActionTypeChoice.Intent),
            onChoiceSelected = { choiceClicked ->
                dialogViewModel.requestSubOverlay(
                    newActionConfigNavigationRequest(
                        viewModel.createAction(context, choiceClicked)
                    )
                )
            }
        )
    )

    private fun newActionCopyNavigationRequest() = NavigationRequest(
        ActionCopyDialog(
            context = context,
            actions = viewModel.actions.value,
            onActionSelected = { actionSelected ->
                dialogViewModel.requestSubOverlay(newActionConfigNavigationRequest(actionSelected))
            }
        )
    )

    private fun newActionConfigNavigationRequest(action: Action, index: Int = -1) = NavigationRequest(
        overlay = ActionConfigDialog(
            context= context,
            action = action,
            onConfirmClicked = { confirmedAction ->
                if (index != -1)  viewModel.updateAction(confirmedAction, index)
                else viewModel.addAction(confirmedAction)
            },
            onDeleteClicked = { viewModel.removeAction(action) }
        ),
        hideCurrent = true,
    )
}