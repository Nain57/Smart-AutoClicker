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
package com.buzbuz.smartautoclicker.core.smart.debugging.utils

import android.annotation.SuppressLint
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence

/** Format this value as a displayable confidence rate. */
@SuppressLint("DefaultLocale")
fun Double.formatDebugConfidenceRate(): String =
    "${String.format("%.2f", (this * 100).coerceIn(0.0, 100.0))} % "

/**
 * Format this image event occurrence conditions results as a displayable text.
 * If only one condition have been fulfilled, use its name. If more are fulfilled, use 'fulfilled/conditionsCount'.
 */
fun DebugLiveImageEventOccurrence.formatConditionResultsDisplayText(): String =
    if (event.conditions.size == 1) {
        imageConditionsResults.first().condition.name
    } else {
        val fulfilled = imageConditionsResults.fold(0) { acc, result ->
            acc + (if (result.isFulfilled) 1 else 0)
        }
        "$fulfilled/${event.conditions.size}"
    }
