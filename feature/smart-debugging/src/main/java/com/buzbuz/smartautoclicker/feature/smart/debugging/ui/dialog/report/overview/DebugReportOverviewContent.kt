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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeFieldDataDisplayBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ContentDebugReportOverviewBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint

import kotlinx.coroutines.launch
import kotlin.getValue


class DebugReportOverviewContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: DebugReportOverviewViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportOverviewViewModel() },
    )

    private lateinit var viewBinding: ContentDebugReportOverviewBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDebugReportOverviewBinding.inflate(LayoutInflater.from(context), container, false)
        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateOverview) }
            }
        }
    }

    private fun updateOverview(uiState: DebugReportOverviewUiState) {
        when (uiState) {
            is DebugReportOverviewUiState.Loading -> toLoadingState()
            is DebugReportOverviewUiState.NotAvailable -> toNotAvailableState()
            is DebugReportOverviewUiState.Available -> toAvailableState(uiState)
        }
    }

    private fun toLoadingState() {
        viewBinding.apply {
            loading.visibility = View.VISIBLE
            overview.visibility = View.GONE
        }
    }

    private fun toNotAvailableState() {
        dialogController.back()
    }

    private fun toAvailableState(state: DebugReportOverviewUiState.Available) {
        viewBinding.apply {
            loading.visibility = View.GONE
            overview.visibility = View.VISIBLE

            fieldScenario.bindEntry(state.scenario)
            fieldTotalDuration.bindEntry(state.totalDuration)
            fieldImgProcCount.bindEntry(state.frameCount)
            fieldAvgImgProcDur.bindEntry(state.averageFrameProcessingDuration)
            fieldImgEvtFulfilledCount.bindEntry(state.imageEventFulfilledCount)
            fieldTriggerEvtFulfilledCount.bindEntry(state.triggerEventFulfilledCount)
        }
    }

    private fun IncludeFieldDataDisplayBinding.bindEntry(entry: OverviewEntry) {
        setTitle(context.getString(entry.titleRes))
        setDescription(entry.value)
    }
}