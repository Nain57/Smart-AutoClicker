/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.copy

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.bindings.updateState
import com.buzbuz.smartautoclicker.overlays.base.dialog.CopyDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param actions the list of edited actions for the configured event.
 * @param onActionSelected the listener called when the user select an Action.
 */
class ActionCopyDialog(
    context: Context,
    private val actions: List<Action>,
    private val onActionSelected: (Action) -> Unit,
) : CopyDialog(context) {

    /** View model for this content. */
    private val viewModel: ActionCopyModel by lazy { ViewModelProvider(this).get(ActionCopyModel::class.java) }

    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: ActionCopyAdapter

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setItemsFromContainer(actions)

        actionCopyAdapter = ActionCopyAdapter { selectedAction ->
            onActionSelected(viewModel.getNewActionForCopy(selectedAction))
            destroy()
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = actionCopyAdapter
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionList.collect(::updateActionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateActionList(newList: List<ActionCopyModel.ActionCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newList)
        actionCopyAdapter.submitList(if (newList == null) ArrayList() else ArrayList(newList))
    }
}