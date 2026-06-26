/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.scenarios.list.model

import android.content.Context
import com.buzbuz.smartautoclicker.R

internal fun Context.getTimeSinceString(timestamp: Long): String {
    if (timestamp == 0L) return resources.getString(R.string.item_scenario_last_used_never)

    val elapsed = System.currentTimeMillis() - timestamp
    return when {
        elapsed < MILLIS_PER_HOUR -> {
            val minutes = (elapsed / MILLIS_PER_MINUTE).toInt()
            resources.getQuantityString(R.plurals.item_scenario_last_used_minutes, minutes, minutes)
        }
        elapsed < MILLIS_PER_DAY -> {
            val hours = (elapsed / MILLIS_PER_HOUR).toInt()
            resources.getQuantityString(R.plurals.item_scenario_last_used_hours, hours, hours)
        }
        else -> {
            val days = (elapsed / MILLIS_PER_DAY).toInt()
            resources.getQuantityString(R.plurals.item_scenario_last_used_days, days, days)
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val MILLIS_PER_DAY = 86_400_000L