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
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.AND
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.database.domain.Event

/**
 * Creates a new event.
 *
 * @param context the Android context.
 * @return the new event.
 */
fun newDefaultEvent(context: Context, scenarioId: Long, scenarioEventsSize: Int) = Event(
    scenarioId = scenarioId,
    name = context.getString(R.string.default_event_name),
    conditionOperator = AND,
    priority = scenarioEventsSize,
    conditions = mutableListOf(),
    actions = mutableListOf(),
)

/**
 * Creates a new default condition.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @param area the area of the condition to create.
 * @param bitmap the image for the condition to create.
 * @return the new condition.
 */
fun newDefaultCondition(context: Context, eventId: Long, area: Rect, bitmap: Bitmap) = Condition(
    eventId = eventId,
    bitmap = bitmap,
    area = area,
    threshold = context.resources.getInteger(R.integer.default_condition_threshold),
)

/**
 * Creates a new default click action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new click.
 */
fun newDefaultClick(context: Context, eventId: Long) = Action.Click(
    eventId = eventId,
    name = context.getString(R.string.default_click_name),
    pressDuration = context.getEventConfigPreferences().getClickPressDurationConfig(context),
)

/**
 * Creates a new default swipe action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new swipe.
 */
fun newDefaultSwipe(context: Context, eventId: Long) = Action.Swipe(
    eventId = eventId,
    name = context.getString(R.string.default_swipe_name),
    swipeDuration = context.getEventConfigPreferences().getSwipeDurationConfig(context),
)

/**
 * Creates a new default pause action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new pause.
 */
fun newDefaultPause(context: Context, eventId: Long) = Action.Pause(
    eventId = eventId,
    name = context.getString(R.string.default_pause_name),
    pauseDuration = context.getEventConfigPreferences().getPauseDurationConfig(context)
)

/** @return the shared preferences for the default configuration. */
fun Context.getEventConfigPreferences(): SharedPreferences =
    getSharedPreferences(
        EVENT_CONFIG_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

/** @return the default duration for a click press. */
private fun SharedPreferences.getClickPressDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_CLICK_PRESS_DURATION,
    context.resources.getInteger(R.integer.default_click_press_duration).toLong()
)

/** Save a new default duration for the click press. */
fun SharedPreferences.Editor.putClickPressDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_CLICK_PRESS_DURATION, durationMs)

/** @return the default duration for a swipe. */
private fun SharedPreferences.getSwipeDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_SWIPE_DURATION,
    context.resources.getInteger(R.integer.default_swipe_duration).toLong()
)

/** Save a new default duration for the swipe. */
fun SharedPreferences.Editor.putSwipeDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_SWIPE_DURATION, durationMs)

/** @return the default duration for a pause. */
private fun SharedPreferences.getPauseDurationConfig(context: Context) : Long = getLong(
    PREF_LAST_PAUSE_DURATION,
    context.resources.getInteger(R.integer.default_pause_duration).toLong()
)

/** Save a new default duration for the pause. */
fun SharedPreferences.Editor.putPauseDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_PAUSE_DURATION, durationMs)

/** Event default configuration SharedPreference name. */
private const val EVENT_CONFIG_PREFERENCES_NAME = "EventConfigPreferences"
/** User last click press duration key in the SharedPreferences. */
private const val PREF_LAST_CLICK_PRESS_DURATION = "Last_Click_Press_Duration"
/** User last swipe press duration key in the SharedPreferences. */
private const val PREF_LAST_SWIPE_DURATION = "Last_Swipe_Duration"
/** User last pause press duration key in the SharedPreferences. */
private const val PREF_LAST_PAUSE_DURATION = "Last_Pause_Duration"
