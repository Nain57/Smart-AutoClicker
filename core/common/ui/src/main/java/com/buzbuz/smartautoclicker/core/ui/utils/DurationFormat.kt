/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.utils

import kotlin.time.Duration.Companion.milliseconds

/**
 * Format a duration into a human readable string.
 * @param msDuration the duration to be formatted in milliseconds.
 * @return the formatted duration.
 */
fun formatDuration(msDuration: Long): String {
    val duration = msDuration.milliseconds
    var value = ""
    if (duration.inWholeHours > 0) {
        value += "${duration.inWholeHours}h "
    }
    if (duration.inWholeMinutes % 60 > 0) {
        value += "${duration.inWholeMinutes % 60}m "
    }
    if (duration.inWholeSeconds % 60 > 0) {
        value += "${duration.inWholeSeconds % 60}s "
    }
    if (duration.inWholeMilliseconds % 1000 > 0) {
        value += "${duration.inWholeMilliseconds % 1000}ms "
    }

    return value.trim()
}