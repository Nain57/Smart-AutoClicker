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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCopyUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionEditionUiFlow

import kotlinx.coroutines.launch

class DumbActionListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val viewModel: DumbActionListViewModel by viewModels()

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of actions. */
    private lateinit var dumbActionsAdapter: DumbActionListAdapter

    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var dumbActionCreator: DumbActionCreator

    /** TouchHelper applied to [dumbActionsAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(DumbActionReorderTouchHelper())

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        dumbActionsAdapter = DumbActionListAdapter(
            actionClickedListener = ::onDumbActionClicked,
            actionReorderListener = viewModel::updateDumbActionOrder,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_dumb_action_list,
                secondaryId = R.string.message_empty_secondary_dumb_action_list,
            )
            list.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                itemTouchHelper.attachToRecyclerView(this)
                adapter = dumbActionsAdapter
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = viewModel::createNewDumbClick,
            createNewDumbSwipe = viewModel::createNewDumbSwipe,
            createNewDumbPause = viewModel::createNewDumbPause,
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::addNewDumbAction,
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.dumbActionsDetails.collect(::updateDumbActionList) }
                launch { viewModel.canCopyAction.collect(::updateCopyButtonState) }
            }
        }
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            OverlayManager.getInstance(context).startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            OverlayManager.getInstance(context).startDumbActionCopyUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener
            )
        }
    }

    private fun onDumbActionClicked(dumbActionDetails: DumbActionDetails) {
        debounceUserInteraction {
            OverlayManager.getInstance(context).startDumbActionEditionUiFlow(
                context = context,
                dumbAction = dumbActionDetails.action,
                listener = updateActionUiFlowListener,
            )
        }
    }

    private fun updateDumbActionList(newList: List<DumbActionDetails>) {
        viewBinding.updateState(newList)
        dumbActionsAdapter.submitList(newList)
    }

    private fun updateCopyButtonState(enabled: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (enabled) show() else hide()
        }
    }
}