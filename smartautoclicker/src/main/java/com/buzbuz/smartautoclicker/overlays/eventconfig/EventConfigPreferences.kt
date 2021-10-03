/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.content.Context
import android.content.SharedPreferences
import com.buzbuz.smartautoclicker.R

/** */
const val EVENT_CONFIG_PREFERENCES_NAME = "EventConfigPreferences"

/** */
private const val PREF_LAST_CLICK_PRESS_DURATION = "Last_Click_Press_Duration"
/** */
private const val PREF_LAST_SWIPE_DURATION = "Last_Swipe_Duration"
/** */
private const val PREF_LAST_PAUSE_DURATION = "Last_Pause_Duration"

/**
 *
 */
fun SharedPreferences.getClickPressDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_CLICK_PRESS_DURATION,
    context.resources.getInteger(R.integer.default_click_press_duration).toLong()
)

/**
 *
 */
fun SharedPreferences.Editor.putClickPressDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_CLICK_PRESS_DURATION, durationMs)

/**
 *
 */
fun SharedPreferences.getSwipeDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_SWIPE_DURATION,
    context.resources.getInteger(R.integer.default_swipe_duration).toLong()
)

/**
 *
 */
fun SharedPreferences.Editor.putSwipeDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_SWIPE_DURATION, durationMs)


/**
 *
 */
fun SharedPreferences.getPauseDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_PAUSE_DURATION,
    context.resources.getInteger(R.integer.default_pause_duration).toLong()
)

/**
 *
 */
fun SharedPreferences.Editor.putPauseDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_PAUSE_DURATION, durationMs)
