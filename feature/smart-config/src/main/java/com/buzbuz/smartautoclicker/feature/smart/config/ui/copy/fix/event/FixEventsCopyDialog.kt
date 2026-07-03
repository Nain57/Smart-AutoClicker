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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.event

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.newDividerWithoutHeader
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsCopyUiState

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType


class FixEventsCopyDialog(
    private val eventsToCopy: List<Event>,
    private val onFixConfirmed: (List<Event>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.FIX_EVENTS_COPY.name

    /** View model for this content. */
    private val viewModel: FixEventsCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { fixEventsCopyViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseListBinding
    /** Adapter displaying the list of events. */
    private val itemAdapter: FixEventsCopyAdapter = FixEventsCopyAdapter(::onItemClicked)


    override fun onCreateView(): ViewGroup {
        viewModel.setEventsToCopy(eventsToCopy)

        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                buttonSave.setDebouncedOnClickListener { onSaveClicked() }

                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setDebouncedOnClickListener { back() }

                dialogTitle.setText(R.string.dialog_title_copy_fix)
            }

            layoutLoadableList.list.apply {
                addItemDecoration(newDividerWithoutHeader(context))
                adapter = itemAdapter
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState)}
            }
        }
    }

    private fun onItemClicked(item: FixCopyUiItem.Item.EventItem) {
        if (item.isValidForCopy) return
        showFixChildrenDialog(item.uiEvent.event)
    }

    private fun onSaveClicked() {
        if (viewModel.uiState.value?.canBeCopied != true) return

        back()
        onFixConfirmed(viewModel.getFixedEventsToCopy())
    }

    private fun updateUiState(uiState: FixEventsCopyUiState?) {
        uiState ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeCopied)
            layoutLoadableList.updateState(uiState.items)
            itemAdapter.submitList(uiState.items)
        }
    }

    private fun showFixChildrenDialog(event: Event) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = FixEventChildrenCopyDialog(
                dialogArguments = FixEventChildrenCopyDialog.Arguments(
                    resultingEventList = viewModel.getResultingEventList(),
                    parent = event,
                    showHelpMessage = false,
                ),
                onFixConfirmed = viewModel::updateEvent,
            ),
            hideCurrent = true,
        )
    }
}
