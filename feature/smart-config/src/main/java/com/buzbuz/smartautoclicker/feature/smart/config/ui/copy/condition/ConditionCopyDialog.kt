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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.condition

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

/**
 * [CopyDialog] implementation for displaying the whole list of conditions for a copy.
 * @param onConditionsCopied the listener called when the user select Conditions.
 */
class ConditionCopyDialog(
    private val requestTriggerConditions: Boolean,
    private val onConditionsCopied: (List<Condition>) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme)  {

    /** View model for this content. */
    private val viewModel: ConditionCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { conditionCopyViewModel() },
    )

    /** Adapter displaying the list of conditions. */
    private lateinit var conditionAdapter: ConditionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_condition_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CONDITION_COPY.name

    override fun onDialogCreated(dialog: BottomSheetDialog) {

        viewModel.setCopyListType(requestTriggerConditions)
        conditionAdapter = ConditionCopyAdapter(
            conditionClickedListener = { selectedCondition, index ->
                viewModel.toggleCheckedForCopy(selectedCondition, index)
            },
            bitmapProvider = { bitmap, onLoaded ->
                viewModel.getConditionBitmap(bitmap, onLoaded)
            },
        )

        viewBinding.layoutLoadableList.list.apply {
            adapter = conditionAdapter

            layoutManager = GridLayoutManager(context, 2).apply {
                spanSizeLookup = conditionAdapter.spanSizeLookup
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    private fun updateUiState(uiState: ConditionCopyUiState?) {
        viewBinding.layoutLoadableList.updateState(uiState?.items)
        conditionAdapter.submitList(ArrayList(uiState?.items ?: emptyList<ConditionCopyItem>()))
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    override fun onCopyClicked() {
        val copyConditions = viewModel.getConditionsCopy()
        if (viewModel.conditionCopyShouldWarnUser(copyConditions)) {
            showCopyFixDialog(copyConditions)
        } else {
            notifySelectionAndDestroy(copyConditions)
        }
    }

    private fun showCopyFixDialog(conditionsToCopy: List<Condition>) {
        val dialogArg = viewModel.getFixEventDialogArgument(conditionsToCopy) ?: return
        overlayManager.navigateTo(
            context = context,
            hideCurrent = false,
            newOverlay = FixEventChildrenCopyDialog(
                dialogArguments = dialogArg,
                onFixConfirmed = { event -> notifySelectionAndDestroy(event.conditions) }
            )
        )
    }

    private fun notifySelectionAndDestroy(conditionsToCopy: List<Condition>) {
        viewModel.saveCopyConditions(conditionsToCopy)
        back()
        onConditionsCopied(conditionsToCopy)
    }
}
