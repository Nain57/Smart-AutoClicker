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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.getDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.CopyDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param onActionSelected the listener called when the user select an Action.
 */
class DumbActionCopyDialog(
    private val onActionSelected: (DumbAction) -> Unit,
) : CopyDialog(R.style.DumbScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: DumbActionCopyModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbActionCopyModel() },
    )

    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: DumbActionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_dumb_action_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        actionCopyAdapter = DumbActionCopyAdapter { selectedAction ->
            debounceUserInteraction {
                back()
                onActionSelected(selectedAction.dumbActionDetails.action)
            }
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(getDividerWithoutHeader(context))
            adapter = actionCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dumbActionList.collect(::updateActionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateActionList(newList: List<DumbActionCopyItem>) {
        viewBinding.layoutLoadableList.updateState(newList)
        actionCopyAdapter.submitList(ArrayList(newList))
    }
}