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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ContentDebugReportTimelineBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.adapter.DebugReportTimelineAdapter
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.DebugReportEventOccurrenceDetailsDialog

import kotlinx.coroutines.launch
import kotlin.getValue


class DebugReportTimelineContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: DebugReportTimelineViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportTimelineViewModel() },
    )

    private val timelineAdapter: DebugReportTimelineAdapter = DebugReportTimelineAdapter(
        onItemClicked = ::onEventOccurrenceClicked,
    )

    private lateinit var viewBinding: ContentDebugReportTimelineBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDebugReportTimelineBinding.inflate(LayoutInflater.from(context), container, false).apply {
            list.adapter = timelineAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun updateUiState(uiState: DebugReportTimelineUiState) {
        when (uiState) {
            DebugReportTimelineUiState.Empty -> toEmptyState()
            DebugReportTimelineUiState.Loading -> toLoadingState()
            DebugReportTimelineUiState.NotAvailable -> toNotAvailableState()
            is DebugReportTimelineUiState.Available -> toAvailableState(uiState)
        }
    }

    private fun toEmptyState() {
        viewBinding.apply {
            loading.visibility = View.GONE
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
        }
    }

    private fun toLoadingState() {
        viewBinding.apply {
            loading.visibility = View.VISIBLE
            list.visibility = View.GONE
            empty.visibility = View.GONE
        }
    }

    private fun toNotAvailableState() {
        dialogController.back()
    }

    private fun toAvailableState(uiState: DebugReportTimelineUiState.Available) {
        viewBinding.apply {
            loading.visibility = View.GONE
            list.visibility = View.VISIBLE
            empty.visibility = View.GONE
        }
        timelineAdapter.submitList(uiState.eventsOccurrences)
    }

    private fun onEventOccurrenceClicked(occurrence: DebugReportTimelineEventOccurrenceItem) {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = DebugReportEventOccurrenceDetailsDialog(
                scenarioId = occurrence.scenarioId,
                eventOccurrence = occurrence.occurrence,
            ),
            hideCurrent = false,
        )
    }
}