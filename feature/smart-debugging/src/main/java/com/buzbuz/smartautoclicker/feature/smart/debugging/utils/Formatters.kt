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

import java.math.BigDecimal
import java.math.RoundingMode
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

    value += "${duration.inWholeHours}:".padStart(3, '0')
    value += "${duration.inWholeMinutes % 60}:".padStart(3, '0')
    value += "${duration.inWholeSeconds % 60}.".padStart(3, '0')
    value += "${duration.inWholeMilliseconds % 1000}".padStart(3, '0')

    return value.trim()
}

/** Compact elapsed duration for Timeline phase labels. */
internal fun Long.formatDebugTimelinePhaseDurationValue(): String {
    if (this < NANOSECONDS_PER_MILLISECOND) return "+< 1"

    val valueMs = BigDecimal.valueOf(this, 6)
    val rounded = if (valueMs < ONE_THOUSAND) {
        valueMs.setScale(1, RoundingMode.HALF_UP).let { oneDecimal ->
            if (oneDecimal < ONE_THOUSAND) oneDecimal else oneDecimal.setScale(0, RoundingMode.HALF_UP)
        }
    } else {
        valueMs.setScale(0, RoundingMode.HALF_UP)
    }
    return "+${rounded.toPlainString()}"
}

/** Compact timestamp used for manually selecting a Timeline filter range. */
internal fun Long.formatDebugTimelineFilterTimestamp(): String {
    val duration = coerceAtLeast(0L).milliseconds

    return when {
        duration.inWholeMinutes < 1 -> {
            val tenths = duration.inWholeMilliseconds / 100L
            val wholeSeconds = tenths / 10L
            val decimal = tenths % 10L
            if (decimal == 0L) "${wholeSeconds}s" else "${wholeSeconds}.${decimal}s"
        }
        duration.inWholeHours < 1 ->
            "${duration.inWholeMinutes}m ${duration.inWholeSeconds % 60}s"
        else ->
            "${duration.inWholeHours}h ${duration.inWholeMinutes % 60}m"
    }
}

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
private val ONE_THOUSAND = BigDecimal.valueOf(1_000L)
