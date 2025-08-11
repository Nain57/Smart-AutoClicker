package com.buzbuz.smartautoclicker.core.database

import androidx.annotation.StringDef

/** Defines the different tables in the database. */
@StringDef(
    SCENARIO_TABLE, EVENT_TABLE, ACTION_TABLE, CONDITION_TABLE, END_CONDITION_TABLE,
    INTENT_EXTRA_TABLE, EVENT_TOGGLE_TABLE, SCENARIO_USAGE_TABLE
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class DatabaseTable

internal const val SCENARIO_TABLE = "scenario_table"
internal const val EVENT_TABLE = "event_table"
internal const val ACTION_TABLE = "action_table"
internal const val CONDITION_TABLE = "condition_table"
internal const val INTENT_EXTRA_TABLE = "intent_extra_table"
internal const val EVENT_TOGGLE_TABLE = "event_toggle_table"
internal const val SCENARIO_USAGE_TABLE = "scenario_usage_table"

/** DELETED but kept because referenced in migrations. */
internal const val END_CONDITION_TABLE = "end_condition_table"
