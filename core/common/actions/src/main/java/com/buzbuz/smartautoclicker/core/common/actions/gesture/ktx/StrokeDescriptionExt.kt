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
package com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import androidx.annotation.RequiresApi
import com.buzbuz.smartautoclicker.core.common.actions.utils.MAXIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.MINIMUM_STROKE_DURATION_MS
import kotlin.math.max
import kotlin.math.min

internal fun buildStroke(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): GestureDescription.StrokeDescription {
    val normalizedStart = startTimeMs.toNormalizedStrokeStartTime()
    val normalizedDuration = durationMs.toNormalizedStrokeDurationMs()

    return if (willContinue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        GestureDescription.StrokeDescription(path, normalizedStart, normalizedDuration, true)
    } else {
        GestureDescription.StrokeDescription(path, normalizedStart, normalizedDuration)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun GestureDescription.StrokeDescription.continueWith(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): GestureDescription.StrokeDescription =
    continueStroke(
        path,
        startTimeMs.toNormalizedStrokeStartTime(),
        durationMs.toNormalizedStrokeDurationMs(),
        willContinue,
    )

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))
