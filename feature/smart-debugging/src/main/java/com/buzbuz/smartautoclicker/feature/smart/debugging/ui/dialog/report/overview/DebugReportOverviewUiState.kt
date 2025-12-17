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

import androidx.annotation.StringRes


sealed interface DebugReportOverviewUiState {

    data object Loading : DebugReportOverviewUiState
    data object NotAvailable : DebugReportOverviewUiState
    data class Available(
        val scenario: OverviewEntry,
        val totalDuration: OverviewEntry,
        val frameCount: OverviewEntry,
        val averageFrameProcessingDuration: OverviewEntry,
        val imageEventFulfilledCount: OverviewEntry,
        val triggerEventFulfilledCount: OverviewEntry,
    ) : DebugReportOverviewUiState
}

data class OverviewEntry(
    @field:StringRes val titleRes: Int,
    val value: String,
)