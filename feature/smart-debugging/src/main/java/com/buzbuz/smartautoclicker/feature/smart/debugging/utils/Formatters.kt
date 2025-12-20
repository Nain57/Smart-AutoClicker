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
package com.buzbuz.smartautoclicker.feature.smart.debugging.utils

import kotlin.time.Duration.Companion.milliseconds


internal fun Long.formatDebugDuration(): String {
    if (this < 1) return "< 1ms"

    val duration = milliseconds
    var value = ""

    if (duration.inWholeMinutes % 60 > 0) {
        value += "${duration.inWholeMinutes % 60}m "
    }

    val secondsText = "${duration.inWholeSeconds % 60}"

    val milliseconds = duration.inWholeMilliseconds % 1000
    val millisecondsText = when {
        milliseconds == 0L -> "000"
        milliseconds < 10L -> "00$milliseconds"
        milliseconds < 100L -> "0$milliseconds"
        else -> "$milliseconds"
    }
    value += "${secondsText}.${millisecondsText}s"

    return value
}

internal fun Long.formatDebugTimelineTimestamp(): String {
    val duration = milliseconds
    var value = ""

    if (duration.inWholeHours > 0) {
        value += "${duration.inWholeHours}h "
    }
    if (duration.inWholeMinutes % 60 > 0) {
        value += "${duration.inWholeMinutes % 60}m "
    }

    value += "${duration.inWholeSeconds % 60}s "

    return value.trim()
}