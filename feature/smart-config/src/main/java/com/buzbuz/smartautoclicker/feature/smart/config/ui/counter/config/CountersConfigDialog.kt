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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConfirmationDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation.CounterCreationDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference.CounterReferenceDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue


class CountersConfigDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** View model for this dialog. */
    private val viewModel: CountersConfigViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { countersViewModel() },
    )

    private lateinit var viewBinding: DialogBaseListBinding
    private lateinit var countersAdapter: CountersConfigAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_counters_config)

                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setIconResource(R.drawable.ic_back)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            floatingButtonsLayout.visibility = View.VISIBLE
            buttonCopy.visibility = View.GONE
            buttonNew.visibility = View.VISIBLE
            buttonNew.setDebouncedOnClickListener { showCounterCreationDialog() }

            countersAdapter = CountersConfigAdapter(
                onExpandCollapse = ::onExpandClicked,
                onStartingValueChange = viewModel::setStartingValue,
                onSetByClick = ::showSetByDialog,
                onReadByClick = ::showReadByDialog,
                onDeleteClick = ::onDeleteClicked,
                onCounterClicked = ::onCounterClicked,
                onCancelReplace = viewModel::cancelReplacement,
            )
            layoutLoadableList.apply {
                list.adapter = countersAdapter
                setEmptyText(
                    id = R.string.message_empty_counter_name_list_title,
                    secondaryId =R.string.message_empty_counter_name_list_desc
                )
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUi) }
            }
        }
    }

    override fun back() {
        if (viewModel.getUiState() is CountersUiState.Replacing) return
        super.back()
    }

    private fun onExpandClicked(counter: CounterUiItem) {
        when (viewModel.getUiState()) {
            is CountersUiState.Loaded -> viewModel.expandCollapseItem(counter)
            is CountersUiState.Replacing -> viewModel.replaceAndDelete(counter)
            else -> Unit
        }
    }

    private fun onDeleteClicked(counter: CounterUiItem) {
        if (viewModel.getUiState() !is CountersUiState.Loaded) return

        if (counter.readByButtonIsEmpty && counter.setByButtonIsEmpty) {
            showDeleteDialog(counter)
        } else {
            viewModel.selectForReplacement(counter)
        }
    }

    private fun onCounterClicked(counter: CounterUiItem) {
        when (viewModel.getUiState()) {
            is CountersUiState.Replacing ->
                viewModel.replaceAndDelete(counter)
            is CountersUiState.Loaded ->
                if (!counter.isExpanded) viewModel.expandCollapseItem(counter)
            else -> Unit
        }
    }

    private fun updateUi(uiState: CountersUiState?) {
        if (uiState == null) return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(
                buttonType = DialogNavigationButton.DISMISS,
                enabled = uiState is CountersUiState.Loaded,
            )

            when (uiState) {
                CountersUiState.Loading ->
                    layoutLoadableList.updateState(null)

                CountersUiState.Empty ->
                    layoutLoadableList.updateState(emptyList())

                is CountersUiState.Loaded -> {
                    layoutLoadableList.updateState(uiState.counterItems)
                    countersAdapter.submitList(uiState.counterItems)
                }

                is CountersUiState.Replacing -> {
                    layoutLoadableList.updateState(uiState.counterItems)
                    countersAdapter.submitList(uiState.counterItems)
                }
            }
        }
    }

    private fun showSetByDialog(counter: CounterUiItem) {
        if (viewModel.getUiState() !is CountersUiState.Loaded) return

        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterReferenceDialog(
                counterName = counter.counterName,
                type = CounterReferenceDialog.ReferencesType.WRITE,
            ),
            hideCurrent = false,
        )
    }

    private fun showReadByDialog(counter: CounterUiItem) {
        if (viewModel.getUiState() !is CountersUiState.Loaded) return

        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterReferenceDialog(
                counterName = counter.counterName,
                type = CounterReferenceDialog.ReferencesType.READ,
            ),
            hideCurrent = false,
        )
    }

    private fun showDeleteDialog(counter: CounterUiItem) {
        context.showDeleteConfirmationDialog {
            viewModel.deleteCounter(counter)
        }
    }

    private fun showCounterCreationDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterCreationDialog(),
            hideCurrent = false,
        )
    }
}