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
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ContentDebugReportTimelineBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.adapter.DebugReportTimelineAdapter
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.DebugReportEventOccurrenceDetailsDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter.DebugReportTimelineFiltersDialog

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

    override fun floatingActionButtonsAreAvailable(): Boolean = true

    override fun primaryFloatingActionButtonIcon(): Int = R.drawable.ic_filter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDebugReportTimelineBinding.inflate(LayoutInflater.from(context), container, false).apply {
            list.adapter = timelineAdapter
            fastScroller.attachToRecyclerView(list)
            buttonClearFilters.setOnClickListener { viewModel.clearFilters() }
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

    override fun onStart() {
        updateFiltersBadge(viewModel.uiState.value.activeFilterCount())
    }

    override fun onStop() {
        dialogController.floatingActionButtons.primaryBadge.visibility = View.GONE
    }

    private fun updateUiState(uiState: DebugReportTimelineUiState) {
        when (uiState) {
            DebugReportTimelineUiState.Empty -> toEmptyState()
            is DebugReportTimelineUiState.FilteredEmpty -> toFilteredEmptyState(uiState)
            DebugReportTimelineUiState.Loading -> toLoadingState()
            DebugReportTimelineUiState.NotAvailable -> toNotAvailableState()
            is DebugReportTimelineUiState.Available -> toAvailableState(uiState)
        }
    }

    private fun toEmptyState() {
        viewBinding.apply {
            loading.visibility = View.GONE
            list.visibility = View.GONE
            fastScroller.visibility = View.GONE
            empty.visibility = View.VISIBLE
            emptyTitle.setText(R.string.title_event_occurrence_empty)
            emptySecondary.visibility = View.GONE
            buttonClearFilters.visibility = View.GONE
        }
        updateFiltersBadge(0)
    }

    private fun toFilteredEmptyState(uiState: DebugReportTimelineUiState.FilteredEmpty) {
        viewBinding.apply {
            loading.visibility = View.GONE
            list.visibility = View.GONE
            fastScroller.visibility = View.GONE
            empty.visibility = View.VISIBLE
            emptyTitle.setText(R.string.title_event_occurrence_filtered_empty)
            emptySecondary.visibility = View.VISIBLE
            emptySecondaryText.setText(R.string.desc_event_occurrence_filtered_empty)
            buttonClearFilters.visibility = View.VISIBLE
        }
        timelineAdapter.submitList(emptyList())
        updateFiltersBadge(uiState.activeFilterCount)
    }

    private fun toLoadingState() {
        viewBinding.apply {
            loading.visibility = View.VISIBLE
            list.visibility = View.GONE
            fastScroller.visibility = View.GONE
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
            fastScroller.visibility = View.VISIBLE
            empty.visibility = View.GONE
        }
        timelineAdapter.submitList(uiState.eventsOccurrences) {
            viewBinding.fastScroller.refresh()
        }
        updateFiltersBadge(uiState.activeFilterCount)
    }

    private fun updateFiltersBadge(activeFilterCount: Int) {
        dialogController.floatingActionButtons.primaryBadge.apply {
            visibility = if (activeFilterCount > 0) View.VISIBLE else View.GONE
            text = activeFilterCount.toString()
            contentDescription = context.getString(
                R.string.content_desc_timeline_filters_active,
                activeFilterCount,
            )
        }
    }

    private fun DebugReportTimelineUiState.activeFilterCount(): Int = when (this) {
        is DebugReportTimelineUiState.Available -> activeFilterCount
        is DebugReportTimelineUiState.FilteredEmpty -> activeFilterCount
        else -> 0
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

    override fun onPrimaryFloatingActionButtonClicked() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = DebugReportTimelineFiltersDialog(
                reportDurationMs = viewModel.uiState.value.let { uiState ->
                    when (uiState) {
                        is DebugReportTimelineUiState.Available -> uiState.durationMs
                        is DebugReportTimelineUiState.FilteredEmpty -> uiState.durationMs
                        else -> 0L
                    }
                },
                currentFilters = viewModel.getFilters(),
                onFiltersApplied = viewModel::setFilters,
            ),
            hideCurrent = false,
        )
    }
}
