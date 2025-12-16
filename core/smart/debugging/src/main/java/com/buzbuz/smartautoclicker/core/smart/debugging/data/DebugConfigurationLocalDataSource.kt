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
package com.buzbuz.smartautoclicker.core.smart.debugging.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DebugConfigurationLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {

    /** Shared preferences containing the debug config. */
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        DEBUG_CONFIGURATION_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    /** @return the isEnabled value for the debug view. */
    fun isDebugViewEnabled(): Boolean =
        sharedPreferences.getBoolean(PREF_DEBUG_VIEW_ENABLED, false)

    /** @return the isEnabled value for the debug report. */
    fun isDebugReportEnabled(): Boolean =
        sharedPreferences.getBoolean(PREF_DEBUG_REPORT_ENABLED, false)

    /** Save a new enabled value for the debug report. */
    fun setDebuggingConfig(debugView: Boolean, debugReport: Boolean) =
        sharedPreferences.edit {
            putBoolean(PREF_DEBUG_VIEW_ENABLED, debugView)
            putBoolean(PREF_DEBUG_REPORT_ENABLED, debugReport)
        }
}

/** Debug configuration SharedPreference name. */
private const val DEBUG_CONFIGURATION_PREFERENCES_NAME = "DebugConfigPreferences"
/** User selection for the debug view visibility in the SharedPreferences. */
private const val PREF_DEBUG_VIEW_ENABLED = "Debug_View_Enabled"
/** User selection for the debug report in the SharedPreferences. */
private const val PREF_DEBUG_REPORT_ENABLED = "Debug_Report_Enabled"