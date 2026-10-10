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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import javax.inject.Inject

/**
 * Records the hold of a pointer by an event.
 *
 * The debug engine handles the processing callbacks asynchronously, so all timestamps are provided by the caller and
 * captured when the callback is received, not when it is handled.
 */
internal class HeldEventRecorder @Inject constructor() {

    /** The event currently holding a pointer, if any. */
    var heldEvent: Event? = null
        private set

    /** The timestamp of the start of the current hold, in milliseconds. 0 if nothing is held. */
    var holdStartTimestampMs: Long = 0L
        private set

    fun onHeldEventChanged(event: Event?, timestampMs: Long) {
        heldEvent = event
        holdStartTimestampMs = if (event != null) timestampMs else 0L
    }

    /** @return the duration of the hold of [event] at [timestampMs], or 0 if [event] is not the held one. */
    fun getHoldDurationMs(event: Event, timestampMs: Long): Long =
        if (heldEvent?.id != event.id) 0L
        else (timestampMs - holdStartTimestampMs).coerceAtLeast(0L)

    fun reset() {
        heldEvent = null
        holdStartTimestampMs = 0L
    }
}
