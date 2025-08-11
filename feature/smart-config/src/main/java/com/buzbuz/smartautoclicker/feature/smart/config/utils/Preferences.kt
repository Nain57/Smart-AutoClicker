
package com.buzbuz.smartautoclicker.feature.smart.config.utils

import android.content.Context
import android.content.SharedPreferences

import com.buzbuz.smartautoclicker.feature.smart.config.R


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
