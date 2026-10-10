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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.GestureDescription
import android.os.Build
import android.util.Log

import kotlin.random.Random


/**
 * Describes a multitouch gesture as the [GestureDescription] to dispatch to Android.
 *
 * The gesture is built from its [ResolvedFinger], obtained by feeding the motions of each finger to a builder from
 * [newFinger]. This is the only entry point for the randomization: the same [Random] is applied to all the fingers,
 * and the fingers alone know nothing about it.
 *
 * @param random the source for the randomization of positions and durations, or null for exact values.
 */
abstract class AndroidGestureBuilder internal constructor(
    private val random: Random?,
) {

    companion object {
        private const val TAG = "AndroidGestureBuilder"

        fun createNewBuilder(random: Random?): AndroidGestureBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) AndroidGestureBuilderCurrent(random)
            else AndroidGestureBuilderCompat24(random)
    }

    /**
     * Whether this builder can execute a wait motion. Holding a finger still on the screen requires stroke
     * continuation, so waits are skipped on the Android versions without it.
     */
    protected abstract val supportsWaitMotions: Boolean

    /**
     * Start the resolution of a finger of the gesture.
     *
     * @param index the position of the finger in the gesture.
     * @param startTimeMs the delay between the start of the gesture and the press of this finger.
     */
    fun newFinger(index: Int, startTimeMs: Long): ResolvedFingerBuilder =
        ResolvedFingerBuilder(index, startTimeMs, random, supportsWaitMotions)

    /**
     * Build the gesture as the chains of [GestureDescription] to dispatch, in order, or null if it can't be executed.
     *
     * The gestures of a same chain can be dispatched in one go, the system appends them to what it has already
     * scheduled. Dispatching the first gesture of a chain cancels whatever is still pending: the previous chain must
     * have been fully played before.
     *
     * @param fingers the resolved fingers of the gesture.
     * @param gestureName the name of the gesture, for logging purposes.
     */
    fun build(fingers: List<ResolvedFinger>, gestureName: String?): List<List<GestureDescription>>? {
        if (fingers.isEmpty()) {
            Log.i(TAG, "No usable finger in gesture $gestureName, skipping action")
            return null
        }

        return try {
            buildSlices(fingers)?.toGestureChains()?.ifEmpty { null }
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, "Gesture $gestureName has been refused by Android, skipping action", ex)
            null
        } catch (ex: IllegalStateException) {
            Log.w(TAG, "Gesture $gestureName has been refused by Android, skipping action", ex)
            null
        }
    }

    /** Split the resolved fingers into the slices to dispatch, in order. */
    protected abstract fun buildSlices(fingers: List<ResolvedFinger>): List<AndroidGestureSlice>?

    /**
     * Group the slices into the chains of gestures to dispatch.
     *
     * Slices of a same chain can be dispatched in one go, the system appends them to what it has already scheduled. The
     * first slice of a chain starts a new touch sequence, and dispatching it cancels whatever is still pending: the
     * previous chain must have been fully played before.
     */
    private fun List<AndroidGestureSlice>.toGestureChains(): List<List<GestureDescription>> {
        val chains = mutableListOf<MutableList<GestureDescription>>()

        forEach { slice ->
            if (slice.continuesPreviousSlice && chains.isNotEmpty()) chains.last().add(slice.description)
            else chains.add(mutableListOf(slice.description))
        }

        return chains
    }

    /**
     * A gesture is executed as an ordered list of slices.
     *
     * The system only honours a continued stroke when it is found in the first step of a **separately dispatched**
     * gesture, so a finger crossing several motions can't be described by one [GestureDescription]. Each slice is one
     * dispatch, and holds at most one stroke per finger touching the screen during that time window.
     *
     * @param description the gesture to dispatch for this time window.
     * @param continuesPreviousSlice true when at least one finger of this slice keeps a pointer of the previous slice
     * down. Such a slice can be dispatched right away, the system appends it to what is already scheduled. A slice with
     * false starts a new touch sequence: dispatching it cancels whatever is still pending, so the previous slice
     * must have been fully played before.
     */
    protected data class AndroidGestureSlice(
        val description: GestureDescription,
        val continuesPreviousSlice: Boolean,
    )
}
