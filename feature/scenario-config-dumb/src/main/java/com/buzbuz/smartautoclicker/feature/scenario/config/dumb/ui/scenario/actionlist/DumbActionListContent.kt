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
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.dialogViewModels
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.click.DumbClickDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.pause.DumbPauseDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.swipe.DumbSwipeDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.DumbScenarioViewModel

import kotlinx.coroutines.launch

class DumbActionListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: DumbScenarioViewModel by dialogViewModels()

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of actions. */
    private lateinit var dumbActionsAdapter: DumbActionListAdapter
    /** TouchHelper applied to [dumbActionsAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(DumbActionReorderTouchHelper())

    override fun onCreateView(container: ViewGroup): ViewGroup {
        dumbActionsAdapter = DumbActionListAdapter(
            actionClickedListener = ::onDumbActionClicked,
            actionReorderListener = dialogViewModel::updateDumbActionOrder,
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.dumbActionsDetails.collect(::updateDumbActionList) }
            }
        }
    }

    private fun onDumbActionClicked(dumbActionDetails: DumbActionDetails) {
        when (dumbActionDetails.action) {
            is DumbAction.DumbClick -> onDumbClickClicked(dumbActionDetails.action)
            is DumbAction.DumbSwipe -> onDumbSwipeClicked(dumbActionDetails.action)
            is DumbAction.DumbPause -> onDumbPauseClicked(dumbActionDetails.action)
        }
    }

    private fun onDumbClickClicked(dumbClick: DumbAction.DumbClick) {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = DumbClickDialog(
                dumbClick = dumbClick,
                onConfirmClicked = dialogViewModel::updateDumbAction,
                onDeleteClicked = { dialogViewModel.deleteDumbAction(dumbClick) },
                onDismissClicked = {},
            ),
            hideCurrent = true,
        )
    }

    private fun onDumbSwipeClicked(dumbSwipe: DumbAction.DumbSwipe) {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = DumbSwipeDialog(
                dumbSwipe = dumbSwipe,
                onConfirmClicked = dialogViewModel::updateDumbAction,
                onDeleteClicked = { dialogViewModel.deleteDumbAction(dumbSwipe) },
                onDismissClicked = {},
            ),
            hideCurrent = true,
        )
    }

    private fun onDumbPauseClicked(dumbPause: DumbAction.DumbPause) {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = DumbPauseDialog(
                dumbPause = dumbPause,
                onConfirmClicked = dialogViewModel::updateDumbAction,
                onDeleteClicked = { dialogViewModel.deleteDumbAction(dumbPause) },
                onDismissClicked = {},
            ),
            hideCurrent = true,
        )
    }

    private fun updateDumbActionList(newList: List<DumbActionDetails>) {
        viewBinding.updateState(newList)
        dumbActionsAdapter.submitList(newList)
    }
}