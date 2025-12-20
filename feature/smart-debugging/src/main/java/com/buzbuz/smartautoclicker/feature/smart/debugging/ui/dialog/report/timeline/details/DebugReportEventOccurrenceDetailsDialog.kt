/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.DialogEventOccurrenceDetailsBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details.adapter.EventOccurrenceItemAdapter

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue


class DebugReportEventOccurrenceDetailsDialog(
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
): OverlayDialog(R.style.AppTheme) {

    /** View model for this content. */
    private val viewModel: DebugReportEventOccurrenceDetailsViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportEventOccurrenceViewModel() },
    )

    private lateinit var conditionsAdapter: EventOccurrenceItemAdapter
    private lateinit var viewBinding: DialogEventOccurrenceDetailsBinding


    override fun onCreateView(): ViewGroup {
        conditionsAdapter = EventOccurrenceItemAdapter(viewModel::getConditionBitmap)
        viewBinding = DialogEventOccurrenceDetailsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            layoutLoadableList.list.adapter = conditionsAdapter
        }

        viewModel.setOccurrence(scenarioId, eventOccurrence)

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun updateUiState(uiState: DebugReportEventOccurrenceUiState) {
        when (uiState) {
            DebugReportEventOccurrenceUiState.Loading -> viewBinding.layoutLoadableList.updateState(null)
            is DebugReportEventOccurrenceUiState.Available -> {
                viewBinding.apply {
                    layoutTopBar.dialogTitle.text = uiState.eventName
                    layoutLoadableList.updateState(uiState.items)
                }
                conditionsAdapter.submitList(uiState.items)
            }
        }
    }
}