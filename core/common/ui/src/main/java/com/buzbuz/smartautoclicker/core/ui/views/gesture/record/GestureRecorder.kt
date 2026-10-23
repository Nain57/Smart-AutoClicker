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

import android.accessibilityservice.GestureDescription
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent


internal class GestureRecorder(
    private val displayDensity: Float,
    private val onNewCapturedGesture: (gesture: RecordedGesture?, isFinished: Boolean) -> Unit,
) {

    /** All fingers recorded for the current gesture, in press order. */
    private val recordedFingers = mutableListOf<GestureFingerState>()
    /** Fingers currently pressed, by pointer id. Android recycles ids, so a re-press must not replace a recorded finger. */
    private val activeFingers = HashMap<Int, GestureFingerState>()

    /**
     * Maximum allowed gesture duration in ms. Recording stops automatically when reached. Defaults to 1ms below the
     * Android limit, as a gesture lasting exactly that limit is rejected.
     */
    var maxDurationMs: Long = GestureDescription.getMaxGestureDuration() - 1

    private var firstFingerEventTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val timeoutRunnable = Runnable { finishOnMaxDuration() }

    fun processEvent(event: MotionEvent): Boolean {
        // The timeout runnable can run late, an event past the max duration must not extend the gesture.
        if (recordedFingers.isNotEmpty() && event.eventTime - firstFingerEventTime > maxDurationMs) {
            finishOnMaxDuration()
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                handleFingerDown(event = event, pointerIndex = 0)
            MotionEvent.ACTION_POINTER_DOWN ->
                if (recordedFingers.size < MAX_FINGERS) handleFingerDown(event = event, pointerIndex = event.actionIndex)
            MotionEvent.ACTION_MOVE ->
                handleMove(event = event)
            MotionEvent.ACTION_POINTER_UP ->
                handleFingerUp(event = event, pointerIndex = event.actionIndex, isLast = false)
            MotionEvent.ACTION_UP ->
                handleFingerUp(event = event, pointerIndex = event.actionIndex, isLast = true)
            MotionEvent.ACTION_CANCEL ->
                clearCapture()
            else -> return false
        }

        return true
    }

    fun clearCapture() {
        Log.d(TAG, "Clear capture")
        handler.removeCallbacks(timeoutRunnable)
        resetFingers()
    }

    private fun finishOnMaxDuration() {
        handler.removeCallbacks(timeoutRunnable)

        val releaseEventTime = firstFingerEventTime + maxDurationMs
        for (finger in activeFingers.values) finger.addRelease(releaseEventTime)
        notifyListener(isFinished = true)
        resetFingers()
    }

    private fun resetFingers() {
        recordedFingers.clear()
        activeFingers.clear()
        firstFingerEventTime = 0L
    }

    private fun handleFingerDown(event: MotionEvent, pointerIndex: Int) {
        Log.d(TAG, "Handle finger down #$pointerIndex: $event")

        if (recordedFingers.isEmpty()) {
            firstFingerEventTime = event.eventTime
            if (maxDurationMs < Long.MAX_VALUE) handler.postDelayed(timeoutRunnable, maxDurationMs)
        }

        val pointerId = event.getPointerId(pointerIndex)
        val finger = GestureFingerState(
            initialPosition = PointF(event.getX(pointerIndex), event.getY(pointerIndex)),
            initialEventTime = event.eventTime,
            displayDensity = displayDensity,
        )
        recordedFingers.add(finger)
        activeFingers[pointerId] = finger
        notifyListener(isFinished = false)
    }

    private fun handleMove(event: MotionEvent) {
        Log.d(TAG, "Handle move $event")

        for (pointerIndex in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(pointerIndex)
            activeFingers[pointerId]?.addMoveOrWait(
                newPosition = PointF(event.getX(pointerIndex), event.getY(pointerIndex)),
                eventTime = event.eventTime,
            )
        }
        notifyListener(isFinished = false)
    }

    private fun handleFingerUp(event: MotionEvent, pointerIndex: Int, isLast: Boolean) {
        Log.d(TAG, "Handle finger up (isLast=$isLast) #$pointerIndex: $event")

        val pointerId = event.getPointerId(pointerIndex)
        activeFingers.remove(pointerId)?.addRelease(event.eventTime)

        if (isLast) {
            handler.removeCallbacks(timeoutRunnable)
            notifyListener(isFinished = true)
            resetFingers()
        } else {
            notifyListener(isFinished = false)
        }
    }

    private fun notifyListener(isFinished: Boolean) {
        if (recordedFingers.isEmpty()) {
            onNewCapturedGesture(null, isFinished)
            return
        }
        onNewCapturedGesture(recordedFingers.toRecordedGesture(), isFinished)
    }

    /** A lone finger that never moved is a click, anything else is a gesture. */
    private fun Collection<GestureFingerState>.toRecordedGesture(): RecordedGesture {
        val singleFinger = singleOrNull()

        return if (singleFinger != null && singleFinger.isStationary)
            RecordedGesture.Click(
                position = singleFinger.pressPosition,
                durationMs = singleFinger.pressDurationMs.coerceAtLeast(MIN_CLICK_DURATION_MS),
            )
        else
            RecordedGesture.Gesture(fingers = map { it.toGestureFinger() })
    }
}

/** Maximum number of fingers allowed on a single gesture. */
private const val MAX_FINGERS = 5
/** A click press duration can't be null, use this value for the taps recorded within the same millisecond. */
private const val MIN_CLICK_DURATION_MS = 1L
/** Tag for logs. */
private const val TAG = "GestureRecorder"
