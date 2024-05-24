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
package com.buzbuz.smartautoclicker.core.common.quality.data


/**
 * The different metrics used to determine the current software quality felt by the user.
 *
 * @param lastServiceStartTimeMs Start time in milliseconds of the accessibility service.
 * @param lastScenarioStartTimeMs Start time of the last user selected scenario in the activity.
 * @param accessibilityLossCount The number of times we have lost the permission since the last troubleshooting dialog
 *                               display.
 * @param troubleshootingDisplayCount The number of times the troubleshooting dialog have been displayed.
 */
internal data class QualityMetrics(
    val lastServiceStartTimeMs: Long,
    val lastScenarioStartTimeMs: Long,
    val accessibilityLossCount: Int,
    val troubleshootingDisplayCount: Int
)

internal const val INVALID_TIME = -1L