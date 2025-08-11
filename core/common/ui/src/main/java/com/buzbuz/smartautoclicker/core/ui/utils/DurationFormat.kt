
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