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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

internal class DurationRecorder {

    var startTimestampMs: Long = 0L
        private set

    /** Start the duration measuring with now as start time. */
    fun start() {
        startTimestampMs = System.currentTimeMillis()
    }

    /** Duration in milliseconds since [start] call. */
    fun durationMs(): Long =
        if (startTimestampMs == 0L) 0 else System.currentTimeMillis() - startTimestampMs

    /** Reset the duration measuring. [startTimestampMs] & [durationMs] will be set to 0. */
    fun reset() {
        startTimestampMs = 0
    }
}