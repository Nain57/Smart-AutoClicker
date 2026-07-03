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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.action

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.newDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 * @param onActionsCopied the listener called when the user select one or more Action.
 */
class ActionCopyDialog(
    private val onActionsCopied: (List<Action>) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.ACTION_COPY.name


    /** View model for this content. */
    private val viewModel: ActionCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { actionCopyViewModel() },
    )

    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: ActionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_action_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        actionCopyAdapter = ActionCopyAdapter (
            onActionSelected = { item, index ->
                debounceUserInteraction { viewModel.toggleCheckedForCopy(item.uiAction.action, index) }
            },
            onActionCheckboxClicked = { item, index -> viewModel.toggleCheckedForCopy(item.uiAction.action, index) }
        )

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(newDividerWithoutHeader(context))
            adapter = actionCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    private fun updateUiState(uiState: ActionCopyUiState?) {
        viewBinding.layoutLoadableList.updateState(uiState?.items)
        actionCopyAdapter.submitList(ArrayList(uiState?.items ?: emptyList<ActionCopyItem>()))
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    override fun onCopyClicked() {
        val copyActions = viewModel.getActionsCopy()
        if (viewModel.actionCopyShouldWarnUser(copyActions)) {
            showCopyFixDialog(copyActions)
        } else {
            notifySelectionAndDestroy(copyActions)
        }
    }

    private fun showCopyFixDialog(actionsToCopy: List<Action>) {
        val dialogArg = viewModel.getFixEventDialogArgument(actionsToCopy) ?: return
        overlayManager.navigateTo(
            context = context,
            hideCurrent = false,
            newOverlay = FixEventChildrenCopyDialog(
                dialogArguments = dialogArg,
                onFixConfirmed = { event -> notifySelectionAndDestroy(event.actions) }
            )
        )
    }

    private fun notifySelectionAndDestroy(actionsToCopy: List<Action>) {
        viewModel.saveCopyActions(actionsToCopy)
        back()
        onActionsCopied(actionsToCopy)
    }
}
