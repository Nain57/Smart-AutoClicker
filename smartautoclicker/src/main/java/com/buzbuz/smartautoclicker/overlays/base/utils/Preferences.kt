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
package com.buzbuz.smartautoclicker.overlays.base.utils

import android.content.Context
import android.content.SharedPreferences

import com.buzbuz.smartautoclicker.R

/** @return the shared preferences for the default configuration. */
fun Context.getEventConfigPreferences(): SharedPreferences =
    getSharedPreferences(
        EVENT_CONFIG_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

/** @return the default duration for a click press. */
fun SharedPreferences.getClickPressDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_CLICK_PRESS_DURATION,
    context.resources.getInteger(R.integer.default_click_press_duration).toLong()
)

/** Save a new default duration for the click press. */
fun SharedPreferences.Editor.putClickPressDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_CLICK_PRESS_DURATION, durationMs)

/** @return the default duration for a swipe. */
fun SharedPreferences.getSwipeDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_SWIPE_DURATION,
    context.resources.getInteger(R.integer.default_swipe_duration).toLong()
)

/** Save a new default duration for the swipe. */
fun SharedPreferences.Editor.putSwipeDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_SWIPE_DURATION, durationMs)

/** @return the default duration for a pause. */
fun SharedPreferences.getPauseDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_PAUSE_DURATION,
    context.resources.getInteger(R.integer.default_pause_duration).toLong()
)

/** Save a new default duration for the pause. */
fun SharedPreferences.Editor.putPauseDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_PAUSE_DURATION, durationMs)

/** @return the default isAdvanced for the intents. */
fun SharedPreferences.getIntentIsAdvancedConfig(context: Context) : Boolean = getBoolean(
    PREF_LAST_INTENT_IS_ADVANCED,
    context.resources.getBoolean(R.bool.default_intent_isAdvanced)
)

/** Save a new default isAdvanced for the intents. */
fun SharedPreferences.Editor.putIntentIsAdvancedConfig(isAdvanced: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_LAST_INTENT_IS_ADVANCED, isAdvanced)

/** @return the shared preferences for the debug config. */
fun Context.getDebugConfigPreferences(): SharedPreferences =
    getSharedPreferences(
        DEBUG_CONFIGURATION_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

/** @return the isEnabled value for the debug view. */
fun SharedPreferences.getIsDebugViewEnabled(context: Context) : Boolean = getBoolean(
    PREF_DEBUG_VIEW_ENABLED,
    context.resources.getBoolean(R.bool.default_debug_view_enabled),
)

/** Save a new enabled value for the debug view. */
fun SharedPreferences.Editor.putIsDebugViewEnabled(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_DEBUG_VIEW_ENABLED, enabled)

/** @return the isEnabled value for the debug report. */
fun SharedPreferences.getIsDebugReportEnabled(context: Context) : Boolean = getBoolean(
    PREF_DEBUG_REPORT_ENABLED,
    context.resources.getBoolean(R.bool.default_debug_report_enabled),
)

/** Save a new enabled value for the debug report. */
fun SharedPreferences.Editor.putIsDebugReportEnabled(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_DEBUG_REPORT_ENABLED, enabled)

/** Event default configuration SharedPreference name. */
private const val EVENT_CONFIG_PREFERENCES_NAME = "EventConfigPreferences"
/** User last click press duration key in the SharedPreferences. */
private const val PREF_LAST_CLICK_PRESS_DURATION = "Last_Click_Press_Duration"
/** User last swipe press duration key in the SharedPreferences. */
private const val PREF_LAST_SWIPE_DURATION = "Last_Swipe_Duration"
/** User last pause press duration key in the SharedPreferences. */
private const val PREF_LAST_PAUSE_DURATION = "Last_Pause_Duration"
/** User last pause press duration key in the SharedPreferences. */
private const val PREF_LAST_INTENT_IS_ADVANCED = "Last_Intent_IsAdvanced"

/** Debug configuration SharedPreference name. */
private const val DEBUG_CONFIGURATION_PREFERENCES_NAME = "DebugConfigPreferences"
/** User selection for the debug view visibility in the SharedPreferences. */
private const val PREF_DEBUG_VIEW_ENABLED = "Debug_View_Enabled"
/** User selection for the debug report in the SharedPreferences. */
private const val PREF_DEBUG_REPORT_ENABLED = "Debug_Report_Enabled"