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
package com.buzbuz.smartautoclicker.feature.scenario.debugging

import android.content.Context
import android.content.SharedPreferences

/** @return the shared preferences for the debug config. */
internal fun Context.getDebugConfigPreferences(): SharedPreferences =
    getSharedPreferences(
        DEBUG_CONFIGURATION_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

/** @return the isEnabled value for the debug view. */
internal fun SharedPreferences.getIsDebugViewEnabled(context: Context) : Boolean = getBoolean(
    PREF_DEBUG_VIEW_ENABLED,
    context.resources.getBoolean(R.bool.default_debug_view_enabled),
)

/** Save a new enabled value for the debug view. */
internal fun SharedPreferences.Editor.putIsDebugViewEnabled(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_DEBUG_VIEW_ENABLED, enabled)

/** @return the isEnabled value for the debug report. */
internal fun SharedPreferences.getIsDebugReportEnabled(context: Context) : Boolean = getBoolean(
    PREF_DEBUG_REPORT_ENABLED,
    context.resources.getBoolean(R.bool.default_debug_report_enabled),
)

/** Save a new enabled value for the debug report. */
internal fun SharedPreferences.Editor.putIsDebugReportEnabled(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_DEBUG_REPORT_ENABLED, enabled)


/** Debug configuration SharedPreference name. */
private const val DEBUG_CONFIGURATION_PREFERENCES_NAME = "DebugConfigPreferences"
/** User selection for the debug view visibility in the SharedPreferences. */
private const val PREF_DEBUG_VIEW_ENABLED = "Debug_View_Enabled"
/** User selection for the debug report in the SharedPreferences. */
private const val PREF_DEBUG_REPORT_ENABLED = "Debug_Report_Enabled"