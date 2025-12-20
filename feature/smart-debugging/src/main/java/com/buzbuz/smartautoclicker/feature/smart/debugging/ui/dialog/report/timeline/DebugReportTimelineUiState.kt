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

import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence


sealed interface DebugReportTimelineUiState {

    data object Loading : DebugReportTimelineUiState
    data object NotAvailable : DebugReportTimelineUiState
    data object Empty: DebugReportTimelineUiState
    data class Available(
        val eventsOccurrences: List<DebugReportTimelineEventOccurrenceItem>,
    ) : DebugReportTimelineUiState

}

data class DebugReportTimelineEventOccurrenceItem(
    val id: Int,
    val scenarioId: Long,
    val eventName: String,
    val timeText: String,
    val occurrenceText: String,
    val conditionsText: String,
    val actions: List<DebugReportTimelineEventActionItem>,
    val occurrence: DebugReportEventOccurrence,
)

data class DebugReportTimelineEventActionItem(
    val id: Int,
    @field:DrawableRes val iconRes: Int,
)