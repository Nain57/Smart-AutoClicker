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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.filter

import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.formatDebugTimelineTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class DebugReportTimelineFiltersViewModel  @Inject constructor() : ViewModel() {

    private val _timeUiState: MutableStateFlow<DebugReportTimeFilterUiState?> = MutableStateFlow(null)
    val timeUiState: StateFlow<DebugReportTimeFilterUiState?> = _timeUiState

    fun setupUserValues(reportDurationMs: Long, filters: List<DebugReportTimelineFilter>) {
        _timeUiState.update {
            buildTimeFilterUiState(
                durationMs = reportDurationMs,
                userFilter = filters.find { it is DebugReportTimelineFilter.Time } as? DebugReportTimelineFilter.Time,
            )
        }
    }

    fun setUserLowerBound(lowerBoundMs: Long) {
        val value = lowerBoundMs.coerceIn(0, _timeUiState.value?.upperValueMs ?: 0)
        _timeUiState.update { previous ->
            previous?.copy(
                lowerValueMs = value,
                lowerValueText = value.formatDebugTimelineTimestamp(),
            )
        }
    }

    fun setUserUpperBound(upperBoundMs: Long) {
        val value = _timeUiState.value?.let { filter ->
            upperBoundMs.coerceIn(filter.lowerValueMs + 1, filter.upperBoundMs)
        } ?: 0

        _timeUiState.update { previous ->
            previous?.copy(
                upperValueMs = value,
                upperValueText = value.formatDebugTimelineTimestamp(),
            )
        }
    }

    fun getFilters(): List<DebugReportTimelineFilter> =
        buildList {
            _timeUiState.value?.let { state ->
                add(DebugReportTimelineFilter.Time(
                    lowerBoundMs = state.lowerValueMs,
                    upperBoundMs = state.upperValueMs,
                ))
            }
        }

    private fun buildTimeFilterUiState(
        durationMs: Long,
        userFilter: DebugReportTimelineFilter.Time?,
    ): DebugReportTimeFilterUiState {

        val lowerValue = userFilter?.lowerBoundMs ?: 0
        var upperValue = userFilter?.upperBoundMs ?: durationMs
        if (upperValue <= lowerValue) upperValue = lowerValue + 1

        return DebugReportTimeFilterUiState(
            lowerBoundMs = 0,
            upperBoundMs = durationMs,
            lowerValueMs = lowerValue,
            upperValueMs = upperValue,
            lowerValueText = lowerValue.formatDebugTimelineTimestamp(),
            upperValueText = upperValue.formatDebugTimelineTimestamp(),
        )
    }
}