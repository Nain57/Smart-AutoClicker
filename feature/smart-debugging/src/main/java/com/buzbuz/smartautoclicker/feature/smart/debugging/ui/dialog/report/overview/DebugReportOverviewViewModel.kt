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
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.buildEventActivityReport
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.toScreenEventActivitySources
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity.toTriggerEventActivitySources

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class DebugReportOverviewViewModel @Inject constructor(
    debuggingRepository: DebuggingRepository,
    smartRepository: IRepository,
) : ViewModel() {

    private val reportData = debuggingRepository.getLastReportOverview().flatMapLatest { overview ->
        if (overview == null) return@flatMapLatest flowOf(null)

        combine(
            smartRepository.getScenarioFlow(overview.scenarioId),
            smartRepository.getScreenEventsFlow(overview.scenarioId),
            smartRepository.getTriggerEventsFlow(overview.scenarioId),
            debuggingRepository.getLastReportEventsOccurrences(),
        ) { scenario, screenEvents, triggerEvents, occurrences ->
            OverviewReportData(overview, scenario, screenEvents, triggerEvents, occurrences)
        }
    }

    val uiState: StateFlow<DebugReportOverviewUiState> = reportData
        .map { data -> data.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugReportOverviewUiState.Loading,
        )

    private fun OverviewReportData?.toUiState(): DebugReportOverviewUiState {
        if (this == null || scenario == null || occurrences == null) return DebugReportOverviewUiState.NotAvailable
        val activity = buildEventActivityReport(
            screenEvents = screenEvents.toScreenEventActivitySources(),
            triggerEvents = triggerEvents.toTriggerEventActivitySources(),
            occurrences = occurrences,
        )

        return DebugReportOverviewUiState.Available(
            scenario = OverviewEntry(
                titleRes = R.string.input_field_label_scenario_name,
                value = scenario.name,
            ),
            totalDuration = OverviewEntry(
                titleRes = R.string.item_title_report_total_duration,
                value = overview.duration.toString(),
            ),
            frameCount = OverviewEntry(
                titleRes = R.string.item_title_report_frame_processed,
                value = overview.frameCount.toString(),
            ),
            averageFrameProcessingDuration = OverviewEntry(
                titleRes = R.string.item_title_report_avg_image_processing_duration,
                value = overview.averageFrameProcessingDuration.toString(),
            ),
            imageEventFulfilledCount = OverviewEntry(
                titleRes = R.string.item_title_report_image_event_fulfilled,
                value = overview.imageEventFulfilledCount.toString(),
            ),
            triggerEventFulfilledCount = OverviewEntry(
                titleRes = R.string.item_title_report_trigger_event_fulfilled,
                value = overview.triggerEventFulfilledCount.toString(),
            ),
            eventActivity = EventActivitySummary(
                reachedEventCount = activity.reachedEventCount,
                totalOccurrenceCount = activity.totalOccurrenceCount,
                mostFrequentEventName = activity.mostFrequentEvent?.name,
                mostFrequentEventCount = activity.mostFrequentEvent?.occurrenceCount,
            ),
        )
    }

}

private data class OverviewReportData(
    val overview: DebugReportOverview,
    val scenario: Scenario?,
    val screenEvents: List<ScreenEvent>,
    val triggerEvents: List<TriggerEvent>,
    val occurrences: List<DebugReportEventOccurrence>?,
)
