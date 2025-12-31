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
package com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping

import com.buzbuz.smartautoclicker.core.smart.debugging.CounterStateChange
import com.buzbuz.smartautoclicker.core.smart.debugging.counterStateChange
import com.buzbuz.smartautoclicker.core.smart.debugging.EventStateChange
import com.buzbuz.smartautoclicker.core.smart.debugging.eventStateChange
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportActionResult


internal fun DebugReportActionResult.CounterChange.toProtobuf(): CounterStateChange =
    counterStateChange {
        counterName = this@toProtobuf.counterName
        counterOldValue = this@toProtobuf.previousValue
        counterNewValue = this@toProtobuf.newValue
    }

internal fun DebugReportActionResult.EventStateChange.toProtobuf(): EventStateChange =
    eventStateChange {
        eventId = this@toProtobuf.eventId
        newValue = this@toProtobuf.newValue
    }


internal fun CounterStateChange.toDomain(): DebugReportActionResult.CounterChange =
    DebugReportActionResult.CounterChange(
        counterName = counterName,
        previousValue = counterOldValue,
        newValue = counterNewValue,
    )

internal fun EventStateChange.toDomain(): DebugReportActionResult.EventStateChange =
    DebugReportActionResult.EventStateChange(
        eventId = eventId,
        newValue = newValue,
    )