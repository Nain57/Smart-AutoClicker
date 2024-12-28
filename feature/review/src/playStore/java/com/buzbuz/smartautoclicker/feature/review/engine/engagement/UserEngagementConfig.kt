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

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


/** Number of short user sessions performed during [ENGAGEMENT_PERIOD_DURATION_MS] for the user to be considered as engaged. */
internal const val SHORT_SESSION_COUNT_ENGAGEMENT_THRESHOLD = 8
/** Duration range for a user session to be considered as short. */
internal val SHORT_SESSION_DURATION_MS_RANGE: LongRange =
    5.minutes.inWholeMilliseconds until 20.minutes.inWholeMilliseconds

/** Number of long user sessions performed during [ENGAGEMENT_PERIOD_DURATION_MS] for the user to be considered as engaged. */
internal const val LONG_SESSION_COUNT_ENGAGEMENT_THRESHOLD = 4
/** Duration range for a user session to be considered as long. */
internal val LONG_SESSION_DURATION_MS_RANGE: LongRange =
    20.minutes.inWholeMilliseconds..Long.MAX_VALUE

/** Duration in between the user must have done x session in order to be considered engaged with the app. */
internal val ENGAGEMENT_PERIOD_DURATION_MS =
    14.days.inWholeMilliseconds

/** Maximum time since the last engagement period to be still considered engaged. */
internal val MAX_ENGAGEMENT_AGE_MS =
    7.days.inWholeMilliseconds