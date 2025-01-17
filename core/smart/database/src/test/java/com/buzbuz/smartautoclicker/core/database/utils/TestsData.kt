/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.utils

/** Data set for the database tests. */
internal object TestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_END_CONDITION_OPERATOR = 1

    /* ------- Event Data ------- */

    const val EVENT_ID = 1667L

    const val EVENT_NAME = "EventName"
    const val EVENT_CONDITION_OPERATOR = 1
    val EVENT_STOP_AFTER = null
}