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
import android.util.Log
import androidx.annotation.RequiresApi
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.buildStroke
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.continueWith
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.hold
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.line
import com.buzbuz.smartautoclicker.core.common.actions.model.HELD_POINTER_FINGER_INDEX
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger

import kotlin.random.Random


/**
 * Splits a gesture into the smallest time windows during which no finger changes motion, and describes each of them
 * as its own [GestureDescription]. A finger touching the screen over several windows is described by a stroke chain,
 * each stroke continuing the one of the previous window.
 *
 * The slicing is global to the gesture, and not per finger: the system only accepts a continuation when the first
 * step of the dispatched gesture holds exactly one stroke for each pointer currently down, no more, no less.
 *
 * A gesture can be built on every processed frame when its event keeps being detected, so the working buffers are kept
 * between two builds instead of being allocated each time. This makes an instance not thread safe.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class AndroidGestureBuilderCurrent(
    random: Random?,
) : AndroidGestureBuilder(random) {

    override val supportsStrokeContinuation: Boolean = true

    /** Sorted and distinct slice boundaries of the gesture being built, valid up to the count returned by [toSliceBoundaries]. */
    private var boundaries: LongArray = LongArray(INITIAL_BOUNDARIES_CAPACITY)

    /** Last stroke built for a finger still touching the screen, indexed by finger index. */
    private var heldStrokes: Array<StrokeDescription?> = arrayOfNulls(FINGER_SLOTS_COUNT)
    /** Position at which the last stroke of a finger ended, indexed by finger index. */
    private var heldPositions: Array<Point?> = arrayOfNulls(FINGER_SLOTS_COUNT)
    /** Same as [heldStrokes], for the slice being built. Swapped with it once the slice is done. */
    private var sliceStrokes: Array<StrokeDescription?> = arrayOfNulls(FINGER_SLOTS_COUNT)
    /** Same as [heldPositions], for the slice being built. Swapped with it once the slice is done. */
    private var slicePositions: Array<Point?> = arrayOfNulls(FINGER_SLOTS_COUNT)

    /** The path of the stroke being built. A [StrokeDescription] keeps its own copy, so it can be reused right away. */
    private val strokePath: Path = Path()

    override fun buildSlices(
        fingers: List<ResolvedFinger>,
        carriedStroke: StrokeDescription?,
    ): AndroidGestureSlices? {
        if (fingers.any { finger -> finger.index !in 0 until FINGER_SLOTS_COUNT }) {
            Log.w(TAG, "Gesture has a finger index out of bounds, can't build it")
            return null
        }

        val boundariesCount = fingers.toSliceBoundaries()
        if (boundariesCount < 2) return null

        try {
            heldStrokes[HELD_POINTER_FINGER_INDEX] = carriedStroke
            return buildSlices(fingers, boundariesCount)
        } finally {
            // Don't keep the strokes of this gesture alive until the next one is built
            heldStrokes.fill(null)
            heldPositions.fill(null)
            sliceStrokes.fill(null)
            slicePositions.fill(null)
        }
    }

    private fun buildSlices(fingers: List<ResolvedFinger>, boundariesCount: Int): AndroidGestureSlices? {
        val slices = mutableListOf<AndroidGestureSlice>()
        /** Time not consumed by the previous slices, to delay the next one with. */
        var pendingDelayMs = 0L

        for (boundaryIndex in 0 until boundariesCount - 1) {
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
                heldStrokes.fill(null)
                heldPositions.fill(null)
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
            sliceStrokes.fill(null)
            slicePositions.fill(null)

            for (sliceFingerIndex in sliceFingers.indices) {
                val sliceFinger = sliceFingers[sliceFingerIndex]

                strokePath.reset()
                if (sliceFinger.isMoving) strokePath.line(sliceFinger.from, sliceFinger.to)
                else strokePath.hold(sliceFinger.from)

                // A finger pressing down while others are already held can't be part of the first step of the slice,
                // the system would then refuse the whole continuation. Delay its press by one millisecond.
                val pressDelayMs =
                    if (sliceFinger.isPressing && continuesPreviousSlice) NEW_FINGER_PRESS_DELAY_MS
                    else 0L
                val startTimeMs = pendingDelayMs + pressDelayMs
                val durationMs = (sliceDurationMs - pressDelayMs).coerceAtLeast(1L)

                val stroke = sliceFinger.continuedStroke
                    ?.continueWith(strokePath, startTimeMs, durationMs, sliceFinger.willContinue)
                    ?: buildStroke(strokePath, startTimeMs, durationMs, sliceFinger.willContinue)

                builder.addStroke(stroke)
                if (sliceFinger.willContinue) {
                    sliceStrokes[sliceFinger.index] = stroke
                    slicePositions[sliceFinger.index] = sliceFinger.to
                }
            }

            slices.add(AndroidGestureSlice(builder.build(), continuesPreviousSlice))
            swapSliceBuffers()

            // A slice dispatched only to press a finger down emits its event right away and consumes nothing more.
            pendingDelayMs = if (advancesTime) 0L else sliceDurationMs
        }

        if (slices.isEmpty()) return null
        return toAndroidGestureSlices(slices)
    }

    /** The strokes of the slice that was just built become the ones to continue in the next slice. */
    private fun swapSliceBuffers() {
        val strokes = heldStrokes
        heldStrokes = sliceStrokes
        sliceStrokes = strokes

        val positions = heldPositions
        heldPositions = slicePositions
        slicePositions = positions
    }

    private fun toAndroidGestureSlices(slices: List<AndroidGestureSlice>): AndroidGestureSlices {
        var heldFingersCount = 0
        var heldFingerIndex = -1
        for (fingerIndex in heldStrokes.indices) {
            if (heldStrokes[fingerIndex] == null) continue
            heldFingersCount++
            heldFingerIndex = fingerIndex
        }

        if (heldFingersCount == 0) return AndroidGestureSlices(slices)
        return AndroidGestureSlices(
            slices = slices,
            heldFingersCount = heldFingersCount,
            heldFingerIndex = heldFingerIndex,
            heldStroke = heldStrokes[heldFingerIndex],
            heldPosition = heldPositions[heldFingerIndex],
        )
    }

    /**
     * Fill [boundaries] with all the times at which a finger starts or ends a motion, sorted and without duplicates.
     * They define the windows to slice the gesture into. Zero is always one of them, so that a gesture where no finger
     * starts immediately keeps its initial delay.
     *
     * @return the number of boundaries.
     */
    private fun List<ResolvedFinger>.toSliceBoundaries(): Int {
        var capacity = 1
        for (fingerIndex in indices) capacity += this[fingerIndex].segments.size * 2
        if (boundaries.size < capacity) boundaries = LongArray(capacity)

        var count = 0
        boundaries[count++] = 0L
        for (fingerIndex in indices) {
            val segments = this[fingerIndex].segments
            for (segmentIndex in segments.indices) {
                boundaries[count++] = segments[segmentIndex].startMs
                boundaries[count++] = segments[segmentIndex].endMs
            }
        }

        boundaries.sort(fromIndex = 0, toIndex = count)

        var distinctCount = 1
        for (index in 1 until count) {
            if (boundaries[index] != boundaries[distinctCount - 1]) boundaries[distinctCount++] = boundaries[index]
        }

        return distinctCount
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
/** One slot for each finger a gesture can have, plus the carried held pointer, which has the highest index. */
private const val FINGER_SLOTS_COUNT = HELD_POINTER_FINGER_INDEX + 1
/** Enough for a few fingers with a few motions each, the buffer grows if a gesture needs more. */
private const val INITIAL_BOUNDARIES_CAPACITY = 32

private const val TAG = "AndroidGestureBuilderCurrent"
