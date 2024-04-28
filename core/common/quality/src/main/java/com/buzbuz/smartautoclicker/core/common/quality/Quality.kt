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
package com.buzbuz.smartautoclicker.core.common.quality

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Describe the different quality levels felt by the user. */
sealed class Quality(internal val backToHighDelay: Duration? = null) {

    /** The quality is not initialized yet. */
    data object Unknown : Quality()

    /** Everything is working as intended. */
    data object High : Quality()

    /** The issue has occurred due to external perturbations, such as aggressive background service management */
    data object Medium : Quality(30.minutes)

    /** The issue has occurred due to a crash of Smart AutoClicker. */
    data object Low : Quality(6.hours)

    /** The user is using the app for the first time. */
    data object FirstTime : Quality(2.hours)
}