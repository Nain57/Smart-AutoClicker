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

/**
 * The state of a event after an event occurrence.
 *
 * @param eventId the unique identifier of the event.
 * @param eventName the name of the event.
 * @param currentValue the event state after the execution of the event occurrence.
 * @param previousValue the event state before the execution of the event occurrence. Null if the value was not changed by
 * this event occurrence.
 */
sealed class DebugEventOccurrenceEventState {

    abstract val eventId: Long
    abstract val eventName: String
    abstract val currentValue: Boolean
    abstract val previousValue: Boolean?

    data class Image(
        override val eventId: Long,
        override val eventName: String,
        override val currentValue: Boolean,
        override val previousValue: Boolean? = null,
        val eventPriority: Int,
    ) : DebugEventOccurrenceEventState()

    data class Trigger(
        override val eventId: Long,
        override val eventName: String,
        override val currentValue: Boolean,
        override val previousValue: Boolean? = null
    ) : DebugEventOccurrenceEventState()
}
