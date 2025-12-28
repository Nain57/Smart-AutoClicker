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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition.adapter.EventOccurrenceItemAdapter

import kotlinx.coroutines.launch
import kotlin.getValue

class DebugConditionContent(
    appContext: Context,
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
) : NavBarDialogContent(appContext) {

    private val viewModel: DebugConditionContentViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugConditionContentViewModel() },
    )

    private lateinit var conditionsAdapter: EventOccurrenceItemAdapter
    private lateinit var viewBinding: IncludeLoadableListBinding


    override fun onCreateView(container: ViewGroup): ViewGroup {
        conditionsAdapter = EventOccurrenceItemAdapter(viewModel::getConditionBitmap)

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false)
        viewBinding.list.adapter = conditionsAdapter

        viewModel.setOccurrence(scenarioId, eventOccurrence)

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun updateUiState(uiState: DebugConditionContentUiState) {
        when (uiState) {
            DebugConditionContentUiState.Loading -> viewBinding.updateState(null)
            is DebugConditionContentUiState.Available -> {
                viewBinding.updateState(uiState.items)
                conditionsAdapter.submitList(uiState.items)
            }
        }
    }
}