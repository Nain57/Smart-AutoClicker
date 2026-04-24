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

/** Types of filters that can be applied to the debug timeline. */
sealed interface DebugReportTimelineFilter {

    /** @return true if the event should be filtered by this filter, false if not. */
    fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean

    /**
     * Filter on a range of time.
     * @param lowerBoundMs The lower bound of the filter in milliseconds.
     * @param upperBoundMs The upper bound of the filter in milliseconds.
     */
    data class Time(val lowerBoundMs: Long, val upperBoundMs: Long) : DebugReportTimelineFilter {
        override fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean =
            occurrence.relativeTimestampMs !in lowerBoundMs..upperBoundMs
    }

    sealed interface Events : DebugReportTimelineFilter {

        /** If true, filter all ImageEvents/TriggerEvents occurrences. */
        val filterAll: Boolean
        /** Only used if [filterAll] is false. Specify the ids of the Events to filters.*/
        val filteredIds: Set<Long>

        fun copyFilter(filteredIds: Set<Long>): Events =
            when (this) {
                is Image -> copy(filteredIds = filteredIds)
                is Trigger -> copy(filteredIds = filteredIds)
            }

        /** Filter on ImageEvents. */
        data class Image(override val filterAll: Boolean = false, override val filteredIds: Set<Long> = emptySet()) : Events {
            override fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean =
                occurrence is DebugReportEventOccurrence.ImageEvent && filterAll || filteredIds.contains(occurrence.eventId)
        }

        /** Filter on TriggerEvents. */
        data class Trigger(override val filterAll: Boolean = false, override val filteredIds: Set<Long> = emptySet()) : Events {
            override fun shouldFilter(occurrence: DebugReportEventOccurrence): Boolean =
                occurrence is DebugReportEventOccurrence.TriggerEvent && filterAll || filteredIds.contains(occurrence.eventId)
        }
    }
}

/** Convenience method to filter an event occurrence on the timeline against a list of filters. */
internal fun List<DebugReportTimelineFilter>.shouldFilter(occurrence: DebugReportEventOccurrence): Boolean {
    forEach { filter -> if (filter.shouldFilter(occurrence)) return true }
    return false
}