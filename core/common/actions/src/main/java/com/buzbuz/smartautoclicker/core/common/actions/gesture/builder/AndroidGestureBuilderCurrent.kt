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
import android.graphics.Point
import android.os.Build
import androidx.annotation.RequiresApi
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.buildStroke
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.continueWith
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.hold
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.line
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger

import kotlin.random.Random


/**
 * Splits a gesture into the smallest time windows during which no finger changes motion, and describes each of them
 * as its own [GestureDescription]. A finger touching the screen over several windows is described by a stroke chain,
 * each stroke continuing the one of the previous window.
 *
 * The slicing is global to the gesture, and not per finger: the system only accepts a continuation when the first
 * step of the dispatched gesture holds exactly one stroke for each pointer currently down, no more, no less.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class AndroidGestureBuilderCurrent(
    random: Random?,
) : AndroidGestureBuilder(random) {

    override val supportsStrokeContinuation: Boolean = true

    override fun buildSlices(
        fingers: List<ResolvedFinger>,
        initialHeldStrokes: Map<Int, StrokeDescription>,
    ): AndroidGestureSlices? {
        val boundaries = fingers.toSliceBoundaries()
        if (boundaries.size < 2) return null

        val slices = mutableListOf<AndroidGestureSlice>()
        /** Last stroke built for a finger still touching the screen, indexed by finger index. */
        val heldStrokes = initialHeldStrokes.toMutableMap()
        /** Position at which the last stroke of a finger ended, indexed by finger index. */
        val heldPositions = mutableMapOf<Int, Point>()
        /** Time not consumed by the previous slices, to delay the next one with. */
        var pendingDelayMs = 0L

        for (boundaryIndex in 0 until boundaries.lastIndex) {
            val sliceStartMs = boundaries[boundaryIndex]
            val sliceEndMs = boundaries[boundaryIndex + 1]
            val sliceDurationMs = sliceEndMs - sliceStartMs

            val sliceFingers = fingers.mapNotNull { finger ->
                val segment = finger.segmentAt(sliceStartMs) ?: return@mapNotNull null
                SliceFinger(
                    index = finger.index,
                    continuedStroke = heldStrokes[finger.index],
                    from = heldPositions[finger.index] ?: segment.from,
                    to = segment.positionAt(sliceEndMs),
                    willContinue = finger.upMs > sliceEndMs || finger.heldAfterRelease,
                )
            }

            // Nothing is touching the screen during this window, keep its duration to delay the next slice.
            if (sliceFingers.isEmpty()) {
                pendingDelayMs += sliceDurationMs
                heldStrokes.clear()
                heldPositions.clear()
                continue
            }

            // The system schedules a dispatched gesture up to its last motion event, and nothing more: a moving
            // finger emits them all along the slice, and a finger leaving the screen emits one at its very end.
            // Without any of those, the slice duration is dropped, and the gesture is even refused when it has no
            // event at all to emit. Keep that duration to delay the next slice instead: its pointers then stay
            // untouched on the screen for exactly that time, which is how a wait is executed.
            val advancesTime = sliceFingers.any { sliceFinger -> sliceFinger.isMoving || !sliceFinger.willContinue }
            if (!advancesTime && sliceFingers.none { sliceFinger -> sliceFinger.isPressing }) {
                pendingDelayMs += sliceDurationMs
                continue
            }

            val continuesPreviousSlice = sliceFingers.any { sliceFinger -> !sliceFinger.isPressing }
            val builder = GestureDescription.Builder()
            val newStrokes = mutableMapOf<Int, StrokeDescription>()
            val newPositions = mutableMapOf<Int, Point>()

            sliceFingers.forEach { sliceFinger ->
                val path = Path().apply {
                    if (sliceFinger.isMoving) line(sliceFinger.from, sliceFinger.to)
                    else hold(sliceFinger.from)
                }

                // A finger pressing down while others are already held can't be part of the first step of the slice,
                // the system would then refuse the whole continuation. Delay its press by one millisecond.
                val pressDelayMs =
                    if (sliceFinger.isPressing && continuesPreviousSlice) NEW_FINGER_PRESS_DELAY_MS
                    else 0L
                val startTimeMs = pendingDelayMs + pressDelayMs
                val durationMs = (sliceDurationMs - pressDelayMs).coerceAtLeast(1L)

                val stroke = sliceFinger.continuedStroke
                    ?.continueWith(path, startTimeMs, durationMs, sliceFinger.willContinue)
                    ?: buildStroke(path, startTimeMs, durationMs, sliceFinger.willContinue)

                builder.addStroke(stroke)
                if (sliceFinger.willContinue) {
                    newStrokes[sliceFinger.index] = stroke
                    newPositions[sliceFinger.index] = sliceFinger.to
                }
            }

            slices.add(AndroidGestureSlice(builder.build(), continuesPreviousSlice))

            heldStrokes.clear()
            heldStrokes.putAll(newStrokes)
            heldPositions.clear()
            heldPositions.putAll(newPositions)
            // A slice dispatched only to press a finger down emits its event right away and consumes nothing more.
            pendingDelayMs = if (advancesTime) 0L else sliceDurationMs
        }

        if (slices.isEmpty()) return null
        return AndroidGestureSlices(slices, heldStrokes.toMap(), heldPositions.toMap())
    }

    /**
     * All the times at which a finger starts or ends a motion, defining the windows to slice the gesture into. Zero
     * is always one of them, so that a gesture where no finger starts immediately keeps its initial delay.
     */
    private fun List<ResolvedFinger>.toSliceBoundaries(): List<Long> {
        val boundaries = sortedSetOf(0L)
        forEach { finger ->
            finger.segments.forEach { segment ->
                boundaries.add(segment.startMs)
                boundaries.add(segment.endMs)
            }
        }

        return boundaries.toList()
    }
}

/** What a finger does during a single slice. */
private data class SliceFinger(
    val index: Int,
    val continuedStroke: StrokeDescription?,
    val from: Point,
    val to: Point,
    val willContinue: Boolean,
) {
    val isMoving: Boolean get() = from != to
    val isPressing: Boolean get() = continuedStroke == null
}

/** Delay applied to a finger pressing down in a slice that continues another one. */
private const val NEW_FINGER_PRESS_DELAY_MS = 1L
