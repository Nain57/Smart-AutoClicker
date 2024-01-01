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
package com.buzbuz.smartautoclicker.core.database

import androidx.annotation.StringDef

/** Defines the different tables in the database. */
@StringDef(
    SCENARIO_TABLE, EVENT_TABLE, ACTION_TABLE, CONDITION_TABLE, END_CONDITION_TABLE,
    INTENT_EXTRA_TABLE, EVENT_TOGGLE_TABLE, TUTORIAL_SUCCESS_TABLE
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class DatabaseTable

internal const val SCENARIO_TABLE = "scenario_table"
internal const val EVENT_TABLE = "event_table"
internal const val ACTION_TABLE = "action_table"
internal const val CONDITION_TABLE = "condition_table"
internal const val INTENT_EXTRA_TABLE = "intent_extra_table"
internal const val EVENT_TOGGLE_TABLE = "event_toggle_table"
internal const val TUTORIAL_SUCCESS_TABLE = "tutorial_success_table"

/** DELETED but kept because referenced in migrations. */
internal const val END_CONDITION_TABLE = "end_condition_table"
