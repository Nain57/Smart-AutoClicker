/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.event.actions

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.baseui.bindings.updateState
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.overlays.config.action.click.ClickDialog
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.config.action.copy.ActionCopyDialog
import com.buzbuz.smartautoclicker.overlays.config.action.intent.IntentDialog
import com.buzbuz.smartautoclicker.overlays.config.action.pause.PauseDialog
import com.buzbuz.smartautoclicker.overlays.config.action.swipe.SwipeDialog
import com.buzbuz.smartautoclicker.overlays.config.action.toggleevent.ToggleEventDialog
import com.buzbuz.smartautoclicker.overlays.config.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.ui.databinding.IncludeLoadableListBinding

import kotlinx.coroutines.launch

class ActionsContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogController).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: ActionsViewModel by lazy {
        ViewModelProvider(this).get(ActionsViewModel::class.java)
    }

    /** TouchHelper applied to [actionAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ActionReorderTouchHelper())

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of actions. */
    private lateinit var actionAdapter: ActionAdapter

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        actionAdapter = ActionAdapter(
            actionClickedListener = ::onActionClicked,
            actionReorderListener = viewModel::updateActionOrder,
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyAction.collect(::updateCopyButtonVisibility) }
                launch { viewModel.actionDetails.collect(::updateActionList) }
            }
        }
    }

    override fun onCreateButtonClicked() {
        dialogViewModel.requestSubOverlay(newActionTypeSelectionNavigationRequest())
    }

    override fun onCopyButtonClicked() {
        dialogViewModel.requestSubOverlay(newActionCopyNavigationRequest())
    }

    private fun onActionClicked(action: EditedAction, index: Int) {
        println("TOTO: onActionClicked $index")
        dialogViewModel.requestSubOverlay(newActionConfigNavigationRequest(action, index))
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (isVisible) show() else hide()
        }
    }

    private fun updateActionList(newList: List<Pair<EditedAction, ActionDetails>>?) {
        viewBinding.updateState(newList)
        actionAdapter.submitList(newList)
    }

    private fun newActionTypeSelectionNavigationRequest() = NavigationRequest(
        MultiChoiceDialog(
            context = context,
            theme = R.style.AppTheme,
            dialogTitleText = R.string.dialog_overlay_title_action_type,
            choices = listOf(
                ActionTypeChoice.Click,
                ActionTypeChoice.Swipe,
                ActionTypeChoice.Pause,
                ActionTypeChoice.Intent,
                ActionTypeChoice.ToggleEvent,
            ),
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
            onActionSelected = { newCopyAction ->
                dialogViewModel.requestSubOverlay(
                    newActionConfigNavigationRequest(newCopyAction)
                )
            }
        )
    )

    private fun newActionConfigNavigationRequest(editedAction: EditedAction, index: Int = -1): NavigationRequest {
        val overlay = when (editedAction.action) {
            is Action.Click -> ClickDialog(context, editedAction, viewModel::removeAction) { savedClick ->
                viewModel.addUpdateAction(savedClick, index)
            }

            is Action.Swipe -> SwipeDialog(context, editedAction, viewModel::removeAction) { savedSwipe ->
                viewModel.addUpdateAction(savedSwipe, index)
            }

            is Action.Pause -> PauseDialog(context, editedAction, viewModel::removeAction) { savedPause ->
                viewModel.addUpdateAction(savedPause, index)
            }

            is Action.Intent -> IntentDialog(context, editedAction, viewModel::removeAction) { savedIntent ->
                viewModel.addUpdateAction(savedIntent, index)
            }

            is Action.ToggleEvent -> ToggleEventDialog(context, editedAction, viewModel::removeAction) { savedToggleEvent ->
                viewModel.addUpdateAction(savedToggleEvent, index)
            }

            else -> throw IllegalArgumentException("Not yet supported")
        }

        return NavigationRequest(
            overlay = overlay,
            hideCurrent = true,
        )
    }
}