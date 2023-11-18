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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain

import android.content.Context
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getClickRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getClickRepeatDelayConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getSwipeDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getSwipeRepeatCountConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getSwipeRepeatDelayConfig

internal fun Context.getDefaultDumbClickName(): String =
    getString(R.string.default_dumb_click_name)

internal fun Context.getDefaultDumbClickDurationMs(): Long = getDumbConfigPreferences()
    .getClickPressDurationConfig(resources.getInteger(R.integer.default_dumb_click_press_duration).toLong())

internal fun Context.getDefaultDumbClickRepeatCount(): Int = getDumbConfigPreferences()
    .getClickRepeatCountConfig(1)

internal fun Context.getDefaultDumbClickRepeatDelay(): Long = getDumbConfigPreferences()
    .getClickRepeatDelayConfig(0)

internal fun Context.getDefaultDumbSwipeName(): String =
    getString(R.string.default_dumb_swipe_name)

internal fun Context.getDefaultDumbSwipeDurationMs(): Long = getDumbConfigPreferences()
    .getSwipeDurationConfig(resources.getInteger(R.integer.default_dumb_swipe_duration).toLong())

internal fun Context.getDefaultDumbSwipeRepeatCount(): Int = getDumbConfigPreferences()
    .getSwipeRepeatCountConfig(1)

internal fun Context.getDefaultDumbSwipeRepeatDelay(): Long = getDumbConfigPreferences()
    .getSwipeRepeatDelayConfig(0)

internal fun Context.getDefaultDumbPauseName(): String =
    getString(R.string.default_dumb_pause_name)

internal fun Context.getDefaultDumbPauseDurationMs(): Long = getDumbConfigPreferences()
    .getPauseDurationConfig(resources.getInteger(R.integer.default_dumb_pause_duration).toLong())
