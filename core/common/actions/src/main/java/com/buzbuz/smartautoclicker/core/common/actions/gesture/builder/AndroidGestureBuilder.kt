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
package com.buzbuz.smartautoclicker.core.common.actions.gesture.builder

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Point
import android.os.Build
import android.util.Log
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HELD_POINTER_FINGER_INDEX
import com.buzbuz.smartautoclicker.core.common.actions.model.HELD_POINTER_RELEASE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedSegment

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
     * Whether this builder can continue the stroke of a finger into another dispatched gesture. Holding a finger
     * still on the screen relies on it, so waits and held fingers are skipped on the Android versions without it.
     */
    protected abstract val supportsStrokeContinuation: Boolean

    /**
     * Start the resolution of a finger of the gesture.
     *
     * @param index the position of the finger in the gesture.
     * @param startTimeMs the delay between the start of the gesture and the press of this finger.
     */
    fun newFinger(index: Int, startTimeMs: Long): ResolvedFingerBuilder =
        ResolvedFingerBuilder(index, startTimeMs, random, supportsStrokeContinuation)

    /**
     * Build the gesture as the chains of [GestureDescription] to dispatch, in order, or null if it can't be executed.
     *
     * The gestures of a same chain can be dispatched in one go, the system appends them to what it has already
     * scheduled. Dispatching the first gesture of a chain cancels whatever is still pending: the previous chain must
     * have been fully played before.
     *
     * @param fingers the resolved fingers of the gesture.
     * @param gestureName the name of the gesture, for logging purposes.
     * @param heldPointer the pointer a previous gesture left down on the screen, if any. It is carried along by this
     * gesture, or lifted when one of the [fingers] takes the hold over.
     * @param owner what this gesture is executed for. It becomes the owner of the pointer this gesture holds, if any.
     */
    fun build(
        fingers: List<ResolvedFinger>,
        gestureName: String?,
        heldPointer: HeldPointer? = null,
        owner: Any? = null,
    ): BuiltGesture? {
        if (fingers.isEmpty()) {
            Log.i(TAG, "No usable finger in gesture $gestureName, skipping action")
            return null
        }

        return try {
            val gestureFingers = fingers.withHeldFingerExtendedToGestureEnd()

            val slices = when (heldPointer) {
                null -> buildSlices(gestureFingers, emptyMap())
                else -> buildSlices(
                    fingers = gestureFingers + heldPointer.toCarriedFinger(gestureFingers),
                    initialHeldStrokes = mapOf(HELD_POINTER_FINGER_INDEX to heldPointer.stroke),
                )
            } ?: return null

            val chains = slices.slices.toGestureChains()
            if (chains.isEmpty()) return null

            BuiltGesture(chains, slices.toHeldPointer(gestureName, heldPointer, owner))
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, "Gesture $gestureName has been refused by Android, skipping action", ex)
            null
        } catch (ex: IllegalStateException) {
            Log.w(TAG, "Gesture $gestureName has been refused by Android, skipping action", ex)
            null
        }
    }

    /**
     * Split the resolved fingers into the slices to dispatch, in order.
     *
     * @param fingers the fingers of the gesture, including the carried [HeldPointer] when there is one.
     * @param initialHeldStrokes the strokes already dispatched for the pointers down on the screen when the gesture
     * starts, indexed by finger index. A finger found in there is continued instead of pressed.
     */
    protected abstract fun buildSlices(
        fingers: List<ResolvedFinger>,
        initialHeldStrokes: Map<Int, StrokeDescription>,
    ): AndroidGestureSlices?

    /**
     * A held finger keeps its pointer down once the gesture is over, so it must still be down in the very last slice:
     * a slice where it is the only pointer left would otherwise close its chain, and dispatching the next chain
     * cancels whatever the previous one left down. Extend it with a wait instead.
     */
    private fun List<ResolvedFinger>.withHeldFingerExtendedToGestureEnd(): List<ResolvedFinger> {
        val heldFinger = firstOrNull { finger -> finger.heldAfterRelease } ?: return this

        val gestureEndMs = maxOf { finger -> finger.upMs }
        if (heldFinger.upMs >= gestureEndMs) return this

        val lastSegment = heldFinger.segments.last()
        val extendedFinger = heldFinger.copy(
            segments = heldFinger.segments + ResolvedSegment(
                startMs = lastSegment.endMs,
                endMs = gestureEndMs,
                from = lastSegment.to,
                to = lastSegment.to,
            ),
        )

        return map { finger -> if (finger.index == heldFinger.index) extendedFinger else finger }
    }

    /**
     * The held pointer as an extra finger of the gesture being built, one that never presses: it is already down, all
     * the slicing has to do is keep a stroke for it in every slice so that the system keeps honouring the
     * continuation.
     *
     * A gesture holding a finger of its own takes the hold over, so the carried pointer is lifted as soon as the
     * gesture starts. Any other gesture carries it until its end and leaves it down.
     */
    private fun HeldPointer.toCarriedFinger(fingers: List<ResolvedFinger>): ResolvedFinger {
        val isReplaced = fingers.any { finger -> finger.heldAfterRelease }

        return ResolvedFinger(
            index = HELD_POINTER_FINGER_INDEX,
            segments = listOf(
                ResolvedSegment(
                    startMs = 0L,
                    endMs =
                        if (isReplaced) HELD_POINTER_RELEASE_DURATION_MS
                        else fingers.maxOf { finger -> finger.upMs },
                    from = position,
                    to = position,
                )
            ),
            heldAfterRelease = !isReplaced,
        )
    }

    /**
     * The pointer the slices leave down on the screen, if any.
     *
     * A carried pointer left down still belongs to the owner of [carriedPointer], while a finger of this gesture taking
     * the hold over belongs to [owner].
     */
    private fun AndroidGestureSlices.toHeldPointer(
        gestureName: String?,
        carriedPointer: HeldPointer?,
        owner: Any?,
    ): HeldPointer? {
        if (heldStrokes.isEmpty()) return null

        val fingerIndex = heldStrokes.keys.singleOrNull() ?: let {
            Log.w(TAG, "Gesture $gestureName keeps ${heldStrokes.size} pointers down, only one can be held")
            return null
        }
        val position = heldPositions[fingerIndex] ?: return null

        val heldOwner =
            if (fingerIndex == HELD_POINTER_FINGER_INDEX) carriedPointer?.owner
            else owner

        return HeldPointer(heldStrokes.getValue(fingerIndex), position, heldOwner)
    }

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

    /**
     * The slices of a gesture, and what they leave down on the screen.
     *
     * @param slices the slices to dispatch, in order.
     * @param heldStrokes the last stroke built for each finger whose pointer is still down once the last slice is
     * played, indexed by finger index.
     * @param heldPositions the position at which each of those pointers rests, indexed by finger index.
     */
    protected data class AndroidGestureSlices(
        val slices: List<AndroidGestureSlice>,
        val heldStrokes: Map<Int, StrokeDescription> = emptyMap(),
        val heldPositions: Map<Int, Point> = emptyMap(),
    )
}
