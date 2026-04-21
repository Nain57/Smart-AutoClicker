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

/** Ui state for the time filter in the debug report timeline filter dialog. */
data class DebugReportTimeFilterUiState(

    /** The lower possible bound for the time filter. Usually 0. */
    val lowerBoundMs: Long,

    /** The upper possible bound for the time filter. Usually the duration of the debug report. */
    val upperBoundMs: Long,

    /**
     * The lower bound of the filter, as selected by the user.
     * Defaults to [lowerBoundMs] when the user haven't specified anything yet.
     */
    val lowerValueMs: Long,

    /**
     * The upper bound of the filter, as selected by the user.
     * Defaults to [upperBoundMs] when the user haven't specified anything yet.
     */
    val upperValueMs: Long,

    /** Display value of [lowerValueMs]. */
    val lowerValueText: String,

    /** Display value of [upperBoundMs]. */
    val upperValueText: String,
)


/** Ui state for the Image Events filter in the debug report timeline filter dialog. */
data class DebugReportImageEventFilterUiState(

    /** State of the checkbox telling if the image events should be shown or not. */
    val checkboxState: Boolean,
    /** Secondary text explaining current effect. */
    val checkboxDescId: Int,

    /** Tells if the event id selector is enabled or not. */
    val filteredIdsSelectorState: Boolean,
    /** The event ids to be filtered. */
    val filteredIds: Set<Long>,
    /** The text for the event ids to be filtered. */
    val filteredIdsText: String,
)

/** Ui state for the Trigger Events filter in the debug report timeline filter dialog. */
data class DebugReportTriggerEventFilterUiState(

    /** State of the checkbox telling if the image events should be shown or not. */
    val checkboxState: Boolean,
    /** Secondary text explaining current effect. */
    val checkboxDescId: Int,

    /** Tells if the event id selector is enabled or not. */
    val filteredIdsSelectorState: Boolean,
    /** The event ids to be filtered. */
    val filteredIds: Set<Long>,
    /** The text for the event ids to be filtered. */
    val filteredIdsText: String,
)