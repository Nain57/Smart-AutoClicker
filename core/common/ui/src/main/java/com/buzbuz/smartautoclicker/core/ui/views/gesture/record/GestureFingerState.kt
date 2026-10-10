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
package com.buzbuz.smartautoclicker.core.ui.views.gesture.record

import android.graphics.PointF
import android.os.Build
import android.util.Log
import kotlin.math.sqrt

internal class GestureFingerState(
    initialPosition: PointF,
    private val initialEventTime: Long,
    displayDensity: Float,
) {

    /** Holding a finger still requires stroke continuation, which is only available from Android O. */
    private val isWaitSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private val stillThresholdPx: Float = STILL_THRESHOLD_DP * displayDensity
    private val moveAngleLockDistancePx: Float = MOVE_ANGLE_LOCK_DISTANCE_THRESHOLD_DP * displayDensity

    val motions: MutableList<RecordedGestureFingerMotion> = mutableListOf(RecordedGestureFingerMotion.Press(initialPosition))

    /** Position of the initial press, kept for the finger that never moves and records a simple click. */
    val pressPosition: PointF = initialPosition

    /** True until a [RecordedGestureFingerMotion.Move] is recorded for this finger. */
    val isStationary: Boolean
        get() = motions.none { it is RecordedGestureFingerMotion.Move }

    /** Time elapsed since the press, whatever the motions recorded in between. */
    val pressDurationMs: Long
        get() = latestEventTimeMs - initialEventTime

    /**
     * Time of the last event received for this finger, of any kind. Unlike [lastEventTimeMs], it is also updated for
     * the events absorbed by the still detection, making it the correct press duration reference and the end time of
     * a pending wait.
     */
    private var latestEventTimeMs: Long = initialEventTime

    /**
     * Direction of the first step that opened the current Move segment. Never updated mid-segment so that an arc's
     * cumulative turn is measured against where the segment started, not the last step.
     */
    private var currentSegmentDirection: PointF? = null

    /** Start position of the current*/
    private var moveStartPosition: PointF = initialPosition
    private var moveDistanceFromStart: Float = 0f

    private var stillStartPosition: PointF = initialPosition
    private var stillStartEventTimeMs: Long = initialEventTime

    /** Time of the last event that ended a move step or a wait, reference for the next move duration. */
    private var lastEventTimeMs: Long = initialEventTime

    fun addMoveOrWait(newPosition: PointF, eventTime: Long) {
        val previousEventTimeMs = latestEventTimeMs
        latestEventTimeMs = eventTime

        val last = motions.lastOrNull()
        val dtStill = eventTime - stillStartEventTimeMs
        val dxStill = newPosition.x - stillStartPosition.x
        val dyStill = newPosition.y - stillStartPosition.y
        val distanceFromStill = sqrt(dxStill * dxStill + dyStill * dyStill)

        // Check if the event is near the reference to detect a wait. User is rarely 100% still.
        if (distanceFromStill <= stillThresholdPx) {
            recordWaitIfLongEnough(durationMs = dtStill)
            return
        }

        // Going past the threshold. A pending wait ended with the last still event, the move starts from there.
        if (last is RecordedGestureFingerMotion.Wait) lastEventTimeMs = previousEventTimeMs
        stillStartPosition = newPosition
        stillStartEventTimeMs = eventTime

        val dtLast = eventTime - lastEventTimeMs
        lastEventTimeMs = eventTime

        val dxRef = newPosition.x - moveStartPosition.x
        val dyRef = newPosition.y - moveStartPosition.y
        val distanceFromRef = sqrt(dxRef * dxRef + dyRef * dyRef)
        val directionFromRef = PointF(dxRef / distanceFromRef, dyRef / distanceFromRef)
        val segmentDirection = currentSegmentDirection

        val previousDistanceFromMoveStart = moveDistanceFromStart
        moveDistanceFromStart = distanceFromRef

        // If the last recorded motion is not a Move, start a fresh one.
        if (last !is RecordedGestureFingerMotion.Move) {
            Log.d(TAG, "Motion is a new move, position=$newPosition")
            motions.add(RecordedGestureFingerMotion.Move(moveToPosition = newPosition, moveToDurationMs = dtLast))

            // Lock the segment direction only once we have enough distance for a reliable bearing.
            currentSegmentDirection =
                if (distanceFromRef > moveAngleLockDistancePx) directionFromRef
                else null

            return
        }

        // No angle lock yet, the line is too short.
        if (segmentDirection == null) {
            Log.d(TAG, "Motion has no angle lock yet, same move; new position = $newPosition")
            motions[motions.lastIndex] = RecordedGestureFingerMotion.Move(
                moveToPosition = newPosition,
                moveToDurationMs = last.moveToDurationMs + dtLast,
            )

            if (distanceFromRef > moveAngleLockDistancePx) currentSegmentDirection = directionFromRef
            return
        }

        // Angle lock is set, verify if we are still moving in the same direction.
        // cosAngle, close to 1 for parallel directions and decreases toward 0 as the deviation grows.
        val cosAngle = directionFromRef.x * segmentDirection.x + directionFromRef.y * segmentDirection.y
        val distance = distanceFromRef - previousDistanceFromMoveStart
        if (cosAngle >= DIRECTION_SIMILARITY_THRESHOLD && distance > -stillThresholdPx) {
            Log.d(TAG, "Motion is a move in the same direction, new position = $newPosition")
            motions[motions.lastIndex] = RecordedGestureFingerMotion.Move(
                moveToPosition = newPosition,
                moveToDurationMs = last.moveToDurationMs + dtLast,
            )
        } else {
            Log.d(TAG, "Motion direction has changed, this is a new move; position=$newPosition")
            motions.add(RecordedGestureFingerMotion.Move(moveToPosition = newPosition, moveToDurationMs = dtLast))

            moveStartPosition = newPosition
            currentSegmentDirection = null
        }
    }

    fun addRelease(eventTime: Long) {
        latestEventTimeMs = eventTime
        // The finger has been still since the still start, even if no event was received in the meantime.
        recordWaitIfLongEnough(durationMs = eventTime - stillStartEventTimeMs)
        motions.add(RecordedGestureFingerMotion.Release)
    }

    fun toGestureFinger(): RecordedGestureFinger = RecordedGestureFinger(
        pressedAtMs = initialEventTime,
        motions = motions.toList(),
    )

    /**
     * Records the still period from the still start as a wait. A pending wait is extended to the new duration;
     * otherwise, a new one is created once the period is long enough.
     */
    private fun recordWaitIfLongEnough(durationMs: Long) {
        if (motions.lastOrNull() is RecordedGestureFingerMotion.Wait) {
            Log.d(TAG, "Motion is still a wait, now of $durationMs")
            motions[motions.lastIndex] = RecordedGestureFingerMotion.Wait(durationMs)
            return
        }

        if (!isWaitSupported || durationMs < MIN_WAIT_DURATION_MS) return

        Log.d(TAG, "Motion is a new wait of $durationMs")
        motions.add(RecordedGestureFingerMotion.Wait(durationMs))
        moveStartPosition = stillStartPosition
        currentSegmentDirection = null
    }
}


/** Distance threshold in dp between two events to consider the finger still (not moving). */
private const val STILL_THRESHOLD_DP = 10f

/**
 * Distance in dp from the segment reference point at which the direction is considered reliable enough
 * to lock. Must be greater than [STILL_THRESHOLD_DP]. Converted to pixels at construction time.
 */
private const val MOVE_ANGLE_LOCK_DISTANCE_THRESHOLD_DP = 16f
/**
 * Minimum cumulative still time (ms) before a pause is recorded as a [RecordedGestureFingerMotion.Wait].
 * Still events below this duration are absorbed into the surrounding moves, preventing brief
 * slowdowns between strokes from generating spurious waits.
 */
private const val MIN_WAIT_DURATION_MS = 200L
/**
 * Minimum dot product between two unit-vectors for them to be considered the same direction.
 * Equals cos(maxAngle), so the allowed deviation from the segment's start direction is arccos of this value:
 */
private const val DIRECTION_SIMILARITY_THRESHOLD = 0.9848f

/** Tag for logs. */
private const val TAG = "GestureFingerState"
