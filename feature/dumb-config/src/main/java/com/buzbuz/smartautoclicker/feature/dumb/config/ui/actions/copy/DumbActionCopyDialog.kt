
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.newDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
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
) : CopyDialog(R.style.AppTheme) {

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
            addItemDecoration(newDividerWithoutHeader(context))
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