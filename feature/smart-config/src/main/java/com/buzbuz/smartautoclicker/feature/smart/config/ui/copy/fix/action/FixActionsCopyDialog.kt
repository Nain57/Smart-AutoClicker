/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.action

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsChildrenCopyUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.showReplaceCounterDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.showReplaceEventToggleDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.showReplaceScreenConditionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch


/**
 * Dialog allowing the user to fix the missing references of a list of actions being copied into the edited event.
 * @param actionsToCopy the copied actions.
 * @param onFixConfirmed called with the fixed copies once all references are valid.
 */
class FixActionsCopyDialog(
    private val actionsToCopy: List<Action>,
    private val onFixConfirmed: (List<Action>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.FIX_ACTIONS_COPY.name

    /** View model for this content. */
    private val viewModel: FixActionsCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { fixActionsCopyViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseListBinding
    /** Adapter displaying the list of actions to fix. */
    private val itemAdapter: FixEventChildrenCopyAdapter = FixEventChildrenCopyAdapter(
        onMissingReferenceClicked = ::onMissingReferenceClicked,
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setActionsToCopy(actionsToCopy)

        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                buttonSave.setDebouncedOnClickListener { onSaveClicked() }

                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setDebouncedOnClickListener { back() }

                dialogTitle.setText(R.string.dialog_title_copy_fix)
            }

            layoutLoadableList.list.adapter = itemAdapter
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun updateUiState(uiState: FixEventsChildrenCopyUiState?) {
        uiState ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeCopied)
            layoutLoadableList.updateState(uiState.items)
            itemAdapter.submitList(uiState.items)
        }
    }

    private fun onSaveClicked() {
        if (viewModel.uiState.value?.canBeCopied != true) return

        back()
        onFixConfirmed(viewModel.getFixedActionsToCopy())
    }

    private fun onMissingReferenceClicked(item: FixCopyUiItem.Item.EventChildren, reference: MissingCopyReference) {
        if (item !is FixCopyUiItem.Item.EventChildren.ActionItem) return

        when (reference) {
            is MissingCopyReference.EventToggleReference -> {
                val action = item.uiAction.action as? ToggleEvent ?: return

                viewModel.startActionEdition(action)
                showReplaceEventToggleDialog(
                    scenarioEvents = viewModel.getScenarioEvents(),
                    action = action,
                    onEventTogglesSelected = { replacement ->
                        viewModel.stopActionEdition()
                        viewModel.updateEventToggles(item, reference, replacement)
                    },
                    onDismissed = { viewModel.stopActionEdition() },
                )
            }

            is MissingCopyReference.ScreenConditionReference -> showReplaceScreenConditionDialog(
                candidates = viewModel.getScreenConditionReplacementCandidates(),
                onScreenConditionSelected = { replacement ->
                    viewModel.updateScreenCondition(item, reference, replacement)
                },
            )

            is MissingCopyReference.CounterReference -> showReplaceCounterDialog { replacement ->
                viewModel.updateCounter(item, reference, replacement)
            }
        }
    }
}
