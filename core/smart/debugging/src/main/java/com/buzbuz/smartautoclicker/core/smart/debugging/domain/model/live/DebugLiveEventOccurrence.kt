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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

/** Event occurrence during a live debugging session. */
sealed interface DebugLiveEventOccurrence {
    val event: Event
    val fulfilled: Boolean
    val fulfilledCount: Int
    val processingDurationMs: Long
    val timestamp: Long
    val conditionsResults: List<DebugLiveEventConditionResult>

    data class Screen(
        override val event: ScreenEvent,
        override val fulfilled: Boolean,
        override val fulfilledCount: Int,
        override val processingDurationMs: Long,
        override val timestamp: Long,
        override val conditionsResults: List<DebugLiveEventConditionResult.Screen>,
    ) : DebugLiveEventOccurrence

    data class Trigger(
        override val event: TriggerEvent,
        override val fulfilled: Boolean,
        override val fulfilledCount: Int,
        override val processingDurationMs: Long,
        override val timestamp: Long,
        override val conditionsResults: List<DebugLiveEventConditionResult.Trigger>,
    ) : DebugLiveEventOccurrence
}

