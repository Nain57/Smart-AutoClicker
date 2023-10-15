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
package com.buzbuz.smartautoclicker.feature.billing

/** Defines the advantages of Smart AutoClicker Pro. */
interface ProModeAdvantage {

    /** Features available with Pro. */
    enum class Feature : ProModeAdvantage {
        ACTION_TYPE_INTENT,
        ACTION_TYPE_TOGGLE_EVENT,
        BACKUP_EXPORT,
        BACKUP_IMPORT,
        EVENT_STATE,
        SCENARIO_ANTI_DETECTION,
        SCENARIO_DETECTION_QUALITY,
        SCENARIO_END_CONDITIONS,
    }

    /** Limitations for users without Pro. */
    enum class Limitation(val limit: Int) : ProModeAdvantage {
        ACTION_COUNT_LIMIT(5),
        CONDITION_COUNT_LIMIT(2),
        DETECTION_DURATION_MINUTES_LIMIT(20),
        EVENT_COUNT_LIMIT(10),
        SMART_SCENARIO_COUNT_LIMIT(2),
    }
}
