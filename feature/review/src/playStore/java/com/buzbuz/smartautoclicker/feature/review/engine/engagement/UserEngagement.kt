/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.review.engine.engagement

import com.buzbuz.smartautoclicker.core.base.extensions.trim
import com.buzbuz.smartautoclicker.feature.review.engine.session.UserSession

internal fun List<UserSession>.isUserEngaged(): Boolean {
    val shortSessionEngagement = verifyEngagement(
        durationRange = SHORT_SESSION_DURATION_MS_RANGE,
        sessionCount = SHORT_SESSION_COUNT_ENGAGEMENT_THRESHOLD,
    )

    val longSessionEngagement = verifyEngagement(
        durationRange = LONG_SESSION_DURATION_MS_RANGE,
        sessionCount = LONG_SESSION_COUNT_ENGAGEMENT_THRESHOLD,
    )

    return shortSessionEngagement || longSessionEngagement
}

private fun List<UserSession>.verifyEngagement(durationRange: LongRange, sessionCount: Int): Boolean {
    // Get all session in the correct duration range, ordered from recent to old.
    // It will contain the [sessionCount] most recent user sessions
    val sessions = filter { it.durationMs in durationRange }
        .sortedByDescending { it.timestamp }
        .trim(sessionCount)

    // Not enough sessions
    if (sessions.size < sessionCount) return false
    // Sessions are not closed enough to be considered a streak
    if ((sessions.first().timestamp - sessions.last().timestamp) > ENGAGEMENT_PERIOD_DURATION_MS) return false
    // Session streak is not recent enough
    if (sessions.first().timestamp + MAX_ENGAGEMENT_AGE_MS < System.currentTimeMillis()) return false

    return true
}