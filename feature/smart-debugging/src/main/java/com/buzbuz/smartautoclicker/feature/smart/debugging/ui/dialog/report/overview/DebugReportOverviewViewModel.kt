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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.feature.smart.debugging.R

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


class DebugReportOverviewViewModel @Inject constructor(
    debuggingRepository: DebuggingRepository,
    private val smartRepository: IRepository,
) : ViewModel() {

    private val lastReportOverview: Flow<DebugReportOverview?> = debuggingRepository.getLastReportOverview()

    val uiState: StateFlow<DebugReportOverviewUiState> = lastReportOverview
        .map { overview -> overview.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugReportOverviewUiState.Loading,
        )

    private suspend fun DebugReportOverview?.toUiState(): DebugReportOverviewUiState {
        if (this == null) return DebugReportOverviewUiState.NotAvailable
        val scenario = smartRepository.getScenario(scenarioId) ?: return DebugReportOverviewUiState.NotAvailable

        return DebugReportOverviewUiState.Available(
            scenario = OverviewEntry(
                titleRes = R.string.input_field_label_scenario_name,
                value = scenario.name,
            ),
            totalDuration = OverviewEntry(
                titleRes = R.string.item_title_report_total_duration,
                value = duration.toString(),
            ),
            frameCount = OverviewEntry(
                titleRes = R.string.item_title_report_frame_processed,
                value = frameCount.toString(),
            ),
            averageFrameProcessingDuration = OverviewEntry(
                titleRes = R.string.item_title_report_avg_image_processing_duration,
                value = averageFrameProcessingDuration.toString(),
            ),
            imageEventFulfilledCount = OverviewEntry(
                titleRes = R.string.item_title_report_image_event_fulfilled,
                value = imageEventFulfilledCount.toString(),
            ),
            triggerEventFulfilledCount = OverviewEntry(
                titleRes = R.string.item_title_report_trigger_event_fulfilled,
                value = triggerEventFulfilledCount.toString(),
            ),
        )
    }

}