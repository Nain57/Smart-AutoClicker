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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data

import android.content.Context
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R

internal fun Context.getDefaultDumbClickName(): String =
    getString(R.string.default_dumb_click_name)

internal fun Context.getDefaultDumbClickDurationMs(): Long =
    resources.getInteger(R.integer.default_dumb_click_press_duration).toLong()

internal fun Context.getDefaultDumbSwipeName(): String =
    getString(R.string.default_dumb_swipe_name)

internal fun Context.getDefaultDumbSwipeDurationMs(): Long =
    resources.getInteger(R.integer.default_dumb_swipe_duration).toLong()

internal fun Context.getDefaultDumbPauseName(): String =
    getString(R.string.default_dumb_pause_name)

internal fun Context.getDefaultDumbPauseDurationMs(): Long =
    resources.getInteger(R.integer.default_dumb_pause_duration).toLong()