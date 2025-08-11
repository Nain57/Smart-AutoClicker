
package com.buzbuz.smartautoclicker.feature.smart.debugging

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