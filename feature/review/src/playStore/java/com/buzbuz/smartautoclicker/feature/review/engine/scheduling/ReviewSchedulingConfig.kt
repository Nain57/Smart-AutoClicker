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
package com.buzbuz.smartautoclicker.feature.review.engine.scheduling

import kotlin.time.Duration.Companion.days

/** Delay minimum before the first review requests (two weeks) */
internal val DELAY_BEFORE_FIRST_REVIEW_REQUEST_MS: Long =
    14.days.inWholeMilliseconds

/** Delay minimum between two review requests (one month) */
internal val DELAY_BETWEEN_REVIEW_REQUEST_MS: Long =
    30.days.inWholeMilliseconds