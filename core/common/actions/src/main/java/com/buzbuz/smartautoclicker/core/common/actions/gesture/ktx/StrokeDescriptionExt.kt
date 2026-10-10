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

import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.core.common.actions.model.HELD_POINTER_RELEASE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.MAXIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.MINIMUM_STROKE_DURATION_MS

import kotlin.math.max
import kotlin.math.min


internal fun buildStroke(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): StrokeDescription {
    val normalizedStart = startTimeMs.toNormalizedStrokeStartTime()
    val normalizedDuration = durationMs.toNormalizedStrokeDurationMs()

    return if (willContinue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        StrokeDescription(path, normalizedStart, normalizedDuration, true)
    } else {
        StrokeDescription(path, normalizedStart, normalizedDuration)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun StrokeDescription.continueWith(
    path: Path,
    startTimeMs: Long,
    durationMs: Long,
    willContinue: Boolean,
): StrokeDescription =
    continueStroke(
        path,
        startTimeMs.toNormalizedStrokeStartTime(),
        durationMs.toNormalizedStrokeDurationMs(),
        willContinue,
    )

/** A continuation of this stroke, as short as possible, from where the pointer rests to [to]. */
internal fun StrokeDescription.toContinuation(from: Point, to: Point, willContinue: Boolean): StrokeDescription? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

    return try {
        continueWith(
            path = Path().apply { if (from == to) hold(from) else line(from, to) },
            startTimeMs = 0L,
            durationMs = HELD_POINTER_RELEASE_DURATION_MS,
            willContinue = willContinue,
        )
    } catch (ex: IllegalArgumentException) {
        Log.w(TAG, "Can't build the continuation of the held pointer stroke", ex)
        null
    } catch (ex: IllegalStateException) {
        Log.w(TAG, "Can't build the continuation of the held pointer stroke", ex)
        null
    }
}

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))

private const val TAG = "StrokeDescriptionExt"
