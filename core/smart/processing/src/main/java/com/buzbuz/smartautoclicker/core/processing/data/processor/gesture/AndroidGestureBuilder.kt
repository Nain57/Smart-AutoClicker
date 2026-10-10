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
package com.buzbuz.smartautoclicker.core.processing.data.processor.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.os.Build
import android.util.Log

import com.buzbuz.smartautoclicker.core.common.actions.gesture.randomizedDurationMs
import com.buzbuz.smartautoclicker.core.common.actions.gesture.randomizedPosition
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.ConditionsResults
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.ProcessingState
import kotlin.math.roundToInt

import kotlin.random.Random


internal abstract class AndroidGestureBuilder(
    protected val processingState: ProcessingState,
    protected val random: Random?,
) {

    companion object {
        protected const val TAG = "AndroidGestureBuilder"

        fun createNewBuilder(processingState: ProcessingState, random: Random?): AndroidGestureBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) AndroidGestureBuilderCurrent(processingState, random)
            else AndroidGestureBuilderCompat24(processingState, random)
    }

    private var gestureEvent: Event? = null
    private var conditionsResults: ConditionsResults? = null

    /**
     * Build the gesture as the chains of [GestureDescription] to dispatch, in order, or null if it can't be executed.
     *
     * The gestures of a same chain can be dispatched in one go, the system appends them to what it has already
     * scheduled. Dispatching the first gesture of a chain cancels whatever is still pending: the previous chain must
     * have been fully played before.
     */
    fun build(gesture: Gesture, event: Event, results: ConditionsResults?): List<List<GestureDescription>>? {
        gestureEvent = event
        conditionsResults = results

        return try {
            val fingers = gesture.fingers.mapIndexedNotNull { index, finger -> resolveFinger(index, finger) }
            if (fingers.isEmpty()) {
                Log.i(TAG, "No usable finger in gesture ${gesture.name}, skipping action")
                return null
            }

            buildSlices(fingers)?.toGestureChains()?.ifEmpty { null }
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, "Gesture ${gesture.name} has been refused by Android, skipping action", ex)
            null
        } catch (ex: IllegalStateException) {
            Log.w(TAG, "Gesture ${gesture.name} has been refused by Android, skipping action", ex)
            null
        } finally {
            gestureEvent = null
            conditionsResults = null
        }
    }

    /**
     * Whether this builder can execute a [GestureFingerMotion.Wait]. Holding a finger still on the screen requires
     * stroke continuation, so waits are skipped on the Android versions without it.
     */
    protected abstract val supportsWaitMotions: Boolean

    /** Split the resolved fingers into the slices to dispatch, in order. */
    protected abstract fun buildSlices(fingers: List<ResolvedFinger>): List<AndroidGestureSlice>?

    /**
     * Place all the motions of a finger on the gesture timeline, resolving and randomizing every position and
     * duration once and for all.
     */
    private fun resolveFinger(index: Int, finger: GestureFinger): ResolvedFinger? {
        if (!finger.isComplete()) {
            Log.i(TAG, "Can't resolve finger $index, finger is incomplete")
            return null
        }

        var currentPosition = (finger.motions.first() as? GestureFingerMotion.Press)
            ?.let(::resolveMotionPosition)
            ?: return null

        var currentTimeMs = finger.startTimeMs.coerceAtLeast(0L)
        val segments = mutableListOf<ResolvedSegment>()

        finger.motions.forEachIndexed { motionIndex, motion ->
            when (motion) {
                is GestureFingerMotion.Press, is GestureFingerMotion.Release -> {
                    // A finger touches the screen once: lifting and pressing it again starts a whole new touch
                    // sequence, which can't be described as the continuation of the previous one.
                    if (motionIndex != 0 && motionIndex != finger.motions.lastIndex) {
                        Log.i(TAG, "Can't resolve finger $index, it presses or releases more than once")
                        return null
                    }
                }

                is GestureFingerMotion.Move -> {
                    val toPosition = resolveMotionPosition(motion) ?: return null
                    val durationMs = motion.durationMs.toResolvedDurationMs()

                    segments.add(
                        ResolvedSegment(currentTimeMs, currentTimeMs + durationMs, currentPosition, toPosition)
                    )
                    currentTimeMs += durationMs
                    currentPosition = toPosition
                }

                is GestureFingerMotion.Wait -> {
                    if (!supportsWaitMotions) {
                        Log.i(TAG, "Skipping wait motion of finger $index, not supported on this Android version")
                        return@forEachIndexed
                    }

                    val durationMs = motion.durationMs.toResolvedDurationMs()

                    segments.add(
                        ResolvedSegment(currentTimeMs, currentTimeMs + durationMs, currentPosition, currentPosition)
                    )
                    currentTimeMs += durationMs
                }
            }
        }

        // Nothing but a press and a release: a simple tap, held long enough to be detected as such.
        if (segments.isEmpty()) {
            segments.add(
                ResolvedSegment(currentTimeMs, currentTimeMs + TAP_DURATION_MS, currentPosition, currentPosition)
            )
        }

        return ResolvedFinger(index, segments)
    }

    private fun resolveMotionPosition(motion: GestureFingerMotion.Positionable): Point? {
        val base = resolveGesturePosition(motion.position)
        if (base == null) {
            Log.d(TAG, "Cannot resolve position for $motion")
            return null
        }
        val off = resolveGesturePosition(motion.offset) ?: Point(0, 0)

        return Point(base.x + off.x, base.y + off.y).randomizedPosition(random)
    }

    private fun resolveGesturePosition(position: GesturePosition): Point? =
        when (position) {
            is GesturePosition.Coordinates -> {
                val x = resolveGestureCoordinateValue(position.x)
                val y = resolveGestureCoordinateValue(position.y)

                if (x != null && y != null) Point(x, y)
                else {
                    Log.d(TAG, "Cannot resolve position for $position, coordinates are null")
                    null
                }
            }

            is GesturePosition.Condition -> {
                val event = gestureEvent
                if (event !is ScreenEvent) {
                    Log.d(TAG, "Cannot resolve position for $position, condition must be a screen event")
                    return null
                }

                val condId = position.conditionId
                val result = when {
                    event.conditionOperator == AND && condId != null -> {
                        val condResult = conditionsResults?.getScreenConditionResult(condId.databaseId)
                        if (condResult == null) {
                            Log.d(TAG, "Cannot resolve position for $position, condition has not been processed")
                            null
                        } else condResult
                    }

                    event.conditionOperator == OR ->
                        conditionsResults?.getFirstScreenConditionDetectedResult()

                    else -> {
                        Log.d(TAG, "Cannot find a condition to resolve, config is invalid")
                        null
                    }
                }

                result?.position?.let { Point(it.x, it.y) }
            }
        }

    private fun resolveGestureCoordinateValue(coordinatesValue: GestureCoordinatesValue): Int? =
        when (coordinatesValue) {
            is GestureCoordinatesValue.Literal ->
                coordinatesValue.value
            is GestureCoordinatesValue.Counter ->
                processingState.getCounterValue(coordinatesValue.counterName)?.toInt()
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

    private fun Long.toResolvedDurationMs(): Long =
        randomizedDurationMs(random).coerceAtLeast(MINIMUM_MOTION_DURATION_MS)


    /**
     * A [Gesture][com.buzbuz.smartautoclicker.core.domain.model.action.Gesture] is executed as an ordered list of slices.
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
     * A finger with all its positions and durations resolved into the gesture timeline.
     * Randomization is already applied, those values are final.
     */
    protected data class ResolvedFinger(
        val index: Int,
        val segments: List<ResolvedSegment>,
    ) {
        /** Time at which the finger touches the screen, relative to the gesture start. */
        val downMs: Long get() = segments.first().startMs
        /** Time at which the finger leaves the screen, relative to the gesture start. */
        val upMs: Long get() = segments.last().endMs

        /** The segment being played at [timeMs], or null if the finger isn't touching the screen at that time. */
        fun segmentAt(timeMs: Long): ResolvedSegment? =
            segments.firstOrNull { segment -> timeMs >= segment.startMs && timeMs < segment.endMs }
    }

    /** A single [Move][com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Move] or
     * [Wait][com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Wait] of a finger,
     * placed on the gesture timeline. A wait has the same [from] and [to]. */
    protected data class ResolvedSegment(
        val startMs: Long,
        val endMs: Long,
        val from: Point,
        val to: Point,
    ) {

        /** Position of the finger at [timeMs], the motion being played at a constant speed. */
        fun positionAt(timeMs: Long): Point {
            if (from == to || timeMs >= endMs) return to
            if (timeMs <= startMs) return from

            val progress = (timeMs - startMs).toDouble() / (endMs - startMs)
            return Point(
                from.x + ((to.x - from.x) * progress).roundToInt(),
                from.y + ((to.y - from.y) * progress).roundToInt(),
            )
        }
    }
}

/** A motion can't be shorter than that, a stroke duration must be strictly positive. */
private const val MINIMUM_MOTION_DURATION_MS = 1L
/** Duration of a finger pressing and releasing without moving. Matches the platform tap timeout. */
private const val TAP_DURATION_MS = 100L
