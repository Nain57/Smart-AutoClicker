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

import android.content.Context
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.formatDebugTimelineTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class DebugReportTimelineFiltersViewModel @Inject constructor() : ViewModel() {

    private val _timeUiState: MutableStateFlow<DebugReportTimeFilterUiState?> = MutableStateFlow(null)
    val timeUiState: StateFlow<DebugReportTimeFilterUiState?> = _timeUiState

    private val _imageEventsUiState: MutableStateFlow<DebugReportImageEventFilterUiState?> = MutableStateFlow(null)
    val imageEventsUiState: StateFlow<DebugReportImageEventFilterUiState?> = _imageEventsUiState

    private val _triggerEventsUiState: MutableStateFlow<DebugReportTriggerEventFilterUiState?> = MutableStateFlow(null)
    val triggerEventsUiState: StateFlow<DebugReportTriggerEventFilterUiState?> = _triggerEventsUiState


    fun setupUserValues(context: Context, reportDurationMs: Long, filters: List<DebugReportTimelineFilter>) {
        _timeUiState.update {
            buildTimeFilterUiState(
                durationMs = reportDurationMs,
                userFilter = filters.find { it is DebugReportTimelineFilter.Time } as? DebugReportTimelineFilter.Time,
            )
        }

        _imageEventsUiState.update {
            buildImageEventsFilterUiState(
                context = context,
                userFilter = filters.find { it is DebugReportTimelineFilter.Events.Image }
                        as? DebugReportTimelineFilter.Events.Image,
            )
        }

        _triggerEventsUiState.update {
            buildTriggerEventsFilterUiState(
                context = context,
                userFilter = filters.find { it is DebugReportTimelineFilter.Events.Trigger }
                        as? DebugReportTimelineFilter.Events.Trigger,
            )
        }
    }

    fun setUserTimeLowerBound(lowerBoundMs: Long) {
        val value = lowerBoundMs.coerceIn(0, _timeUiState.value?.upperValueMs ?: 0)
        _timeUiState.update { previous ->
            previous?.copy(
                lowerValueMs = value,
                lowerValueText = value.formatDebugTimelineTimestamp(),
            )
        }
    }

    fun setUserTimeUpperBound(upperBoundMs: Long) {
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

    fun toggleShowImageEvents() {
        _imageEventsUiState.update { previous ->
            previous?.copy(
                checkboxState = !previous.checkboxState,
                filteredIdsSelectorState = !previous.checkboxState,
            )
        }
    }

    fun toggleShowTriggerEvents() {
        _triggerEventsUiState.update { previous ->
            previous?.copy(
                checkboxState = !previous.checkboxState,
                filteredIdsSelectorState = !previous.checkboxState,
            )
        }
    }

    fun setEventsFilter(context: Context, filter: DebugReportTimelineFilter.Events) {
        when (filter) {
            is DebugReportTimelineFilter.Events.Image -> _imageEventsUiState.update { previous ->
                previous?.copy(
                    filteredIds = filter.filteredIds,
                    filteredIdsText = filter.filteredIds.getDisplayText(context, previous.checkboxState),
                )
            }

            is DebugReportTimelineFilter.Events.Trigger -> _triggerEventsUiState.update { previous ->
                previous?.copy(
                    filteredIds = filter.filteredIds,
                    filteredIdsText = filter.filteredIds.getDisplayText(context, previous.checkboxState),
                )
            }
        }
    }

    fun getImageEventsFilter(): DebugReportTimelineFilter.Events.Image {
        val state = _imageEventsUiState.value ?: return DebugReportTimelineFilter.Events.Image()
        return DebugReportTimelineFilter.Events.Image(
            filterAll = !state.checkboxState,
            filteredIds = state.filteredIds,
        )
    }

    fun getTriggerEventsFilter(): DebugReportTimelineFilter.Events.Trigger {
        val state = _triggerEventsUiState.value ?: return DebugReportTimelineFilter.Events.Trigger()
        return DebugReportTimelineFilter.Events.Trigger(
            filterAll = !state.checkboxState,
            filteredIds = state.filteredIds,
        )
    }

    fun getAllFilters(): List<DebugReportTimelineFilter> =
        buildList {
            _timeUiState.value?.let { state ->
                add(DebugReportTimelineFilter.Time(
                    lowerBoundMs = state.lowerValueMs,
                    upperBoundMs = state.upperValueMs,
                ))
            }
            _imageEventsUiState.value?.let { state ->
                add(DebugReportTimelineFilter.Events.Image(
                    filterAll = !state.checkboxState,
                    filteredIds = state.filteredIds,
                ))
            }
            _triggerEventsUiState.value?.let { state ->
                add(DebugReportTimelineFilter.Events.Trigger(
                    filterAll = !state.checkboxState,
                    filteredIds = state.filteredIds,
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

    private fun buildImageEventsFilterUiState(
        context: Context,
        userFilter: DebugReportTimelineFilter.Events.Image?,
    ): DebugReportImageEventFilterUiState {

        val showEvents = userFilter?.filterAll?.not() ?: true
        val filteredIds = userFilter?.filteredIds ?: emptySet()
        return DebugReportImageEventFilterUiState(
            checkboxState = showEvents,
            checkboxDescId = if (showEvents) 0 else 1,
            filteredIdsSelectorState = showEvents,
            filteredIds = filteredIds,
            filteredIdsText = filteredIds.getDisplayText(context, showEvents),
        )
    }

    private fun buildTriggerEventsFilterUiState(
        context: Context,
        userFilter: DebugReportTimelineFilter.Events.Trigger?,
    ): DebugReportTriggerEventFilterUiState {

        val showEvents = userFilter?.filterAll?.not() ?: true
        val filteredIds = userFilter?.filteredIds ?: emptySet()
        return DebugReportTriggerEventFilterUiState(
            checkboxState = showEvents,
            checkboxDescId = if (showEvents) 0 else 1,
            filteredIdsSelectorState = showEvents,
            filteredIds = filteredIds,
            filteredIdsText = filteredIds.getDisplayText(context, showEvents),
        )
    }
}

private fun Set<Long>.getDisplayText(context: Context, enabled: Boolean): String =
    when {
        isEmpty() || !enabled -> context.getString(R.string.field_debug_filter_events_show_select_desc_none)
        else -> context.getString(R.string.field_debug_filter_events_show_select_desc, size)
    }