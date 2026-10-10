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

import android.graphics.Point
import android.util.Log
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.randomizedDurationMs
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.randomizedPosition
import com.buzbuz.smartautoclicker.core.common.actions.model.FIRST_FINGER_INDEX
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedSegment

import kotlin.random.Random


/**
 * Places the motions of a finger on the gesture timeline, resolving and randomizing every position and duration once
 * and for all.
 *
 * Feed it the motions of the finger in order, from its press to its release, then call [build]. A finger touches the
 * screen once: pressing or releasing more than once, or moving while not touching the screen, invalidates it.
 *
 * Obtain one from [AndroidGestureBuilder.newFinger], so that the randomization and the Android version specific
 * behaviours are consistent with the gesture being built.
 */
class ResolvedFingerBuilder internal constructor(
    private val index: Int,
    startTimeMs: Long,
    private val random: Random?,
    private val supportsStrokeContinuation: Boolean,
) {

    private val segments = mutableListOf<ResolvedSegment>()
    private var currentTimeMs: Long = startTimeMs.coerceAtLeast(0L)
    private var currentPosition: Point? = null
    private var isReleased: Boolean = false
    private var isInvalid: Boolean = false
    private var isHeld: Boolean = false

    /**
     * The finger touches the screen at [position] and stays there for [holdMs]. Must be called first, and only once.
     *
     * Unlike [wait], this initial hold does not need stroke continuation: it is the duration of the very first stroke
     * of the finger, so it is honoured on every Android version.
     */
    fun press(position: Point, holdMs: Long = 0L): ResolvedFingerBuilder = apply {
        if (currentPosition != null || isReleased) {
            invalidate("it presses more than once")
            return@apply
        }

        val pressPosition = random.randomizedPosition(position)
        currentPosition = pressPosition
        if (holdMs <= 0L) return@apply

        val resolvedDurationMs = random.randomizedDurationMs(holdMs)
        segments.add(ResolvedSegment(currentTimeMs, currentTimeMs + resolvedDurationMs, pressPosition, pressPosition))
        currentTimeMs += resolvedDurationMs
    }

    /** The finger slides from its current position to [position] in [durationMs]. */
    fun move(position: Point, durationMs: Long): ResolvedFingerBuilder = apply {
        val from = currentPositionOrInvalidate() ?: return@apply
        val to = random.randomizedPosition(position)
        val resolvedDurationMs = random.randomizedDurationMs(durationMs)

        segments.add(ResolvedSegment(currentTimeMs, currentTimeMs + resolvedDurationMs, from, to))
        currentTimeMs += resolvedDurationMs
        currentPosition = to
    }

    /**
     * The finger stays still at its current position for [durationMs]. Holding a finger requires stroke
     * continuation, so this is skipped on the Android versions without it.
     */
    fun wait(durationMs: Long): ResolvedFingerBuilder = apply {
        val position = currentPositionOrInvalidate() ?: return@apply
        if (!supportsStrokeContinuation) {
            Log.i(TAG, "Skipping wait motion of finger $index, not supported on this Android version")
            return@apply
        }

        val resolvedDurationMs = random.randomizedDurationMs(durationMs)

        segments.add(ResolvedSegment(currentTimeMs, currentTimeMs + resolvedDurationMs, position, position))
        currentTimeMs += resolvedDurationMs
    }

    /**
     * The finger leaves the screen at its current position. Must be called last, and only once.
     *
     * @param hold true to keep the pointer down on the screen once the gesture is over instead of lifting it. Holding
     * requires stroke continuation, and only the first finger of a gesture can be held: the request is dropped
     * otherwise, and the finger is released as usual.
     */
    fun release(hold: Boolean = false): ResolvedFingerBuilder = apply {
        if (currentPositionOrInvalidate() == null) return@apply
        isReleased = true
        if (!hold) return@apply

        when {
            !supportsStrokeContinuation ->
                Log.i(TAG, "Skipping hold of finger $index, not supported on this Android version")
            index != FIRST_FINGER_INDEX ->
                Log.i(TAG, "Skipping hold of finger $index, only the first finger of a gesture can be held")
            else ->
                isHeld = true
        }
    }

    /** @return the finger placed on the timeline, or null if its motions are not a valid touch sequence. */
    fun build(): ResolvedFinger? {
        if (isInvalid) return null
        if (currentPosition == null || !isReleased) {
            invalidate("it does not press and release exactly once")
            return null
        }

        // Nothing but a press and a release: a simple tap, held long enough to be detected as such.
        if (segments.isEmpty()) {
            segments.add(
                ResolvedSegment(currentTimeMs, currentTimeMs + TAP_DURATION_MS, currentPosition!!, currentPosition!!)
            )
        }

        return ResolvedFinger(index, segments.toList(), isHeld)
    }

    private fun currentPositionOrInvalidate(): Point? {
        val position = currentPosition
        when {
            position == null -> invalidate("it moves before pressing")
            isReleased -> invalidate("it moves or releases after releasing")
            else -> return position
        }
        return null
    }

    private fun invalidate(reason: String) {
        if (!isInvalid) Log.i(TAG, "Can't resolve finger $index, $reason")
        isInvalid = true
    }
}

/** Duration of a finger pressing and releasing without moving. Matches the platform tap timeout. */
private const val TAP_DURATION_MS = 100L

private const val TAG = "ResolvedFingerBuilder"
