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

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportActionResult
import javax.inject.Inject

internal class EventStateRecorder @Inject constructor() {

    private val _changes: MutableList<DebugReportActionResult.EventStateChange> = mutableListOf()
    val changes: List<DebugReportActionResult.EventStateChange> = _changes

    fun onEventProcessingStarted() {
        _changes.clear()
    }

    fun onEventStateChanged(event: Event, newValue: Boolean) {
        _changes.add(
            DebugReportActionResult.EventStateChange(
                eventId = event.id.databaseId,
                newValue = newValue,
            )
        )
    }

    fun reset() {
        _changes.clear()
    }
}