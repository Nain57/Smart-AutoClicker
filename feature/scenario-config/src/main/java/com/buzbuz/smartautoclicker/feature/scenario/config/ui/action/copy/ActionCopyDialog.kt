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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.copy

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.CopyDialog
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.scenario.config.R

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param onActionSelected the listener called when the user select an Action.
 */
class ActionCopyDialog(
    context: Context,
    private val onActionSelected: (Action) -> Unit,
) : CopyDialog(context, R.style.ScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: ActionCopyModel by lazy { ViewModelProvider(this).get(ActionCopyModel::class.java) }

    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: ActionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        actionCopyAdapter = ActionCopyAdapter { selectedAction ->
            onActionSelected(selectedAction.actionDetails.action)
            destroy()
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = actionCopyAdapter
        }

        viewBinding.layoutTopBar.search.queryHint = context.getString(R.string.search_view_hint_action_copy)

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