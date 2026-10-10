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
package com.buzbuz.smartautoclicker.core.common.actions.gesture.builder

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.buildStroke
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.lineTo
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.moveTo
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger

import kotlin.random.Random


/**
 * Stroke continuation is only available from Android O, so a finger can only be described by a single stroke here.
 *
 * A stroke is played at a constant speed along its path, which means the time spent on a part of the path is its
 * share of the total path length, and not the duration configured for the matching motion. The overall gesture
 * duration is correct, the timing of each motion is not: this is why waits are not offered on those versions.
 */
internal class AndroidGestureBuilderCompat24(
    random: Random?,
) : AndroidGestureBuilder(random) {

    override val supportsStrokeContinuation: Boolean = false

    /** Without stroke continuation there is nothing to continue and nothing can be left down, so both maps stay empty. */
    override fun buildSlices(
        fingers: List<ResolvedFinger>,
        initialHeldStrokes: Map<Int, StrokeDescription>,
    ): AndroidGestureSlices {
        val builder = GestureDescription.Builder()

        fingers.forEach { finger ->
            val path = Path().apply {
                moveTo(finger.segments.first().from)
                finger.segments.forEach { segment ->
                    if (segment.from != segment.to) lineTo(segment.to)
                }
            }

            builder.addStroke(
                buildStroke(
                    path = path,
                    startTimeMs = finger.downMs,
                    durationMs = finger.upMs - finger.downMs,
                    willContinue = false,
                )
            )
        }

        return AndroidGestureSlices(listOf(AndroidGestureSlice(builder.build(), continuesPreviousSlice = false)))
    }
}
