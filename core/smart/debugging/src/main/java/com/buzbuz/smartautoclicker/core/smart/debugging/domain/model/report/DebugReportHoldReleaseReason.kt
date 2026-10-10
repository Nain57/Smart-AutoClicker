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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report

/** The reason of the release of a pointer held by an event. */
enum class DebugReportHoldReleaseReason {
    /** The conditions of the held event are no longer fulfilled. */
    UNFULFILLED,
    /** The pointer has been lifted outside of the scenario, most likely by a user interaction with the screen. */
    INTERRUPTED,
    /** The held event has been disabled. */
    EVENT_DISABLED,
    /** The gesture of another event has taken over or ended the hold. */
    OTHER_EVENT,
    /** The screen orientation has changed. */
    SCREEN_ROTATION,
    /** The scenario has been stopped. */
    SCENARIO_STOPPED,
}
