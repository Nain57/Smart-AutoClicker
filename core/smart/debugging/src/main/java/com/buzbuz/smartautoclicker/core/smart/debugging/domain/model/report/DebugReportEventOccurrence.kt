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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report

/** Base class for the occurrence of an event during a session with debug report enabled. */
sealed class DebugReportEventOccurrence {

    /** The unique identifier of the ImageEvent that has been detected. */
    abstract val eventId: Long
    /** Time since session start at which this event has occurred in milliseconds. */
    abstract val relativeTimestampMs: Long
    /** The results for all conditions interpreted for this event occurrence.*/
    abstract val conditionsResults: List<DebugReportConditionResult>
    /** The list of value changes for the counters. Empty if none have changed. */
    abstract val counterChanges: List<DebugReportActionResult.CounterChange>

    /** A TriggerEvent has been fulfilled. */
    data class TriggerEvent(
        override val eventId: Long,
        override val relativeTimestampMs: Long,
        override val conditionsResults: List<DebugReportConditionResult.TriggerCondition>,
        override val counterChanges: List<DebugReportActionResult.CounterChange>,
    ) : DebugReportEventOccurrence()

    /**
     * An ImageEvent has been fulfilled.
     *
     * @param frameNumber the number of the frame in the current detection session.
     */
    data class ImageEvent(
        override val eventId: Long,
        override val relativeTimestampMs: Long,
        override val conditionsResults: List<DebugReportConditionResult.ImageCondition>,
        override val counterChanges: List<DebugReportActionResult.CounterChange>,
        val frameNumber: Long,
    ) : DebugReportEventOccurrence()
}