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

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence

sealed interface DebugReportTimelineFilter {

    fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean

    data class Time(val lowerBoundMs: Long, val upperBoundMs: Long) : DebugReportTimelineFilter {
        override fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean =
            occurrence.relativeTimestampMs !in lowerBoundMs..upperBoundMs
    }
}

internal fun List<DebugReportTimelineFilter>.shouldFilter(occurrence: DebugReportEventOccurrence): Boolean {
    forEach { filter -> if (filter.shouldFilter(occurrence)) return true }
    return false
}